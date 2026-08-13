package com.smbility.railcargo.cargo.service;

import com.smbility.railcargo.cargo.domain.CargoOrder;
import com.smbility.railcargo.cargo.domain.TemperatureCondition;
import com.smbility.railcargo.cargo.dto.CargoAiAnalysisResult;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

/**
 * 규칙(키워드) 기반 화물 정보 구조화 구현체. 외부 API 키 없이 항상 동작하는 기본값이며 항상 빈으로 등록된다.
 * {@code app.ai.provider=gemini}로 설정되면 {@code GeminiCargoAiAnalysisService}가 {@code @Primary}로
 * 대신 선택되고, 이 클래스는 Gemini 호출 실패 시 폴백(fallback) 용도로도 함께 쓰인다.
 * 화물명/원본 입력 텍스트에서 키워드와 숫자를 추출해 운송조건을 추정하고,
 * 값을 실제로 찾지 못한 필드는 {@code lowConfidenceFields}에 담아 화주 확인이 필요함을 표시한다.
 */
@Service
public class RuleBasedCargoAiAnalysisService implements CargoAiAnalysisService {

    private static final Pattern WEIGHT_PATTERN = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*(kg|킬로그램|톤|t)", Pattern.CASE_INSENSITIVE);
    private static final Pattern VOLUME_PATTERN = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*(cbm|m3|㎥)", Pattern.CASE_INSENSITIVE);

    private static final BigDecimal DEFAULT_WEIGHT_KG = BigDecimal.valueOf(50);

    @Override
    public CargoAiAnalysisResult analyze(CargoOrder cargoOrder) {
        String text = normalize(cargoOrder.getCargoName(), cargoOrder.getRawInput());

        boolean weightFound = WEIGHT_PATTERN.matcher(text).find();
        boolean volumeFound = VOLUME_PATTERN.matcher(text).find();

        BigDecimal weightKg = extractWeightKg(text).orElse(DEFAULT_WEIGHT_KG);
        BigDecimal volumeCbm = extractVolumeCbm(text).orElse(null);
        TemperatureCondition temperatureCondition = extractTemperatureCondition(text);
        boolean hazardous = containsAny(text, "위험물", "인화성", "폭발", "화약", "부식성");
        String packagingType = extractPackagingType(text);
        String handlingNote = extractHandlingNote(text);

        List<String> lowConfidenceFields = new ArrayList<>();
        if (!weightFound) {
            lowConfidenceFields.add("weightKg");
        }
        if (!volumeFound) {
            lowConfidenceFields.add("volumeCbm");
        }

        return new CargoAiAnalysisResult(weightKg, volumeCbm, temperatureCondition, hazardous,
                packagingType, handlingNote, lowConfidenceFields);
    }

    private String normalize(String cargoName, String rawInput) {
        String combined = (cargoName == null ? "" : cargoName) + " " + (rawInput == null ? "" : rawInput);
        return combined.toLowerCase();
    }

    private java.util.Optional<BigDecimal> extractWeightKg(String text) {
        Matcher matcher = WEIGHT_PATTERN.matcher(text);
        if (!matcher.find()) {
            return java.util.Optional.empty();
        }
        BigDecimal value = new BigDecimal(matcher.group(1));
        String unit = matcher.group(2);
        if (unit.startsWith("톤") || unit.equalsIgnoreCase("t")) {
            value = value.multiply(BigDecimal.valueOf(1000));
        }
        return java.util.Optional.of(value.setScale(2, RoundingMode.HALF_UP));
    }

    private java.util.Optional<BigDecimal> extractVolumeCbm(String text) {
        Matcher matcher = VOLUME_PATTERN.matcher(text);
        if (!matcher.find()) {
            return java.util.Optional.empty();
        }
        return java.util.Optional.of(new BigDecimal(matcher.group(1)).setScale(3, RoundingMode.HALF_UP));
    }

    private TemperatureCondition extractTemperatureCondition(String text) {
        if (containsAny(text, "냉동")) {
            return TemperatureCondition.FROZEN;
        }
        if (containsAny(text, "냉장", "신선", "아이스팩", "저온")) {
            return TemperatureCondition.REFRIGERATED;
        }
        return TemperatureCondition.ROOM;
    }

    private String extractPackagingType(String text) {
        if (containsAny(text, "팔레트")) {
            return "팔레트";
        }
        if (containsAny(text, "컨테이너")) {
            return "컨테이너";
        }
        if (containsAny(text, "박스", "상자")) {
            return "박스";
        }
        return "박스";
    }

    private String extractHandlingNote(String text) {
        if (containsAny(text, "파손", "취급주의", "충격")) {
            return "파손 주의";
        }
        return null;
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}
