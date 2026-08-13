package com.smbility.railcargo.cargo.document;

import com.google.cloud.documentai.v1.Document;
import com.google.cloud.documentai.v1.DocumentProcessorServiceClient;
import com.google.cloud.documentai.v1.DocumentProcessorServiceSettings;
import com.google.cloud.documentai.v1.ProcessRequest;
import com.google.cloud.documentai.v1.ProcessResponse;
import com.google.cloud.documentai.v1.RawDocument;
import com.google.protobuf.ByteString;
import com.smbility.railcargo.cargo.dto.CargoDocumentExtractionResponse;
import com.smbility.railcargo.common.exception.BusinessException;
import com.smbility.railcargo.common.exception.ErrorCode;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class CargoDocumentExtractionService {

    private static final long MAX_FILE_BYTES = 20L * 1024 * 1024;
    private static final int MAX_EXTRACTED_CHARS = 200_000;
    private static final float LOW_CONFIDENCE_THRESHOLD = 0.70f;

    private static final Map<String, String> SUPPORTED_TYPES = Map.ofEntries(
            Map.entry("pdf", "application/pdf"),
            Map.entry("png", "image/png"),
            Map.entry("jpg", "image/jpeg"),
            Map.entry("jpeg", "image/jpeg"),
            Map.entry("gif", "image/gif"),
            Map.entry("tif", "image/tiff"),
            Map.entry("tiff", "image/tiff"),
            Map.entry("bmp", "image/bmp"),
            Map.entry("webp", "image/webp"),
            Map.entry("xls", "application/vnd.ms-excel"),
            Map.entry("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
            Map.entry("txt", "text/plain"),
            Map.entry("csv", "text/csv"),
            Map.entry("json", "application/json"),
            Map.entry("xml", "application/xml")
    );

    private final DocumentAiProperties properties;

    public CargoDocumentExtractionResponse extract(MultipartFile file) {
        validate(file);
        String fileName = safeFileName(file.getOriginalFilename());
        String extension = extension(fileName);
        String mimeType = SUPPORTED_TYPES.get(extension);

        try {
            if (extension.equals("xls") || extension.equals("xlsx")) {
                return extractWorkbook(file, fileName, mimeType);
            }
            if (extension.equals("txt") || extension.equals("csv") || extension.equals("json") || extension.equals("xml")) {
                return extractText(file, fileName, mimeType);
            }
            CargoDocumentExtractionResponse demoFixture = extractKnownDemoFixture(fileName, mimeType);
            if (demoFixture != null && !properties.enabled()) {
                return demoFixture;
            }
            return extractWithGoogleDocumentAi(file, fileName, mimeType);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Document extraction failed for {}: {}", fileName, e.getMessage());
            throw new BusinessException(ErrorCode.DOCUMENT_PROCESSING_FAILED,
                    "문서를 읽지 못했습니다. 파일이 손상되지 않았는지 확인해주세요.");
        }
    }

    /**
     * 시연 시나리오에서 사용하는 두 샘플 문서는 외부 OCR 자격증명이 없는 환경에서도 재현 가능해야 한다.
     * 파일명까지 정확히 일치하는 경우에만 고정된 샘플 내용을 제공하고, 일반 문서는 기존 Document AI 경로를 유지한다.
     */
    private CargoDocumentExtractionResponse extractKnownDemoFixture(String fileName, String mimeType) {
        String normalized = fileName.toLowerCase(Locale.ROOT);
        String text;
        if (normalized.equals("inv_001_냉동닭가슴살.pdf")) {
            text = "송장번호: INV-001\n품목: 냉동 닭가슴살\n수량: 100BOX\n총중량: 500kg\n"
                    + "보관 및 운송온도: -18℃\n출발지: 부산\n도착지: 서울\n화물가액: 3,000,000원";
        } else if (normalized.equals("po_002_생수.png")) {
            text = "발주번호: PO-002\n품목: 생수\n수량: 1,000BOX\n총중량: 1,000kg\n"
                    + "온도조건: 상온\n출발지: 대전\n도착지: 부산\n희망납품일: 9/2\n화물가액: 12,000,000원";
        } else {
            return null;
        }
        return new CargoDocumentExtractionResponse(fileName, mimeType, "DEMO_FIXTURE", text,
                1, 0, 0, List.of("시연용 샘플 문서 데이터가 적용되었습니다."));
    }

    private CargoDocumentExtractionResponse extractWithGoogleDocumentAi(
            MultipartFile file, String fileName, String mimeType) throws IOException {
        requireGoogleConfiguration();
        String endpoint = properties.location() + "-documentai.googleapis.com:443";
        DocumentProcessorServiceSettings settings = DocumentProcessorServiceSettings.newBuilder()
                .setEndpoint(endpoint)
                .build();
        String processorName = "projects/%s/locations/%s/processors/%s".formatted(
                properties.projectId(), properties.location(), properties.processorId());
        RawDocument rawDocument = RawDocument.newBuilder()
                .setContent(ByteString.copyFrom(file.getBytes()))
                .setMimeType(mimeType)
                .build();
        ProcessRequest request = ProcessRequest.newBuilder()
                .setName(processorName)
                .setRawDocument(rawDocument)
                .build();

        try (DocumentProcessorServiceClient client = DocumentProcessorServiceClient.create(settings)) {
            ProcessResponse response = client.processDocument(request);
            return toResponse(fileName, mimeType, response.getDocument());
        }
    }

    private CargoDocumentExtractionResponse toResponse(String fileName, String mimeType, Document document) {
        String fullText = document.getText() == null ? "" : document.getText().trim();
        StringBuilder structured = new StringBuilder(fullText);
        List<String> warnings = new ArrayList<>();
        int formFieldCount = 0;
        int tableCount = 0;

        for (Document.Page page : document.getPagesList()) {
            if (!page.getFormFieldsList().isEmpty()) {
                structured.append("\n\n[양식 필드 - ").append(page.getPageNumber()).append("페이지]\n");
            }
            for (Document.Page.FormField field : page.getFormFieldsList()) {
                formFieldCount++;
                String key = anchorText(field.getFieldName().getTextAnchor(), document.getText());
                String value = anchorText(field.getFieldValue().getTextAnchor(), document.getText());
                structured.append(key).append(": ").append(value).append('\n');
                if (field.getFieldValue().getConfidence() < LOW_CONFIDENCE_THRESHOLD) {
                    warnings.add("신뢰도가 낮은 항목을 확인해주세요: " + (key.isBlank() ? "이름 없는 필드" : key));
                }
            }

            for (Document.Page.Table table : page.getTablesList()) {
                tableCount++;
                structured.append("\n[표 ").append(tableCount).append(" - ")
                        .append(page.getPageNumber()).append("페이지]\n");
                appendRows(structured, table.getHeaderRowsList(), document.getText());
                appendRows(structured, table.getBodyRowsList(), document.getText());
            }
        }

        if (fullText.isBlank()) {
            warnings.add("문서에서 텍스트를 찾지 못했습니다. 스캔 해상도와 방향을 확인해주세요.");
        }
        String extracted = limitText(structured.toString(), warnings);
        return new CargoDocumentExtractionResponse(fileName, mimeType,
                "GOOGLE_DOCUMENT_AI_FORM_PARSER", extracted, document.getPagesCount(),
                formFieldCount, tableCount, List.copyOf(warnings));
    }

    private void appendRows(StringBuilder target, List<Document.Page.Table.TableRow> rows, String fullText) {
        for (Document.Page.Table.TableRow row : rows) {
            List<String> cells = row.getCellsList().stream()
                    .map(cell -> anchorText(cell.getLayout().getTextAnchor(), fullText))
                    .toList();
            target.append(String.join(" | ", cells)).append('\n');
        }
    }

    private CargoDocumentExtractionResponse extractWorkbook(
            MultipartFile file, String fileName, String mimeType) throws IOException {
        StringBuilder text = new StringBuilder();
        List<String> warnings = new ArrayList<>();
        int tableCount = 0;
        int sheetCount;

        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(file.getBytes()))) {
            sheetCount = workbook.getNumberOfSheets();
            DataFormatter formatter = new DataFormatter(Locale.KOREA);
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            for (Sheet sheet : workbook) {
                tableCount++;
                text.append("[시트: ").append(sheet.getSheetName()).append("]\n");
                for (Row row : sheet) {
                    List<String> cells = new ArrayList<>();
                    int lastCell = Math.max(row.getLastCellNum(), 0);
                    for (int index = 0; index < lastCell; index++) {
                        cells.add(formatter.formatCellValue(row.getCell(index, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK), evaluator));
                    }
                    text.append(String.join("\t", cells)).append('\n');
                    if (text.length() > MAX_EXTRACTED_CHARS) break;
                }
                appendStructuredCargoRows(text, sheet, formatter, evaluator);
                text.append('\n');
                if (text.length() > MAX_EXTRACTED_CHARS) break;
            }
        }

        return new CargoDocumentExtractionResponse(fileName, mimeType, "APACHE_POI",
                limitText(text.toString(), warnings), sheetCount, 0, tableCount, List.copyOf(warnings));
    }

    private void appendStructuredCargoRows(StringBuilder target, Sheet sheet,
                                           DataFormatter formatter, FormulaEvaluator evaluator) {
        Row header = null;
        int itemColumn = -1;
        int weightColumn = -1;
        for (Row row : sheet) {
            int candidateItemColumn = findColumn(row, formatter, evaluator, "품목", "품명", "상품명");
            int candidateWeightColumn = findColumn(row, formatter, evaluator, "중량", "총중량", "무게");
            if (candidateItemColumn >= 0 && candidateWeightColumn >= 0) {
                header = row;
                itemColumn = candidateItemColumn;
                weightColumn = candidateWeightColumn;
                break;
            }
        }
        if (header == null) return;

        int temperatureColumn = findColumn(header, formatter, evaluator, "온도", "온도조건");
        int originColumn = findColumn(header, formatter, evaluator, "출발지", "상차지");
        int destinationColumn = findColumn(header, formatter, evaluator, "도착지", "하차지");
        target.append("[구조화 화물 행]\n");
        for (int rowIndex = header.getRowNum() + 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) continue;
            String item = cellText(row, itemColumn, formatter, evaluator);
            String weight = cellText(row, weightColumn, formatter, evaluator);
            if (item.isBlank() || weight.isBlank() || item.equals("합계") || item.equals("총계")) continue;
            String normalizedWeight = weight.matches(".*(?i)(kg|킬로그램|킬로|톤|t).*" ) ? weight : weight + "kg";
            target.append("품목: ").append(item).append(" | 중량: ").append(normalizedWeight);
            appendField(target, "온도조건", cellText(row, temperatureColumn, formatter, evaluator));
            appendField(target, "출발지", cellText(row, originColumn, formatter, evaluator));
            appendField(target, "도착지", cellText(row, destinationColumn, formatter, evaluator));
            target.append('\n');
        }
    }

    private int findColumn(Row row, DataFormatter formatter, FormulaEvaluator evaluator, String... labels) {
        int lastCell = Math.max(row.getLastCellNum(), 0);
        for (int index = 0; index < lastCell; index++) {
            String value = cellText(row, index, formatter, evaluator).replaceAll("[\\s()_]+", "").toLowerCase(Locale.ROOT);
            for (String label : labels) {
                if (value.contains(label.replaceAll("[\\s()_]+", "").toLowerCase(Locale.ROOT))) return index;
            }
        }
        return -1;
    }

    private String cellText(Row row, int column, DataFormatter formatter, FormulaEvaluator evaluator) {
        if (column < 0) return "";
        return formatter.formatCellValue(row.getCell(column, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK), evaluator).trim();
    }

    private void appendField(StringBuilder target, String label, String value) {
        if (!value.isBlank()) target.append(" | ").append(label).append(": ").append(value);
    }

    private CargoDocumentExtractionResponse extractText(
            MultipartFile file, String fileName, String mimeType) throws IOException {
        List<String> warnings = new ArrayList<>();
        String text = new String(file.getBytes(), StandardCharsets.UTF_8);
        return new CargoDocumentExtractionResponse(fileName, mimeType, "DIRECT_TEXT",
                limitText(text, warnings), 1, 0, 0, List.copyOf(warnings));
    }

    private String anchorText(Document.TextAnchor anchor, String fullText) {
        StringBuilder result = new StringBuilder();
        for (Document.TextAnchor.TextSegment segment : anchor.getTextSegmentsList()) {
            int start = Math.toIntExact(segment.getStartIndex());
            int end = Math.toIntExact(segment.getEndIndex());
            if (start >= 0 && end <= fullText.length() && start <= end) {
                result.append(fullText, start, end);
            }
        }
        return result.toString().replaceAll("\\s+", " ").trim();
    }

    private String limitText(String value, List<String> warnings) {
        String trimmed = value.trim();
        if (trimmed.length() <= MAX_EXTRACTED_CHARS) return trimmed;
        warnings.add("추출 결과가 길어 앞부분 200,000자만 사용합니다.");
        return trimmed.substring(0, MAX_EXTRACTED_CHARS);
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "분석할 파일을 선택해주세요.");
        }
        if (file.getSize() > MAX_FILE_BYTES) {
            throw new BusinessException(ErrorCode.PAYLOAD_TOO_LARGE, "파일은 최대 20MB까지 업로드할 수 있습니다.");
        }
        String extension = extension(safeFileName(file.getOriginalFilename()));
        if (!SUPPORTED_TYPES.containsKey(extension)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT,
                    "PDF, 이미지(PNG/JPG/TIFF/BMP/WEBP/GIF), 엑셀(XLS/XLSX) 파일을 업로드해주세요.");
        }
    }

    private void requireGoogleConfiguration() {
        if (!properties.enabled() || blank(properties.projectId()) || blank(properties.location()) || blank(properties.processorId())) {
            throw new BusinessException(ErrorCode.DOCUMENT_AI_UNAVAILABLE,
                    "Google Document AI가 아직 설정되지 않았습니다. 관리자에게 문의해주세요.");
        }
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private String safeFileName(String original) {
        String value = original == null || original.isBlank() ? "document" : original;
        value = value.replace('\\', '/');
        return value.substring(value.lastIndexOf('/') + 1);
    }

    private String extension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot < 0 ? "" : fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}
