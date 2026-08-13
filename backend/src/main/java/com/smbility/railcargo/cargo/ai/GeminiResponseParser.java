package com.smbility.railcargo.cargo.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smbility.railcargo.cargo.domain.HazardGrade;
import com.smbility.railcargo.cargo.domain.TemperatureCondition;
import com.smbility.railcargo.cargo.dto.CargoAiAnalysisResult;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Gemini가 생성한 JSON 문자열을 {@link CargoAiAnalysisResult}로 변환한다.
 * 네트워크 호출과 분리해두어 순수 함수로 단위 테스트할 수 있게 했다.
 *
 * <p>Spring MVC가 내부적으로 쓰는 {@code ObjectMapper}를 그대로 주입받지 않고 전용 인스턴스를 직접 만든다 —
 * 전역 빈으로 노출해 재사용하면 MVC의 JSON 직렬화 설정(JSR-310 모듈 등)과 의도치 않게 충돌할 수 있어서다.
 */
@Component
public class GeminiResponseParser {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public CargoAiAnalysisResult parse(String rawJson) {
        JsonNode node;
        try {
            node = objectMapper.readTree(rawJson);
        } catch (Exception e) {
            throw new IllegalArgumentException("Gemini 응답을 JSON으로 파싱할 수 없습니다: " + rawJson, e);
        }

        BigDecimal weightKg = readDecimal(node, "weightKg");
        BigDecimal volumeCbm = readDecimal(node, "volumeCbm");
        TemperatureCondition temperatureCondition = readTemperatureCondition(node);
        boolean hazardous = node.path("hazardous").asBoolean(false);
        HazardGrade hazardGrade = hazardous ? readHazardGrade(node) : null;
        String hazardClassCode = readText(node, "hazardClassCode");
        String hazardClassName = readText(node, "hazardClassName");
        boolean rejected = node.path("rejected").asBoolean(false);
        boolean requiresMsds = node.path("requiresMsds").asBoolean(false);
        BigDecimal surchargeRate = readDecimal(node, "surchargeRate");
        BigDecimal fixedPowerFeeKrw = readDecimal(node, "fixedPowerFeeKrw");
        BigDecimal detectedTemperatureC = readDecimal(node, "detectedTemperatureC");
        List<String> detectedSpecialCargoCodes = readStringArray(node, "detectedSpecialCargoCodes");
        String packagingType = readText(node, "packagingType");
        String handlingNote = readText(node, "handlingNote");
        List<String> lowConfidenceFields = readLowConfidenceFields(node);
        List<String> detectedItems = readStringArray(node, "detectedItems");
        List<String> analysisWarnings = readStringArray(node, "analysisWarnings");

        if (weightKg == null) {
            throw new IllegalArgumentException("Gemini 응답에 weightKg가 없습니다: " + rawJson);
        }

        return new CargoAiAnalysisResult(weightKg, volumeCbm,
                temperatureCondition, hazardous, hazardGrade, hazardClassCode, hazardClassName,
                rejected, requiresMsds, surchargeRate == null ? BigDecimal.ZERO : surchargeRate,
                fixedPowerFeeKrw == null ? BigDecimal.ZERO : fixedPowerFeeKrw, detectedTemperatureC,
                detectedSpecialCargoCodes, packagingType, handlingNote, lowConfidenceFields,
                detectedItems, analysisWarnings);
    }

    private HazardGrade readHazardGrade(JsonNode node) {
        String raw = readText(node, "hazardGrade");
        if (raw == null) {
            return HazardGrade.D;
        }
        try {
            return HazardGrade.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return HazardGrade.D;
        }
    }

    private BigDecimal readDecimal(JsonNode node, String field) {
        JsonNode value = node.path(field);
        if (!value.isNumber()) {
            return null;
        }
        return BigDecimal.valueOf(value.asDouble());
    }

    private String readText(JsonNode node, String field) {
        JsonNode value = node.path(field);
        if (value.isMissingNode() || value.isNull()) {
            return null;
        }
        return value.asText();
    }

    private TemperatureCondition readTemperatureCondition(JsonNode node) {
        String raw = readText(node, "temperatureCondition");
        if (raw == null) {
            return TemperatureCondition.ROOM;
        }
        try {
            return TemperatureCondition.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return TemperatureCondition.ROOM;
        }
    }

    private List<String> readLowConfidenceFields(JsonNode node) {
        return readStringArray(node, "lowConfidenceFields");
    }

    private List<String> readStringArray(JsonNode node, String field) {
        List<String> fields = new ArrayList<>();
        JsonNode array = node.path(field);
        if (array.isArray()) {
            array.forEach(item -> fields.add(item.asText()));
        }
        return fields;
    }
}
