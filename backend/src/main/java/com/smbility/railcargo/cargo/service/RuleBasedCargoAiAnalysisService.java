package com.smbility.railcargo.cargo.service;

import com.smbility.railcargo.cargo.domain.CargoOrder;
import com.smbility.railcargo.cargo.domain.TemperatureCondition;
import com.smbility.railcargo.cargo.dto.CargoAiAnalysisResult;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

/**
 * 실제 AI 모델이 붙기 전까지 사용하는 규칙(키워드) 기반 mock 구현체.
 * 화물명/원본 입력 텍스트에서 키워드와 숫자를 추출해 운송조건을 추정한다.
 * 정확한 값을 찾지 못하면 {@code estimated=true}로 표시한다.
 * 부피는 임의로 추정하지 않고 사용자가 입력할 수 있도록 비워 둔다.
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
        boolean estimated = !weightFound || !volumeFound;

        return new CargoAiAnalysisResult(weightKg, volumeCbm, temperatureCondition, hazardous,
                packagingType, handlingNote, estimated);
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
