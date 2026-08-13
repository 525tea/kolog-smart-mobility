package com.smbility.railcargo.cargo.document;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.smbility.railcargo.common.exception.BusinessException;
import com.smbility.railcargo.common.exception.ErrorCode;
import java.io.ByteArrayOutputStream;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class CargoDocumentExtractionServiceTest {

    private final CargoDocumentExtractionService service = new CargoDocumentExtractionService(
            new DocumentAiProperties(false, "", "us", ""));

    @Test
    void extractsSheetAndCellStructureFromXlsxWithoutGoogleCredentials() throws Exception {
        byte[] bytes;
        try (var workbook = new XSSFWorkbook(); var output = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("발주서");
            var header = sheet.createRow(0);
            header.createCell(0).setCellValue("품목");
            header.createCell(1).setCellValue("중량");
            var row = sheet.createRow(1);
            row.createCell(0).setCellValue("냉동 만두 파렛트 8개");
            row.createCell(1).setCellValue("200kg");
            workbook.write(output);
            bytes = output.toByteArray();
        }

        var file = new MockMultipartFile("file", "발주서.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", bytes);

        var result = service.extract(file);

        assertThat(result.provider()).isEqualTo("APACHE_POI");
        assertThat(result.pageOrSheetCount()).isEqualTo(1);
        assertThat(result.extractedText())
                .contains("[시트: 발주서]")
                .contains("품목\t중량")
                .contains("냉동 만두 파렛트 8개\t200kg");
    }

    @Test
    void rejectsUnsupportedExtension() {
        var file = new MockMultipartFile("file", "payload.exe", "application/octet-stream", new byte[]{1, 2});

        assertThatThrownBy(() -> service.extract(file))
                .isInstanceOfSatisfying(BusinessException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));
    }

    @Test
    void requiresConfiguredGoogleDocumentAiForPdfAndImages() {
        var file = new MockMultipartFile("file", "송장.pdf", "application/pdf", "%PDF-1.7".getBytes());

        assertThatThrownBy(() -> service.extract(file))
                .isInstanceOfSatisfying(BusinessException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(ErrorCode.DOCUMENT_AI_UNAVAILABLE));
    }
}
