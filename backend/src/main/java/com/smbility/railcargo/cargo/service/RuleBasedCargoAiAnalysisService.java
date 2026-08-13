package com.smbility.railcargo.cargo.service;

import com.smbility.railcargo.cargo.domain.CargoOrder;
import com.smbility.railcargo.cargo.domain.HazardGrade;
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

    private static final Pattern WEIGHT_PATTERN = Pattern.compile("(\\d{1,3}(?:,\\d{3})+(?:\\.\\d+)?|\\d+(?:\\.\\d+)?)\\s*(kg|킬로그램|킬로|톤|t)", Pattern.CASE_INSENSITIVE);
    private static final Pattern VOLUME_PATTERN = Pattern.compile("(\\d{1,3}(?:,\\d{3})+(?:\\.\\d+)?|\\d+(?:\\.\\d+)?)\\s*(cbm|m3|㎥)", Pattern.CASE_INSENSITIVE);
    private static final Pattern TEMPERATURE_PATTERN = Pattern.compile("(?:영하\\s*|-)?\\d+(?:\\.\\d+)?\\s*(?:도|℃)");
    private static final Pattern PALLET_PATTERN = Pattern.compile("(\\d+)\\s*(?:개|ea)?\\s*(?:팔레트|파렛트|pallet)|(?:팔레트|파렛트|pallet)\\s*(\\d+)\\s*(?:개|ea)?", Pattern.CASE_INSENSITIVE);
    private static final Pattern BOX_PATTERN = Pattern.compile("(?:약\\s*)?(\\d+)\\s*(?:개\\s*)?(?:박스|box)|(?:박스|box)\\s*(?:약\\s*)?(\\d+)\\s*개?", Pattern.CASE_INSENSITIVE);
    private static final Pattern DIMENSION_PATTERN = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*cm\\s*[x×*]\\s*(\\d+(?:\\.\\d+)?)\\s*cm\\s*[x×*]\\s*(\\d+(?:\\.\\d+)?)\\s*cm", Pattern.CASE_INSENSITIVE);
    private static final Pattern ITEM_WEIGHT_PATTERN = Pattern.compile("([가-힣a-z][가-힣a-z ]{0,18}?)\\s*(\\d{1,3}(?:,\\d{3})+|\\d+)\\s*(kg|킬로그램|킬로|톤|t)", Pattern.CASE_INSENSITIVE);

    private static final BigDecimal DEFAULT_WEIGHT_KG = BigDecimal.valueOf(50);

    @Override
    public CargoAiAnalysisResult analyze(CargoOrder cargoOrder) {
        String text = normalize(cargoOrder.getCargoName(), cargoOrder.getRawInput());

        boolean weightFound = WEIGHT_PATTERN.matcher(text).find();

        BigDecimal weightKg = extractWeightKg(text).orElse(DEFAULT_WEIGHT_KG);
        BigDecimal volumeCbm = extractVolumeCbm(text).orElseGet(() -> calculateBoxVolume(text));
        boolean volumeFound = volumeCbm != null;
        BigDecimal detectedTemperature = extractTemperature(text);
        ColdChainResult coldChain = extractColdChain(text, detectedTemperature);
        TemperatureCondition temperatureCondition = coldChain.condition();
        HazardResult hazard = extractHazard(text);
        boolean hazardous = hazard.detected();
        HazardGrade hazardGrade = hazardous ? hazard.grade() : null;
        SpecialCargoResult specialCargo = extractSpecialCargo(text);
        BigDecimal surchargeRate = hazard.surchargeRate().max(coldChain.surchargeRate()).max(specialCargo.maxRate());
        String packagingType = extractPackagingType(text);
        String handlingNote = extractHandlingNote(text, specialCargo.codes());
        List<String> detectedItems = extractDetectedItems(normalize(null, cargoOrder.getRawInput()));
        List<String> analysisWarnings = extractAnalysisWarnings(text, detectedTemperature, hazard, detectedItems);

        List<String> lowConfidenceFields = new ArrayList<>();
        if (!weightFound) {
            lowConfidenceFields.add("weightKg");
        }
        if (!volumeFound) {
            lowConfidenceFields.add("volumeCbm");
        }
        boolean hazardGradeGuessed = hazardous && hazard.classCode() == null;
        if (hazardGradeGuessed) {
            lowConfidenceFields.add("hazardGrade");
        }
        if (detectedTemperature == null && temperatureCondition != TemperatureCondition.ROOM
                && containsAny(text, "신선", "너무 덥지", "저온")) {
            lowConfidenceFields.add("temperatureCondition");
        }

        return new CargoAiAnalysisResult(weightKg, volumeCbm, temperatureCondition, hazardous, hazardGrade,
                hazard.classCode(), hazard.className(), hazard.rejected(), hazard.requiresMsds(),
                surchargeRate, coldChain.fixedPowerFee(), detectedTemperature, specialCargo.codes(),
                packagingType, handlingNote, lowConfidenceFields, detectedItems, analysisWarnings);
    }

    private String normalize(String cargoName, String rawInput) {
        String combined = (cargoName == null ? "" : cargoName) + " " + (rawInput == null ? "" : rawInput);
        return combined.toLowerCase();
    }

    private java.util.Optional<BigDecimal> extractWeightKg(String text) {
        Matcher matcher = WEIGHT_PATTERN.matcher(text);
        List<BigDecimal> weights = new ArrayList<>();
        while (matcher.find()) {
            BigDecimal value = new BigDecimal(matcher.group(1).replace(",", ""));
            String unit = matcher.group(2);
            if (unit.startsWith("톤") || unit.equalsIgnoreCase("t")) {
                value = value.multiply(BigDecimal.valueOf(1000));
            }
            weights.add(value);
        }
        if (weights.isEmpty()) {
            return java.util.Optional.empty();
        }
        boolean multipleItems = text.contains("[시트:") || text.matches("(?s).*\\d+\\s*(?:kg|킬로그램)\\s*(?:과|와|,)\\s*.*\\d+\\s*(?:kg|킬로그램).*");
        BigDecimal value = multipleItems
                ? weights.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                : weights.stream().max(BigDecimal::compareTo).orElseThrow();
        return java.util.Optional.of(value.setScale(2, RoundingMode.HALF_UP));
    }

    private java.util.Optional<BigDecimal> extractVolumeCbm(String text) {
        Matcher matcher = VOLUME_PATTERN.matcher(text);
        if (!matcher.find()) {
            return java.util.Optional.empty();
        }
        return java.util.Optional.of(new BigDecimal(matcher.group(1).replace(",", "")).setScale(3, RoundingMode.HALF_UP));
    }

    private BigDecimal calculateBoxVolume(String text) {
        Matcher dimensions = DIMENSION_PATTERN.matcher(text);
        Matcher boxes = BOX_PATTERN.matcher(text);
        if (!dimensions.find() || !boxes.find()) return null;
        BigDecimal width = new BigDecimal(dimensions.group(1));
        BigDecimal depth = new BigDecimal(dimensions.group(2));
        BigDecimal height = new BigDecimal(dimensions.group(3));
        String countValue = boxes.group(1) != null ? boxes.group(1) : boxes.group(2);
        BigDecimal count = new BigDecimal(countValue);
        return width.multiply(depth).multiply(height).multiply(count)
                .divide(BigDecimal.valueOf(1_000_000), 3, RoundingMode.HALF_UP);
    }

    private BigDecimal extractTemperature(String text) {
        Matcher matcher = TEMPERATURE_PATTERN.matcher(text);
        if (!matcher.find()) return null;
        String raw = matcher.group();
        boolean belowZero = raw.contains("영하") || raw.trim().startsWith("-");
        String numeric = raw.replaceAll("[^\\d.]", "");
        if (numeric.isBlank()) return null;
        BigDecimal value = new BigDecimal(numeric);
        return belowZero ? value.negate() : value;
    }

    private ColdChainResult extractColdChain(String text, BigDecimal temperature) {
        if ((temperature != null && temperature.compareTo(BigDecimal.valueOf(-10)) <= 0) || containsAny(text, "냉동", "초저온")) {
            return new ColdChainResult(TemperatureCondition.FROZEN, BigDecimal.valueOf(0.30), BigDecimal.valueOf(50_000));
        }
        if ((temperature != null && temperature.compareTo(BigDecimal.TEN) <= 0)
                || containsAny(text, "냉장", "신선", "아이스팩", "저온")) {
            return new ColdChainResult(TemperatureCondition.REFRIGERATED, BigDecimal.valueOf(0.20), BigDecimal.valueOf(40_000));
        }
        if ((temperature != null && temperature.compareTo(BigDecimal.valueOf(25)) <= 0)
                || containsAny(text, "정온", "항온")) {
            return new ColdChainResult(TemperatureCondition.CONSTANT, BigDecimal.valueOf(0.15), BigDecimal.valueOf(30_000));
        }
        return new ColdChainResult(TemperatureCondition.ROOM, BigDecimal.ZERO, BigDecimal.ZERO);
    }

    private HazardResult extractHazard(String text) {
        if (containsAny(text, "화약", "폭약", "폭발물")) return hazard("1급", "폭발물", HazardGrade.A, true);
        if (containsAny(text, "부탄가스", "스프레이", "캠핑가스", "캠핑", "액화질소", "고압가스")) return hazard("2급", "가스류", HazardGrade.A, false);
        if (containsAny(text, "페인트", "알코올", "향수", "시너", "휘발유", "인화성")) return hazard("3급", "인화성 액체", HazardGrade.B, false);
        if (containsAny(text, "성냥", "고체연료", "나트륨", "가연성")) return hazard("4급", "가연성·금수성 물질", HazardGrade.B, false);
        if (containsAny(text, "표백제", "소독제", "소독", "질산암모늄", "산화성")) return hazard("5급", "산화성·과산화물", HazardGrade.C, false);
        if (containsAny(text, "농약", "살충제", "의료폐기물", "독성")) return hazard("6급", "독성·전염성 물질", HazardGrade.D, false);
        if (containsAny(text, "동위원소", "방사성")) return hazard("7급", "방사성 물질", HazardGrade.D, false);
        if (containsAny(text, "염산", "빙초산", "배터리 액", "배터리액", "부식성")) return hazard("8급", "부식성 물질", HazardGrade.C, false);
        if (containsAny(text, "리튬 배터리", "리튬배터리", "전기자전거", "킥보드", "드라이아이스", "자석", "배터리", "전동", "스마트", "충전식", "위험물")) {
            return hazard("9급", "기타 위험물", HazardGrade.D, false);
        }
        if (containsAny(text, "산업용 세정제", "산업용 화학제품", "화학제품")) {
            return new HazardResult(true, null, null, HazardGrade.D, false, true, BigDecimal.valueOf(0.20));
        }
        return new HazardResult(false, null, null, null, false, false, BigDecimal.ZERO);
    }

    private HazardResult hazard(String code, String name, HazardGrade grade, boolean rejected) {
        return new HazardResult(true, code, name, grade, rejected, true,
                rejected ? BigDecimal.ZERO : BigDecimal.valueOf(0.20));
    }

    private SpecialCargoResult extractSpecialCargo(String text) {
        List<String> codes = new ArrayList<>();
        BigDecimal maxRate = BigDecimal.ZERO;
        if (containsAny(text, "대리석", "철근", "쇳덩이", "무거운")) {
            codes.add("OVERWEIGHT"); maxRate = maxRate.max(BigDecimal.valueOf(0.20));
        }
        if (containsAny(text, "유리", "도자기", "모니터", "깨지기", "파손")) {
            codes.add("FRAGILE"); maxRate = maxRate.max(BigDecimal.valueOf(0.10));
        }
        if (containsAny(text, "고철", "동물", "비료", "냄새", "오염")) {
            codes.add("ODOROUS"); maxRate = maxRate.max(BigDecimal.valueOf(0.20));
        }
        if (containsAny(text, "식용유", "시럽", "음료원액", "말통")) {
            codes.add("LIQUID_BULK"); maxRate = maxRate.max(BigDecimal.valueOf(0.15));
        }
        return new SpecialCargoResult(codes, maxRate);
    }

    private String extractPackagingType(String text) {
        Matcher palletMatcher = PALLET_PATTERN.matcher(text);
        if (palletMatcher.find()) {
            String count = palletMatcher.group(1) != null ? palletMatcher.group(1) : palletMatcher.group(2);
            return count == null ? "파렛트" : "파렛트 " + count + "개";
        }
        if (containsAny(text, "컨테이너")) {
            return "컨테이너";
        }
        if (containsAny(text, "종이박스", "종이 박스")) {
            return "종이박스";
        }
        Matcher boxMatcher = BOX_PATTERN.matcher(text);
        if (boxMatcher.find()) {
            String count = boxMatcher.group(1) != null ? boxMatcher.group(1) : boxMatcher.group(2);
            String material = containsAny(text, "종이박스", "종이 박스") ? "종이박스" : "BOX";
            return count == null ? material : count + material;
        }
        return "박스";
    }

    private List<String> extractDetectedItems(String text) {
        List<String> items = new ArrayList<>();
        Matcher matcher = ITEM_WEIGHT_PATTERN.matcher(text);
        while (matcher.find()) {
            String name = matcher.group(1).trim()
                    .replaceAll("^(?:그리고|첨부 문서.*?결과:|품목\\s*)", "")
                    .replaceAll(".*[\\n\\t|]", "")
                    .replaceAll("^(?:과|와)\\s+", "")
                    .trim();
            name = name.replaceFirst("^(.+?)\\s+\\1$", "$1");
            if (name.length() < 2 || containsAny(name, "총중량", "중량", "무게", "박스")) continue;
            BigDecimal weight = new BigDecimal(matcher.group(2).replace(",", ""));
            if (matcher.group(3).startsWith("톤") || matcher.group(3).equalsIgnoreCase("t")) {
                weight = weight.multiply(BigDecimal.valueOf(1000));
            }
            String item = name + " " + weight.stripTrailingZeros().toPlainString() + "kg";
            if (!items.contains(item)) items.add(item);
        }
        return items.size() > 1 ? List.copyOf(items) : List.of();
    }

    private List<String> extractAnalysisWarnings(String text, BigDecimal temperature,
                                                 HazardResult hazard, List<String> detectedItems) {
        List<String> warnings = new ArrayList<>();
        if (text.contains("화물유형: 일반화물") && containsAny(text, "냉동", "냉장", "신선")) {
            warnings.add("선택한 일반화물과 분석된 냉동·신선 조건이 다릅니다. 화물 유형을 확인해주세요.");
        }
        if (temperature == null && containsAny(text, "신선", "너무 덥지")) {
            warnings.add("구체적인 운송 온도가 없어 임의의 온도를 만들지 않았습니다. 적정 온도를 확인해주세요.");
        }
        if (hazard.detected() && hazard.classCode() == null) {
            warnings.add("위험 가능성은 감지했지만 등급은 확정하지 않았습니다. 제품 MSDS의 운송 정보를 확인해주세요.");
        }
        if (containsAny(text, "대형 기계", "크기가 상당히 큰")) {
            warnings.add("기계 규격·부피와 컨테이너 적합성을 확인한 뒤 철도 운송 가능 여부를 판단해야 합니다.");
        }
        if (detectedItems.size() > 1) {
            warnings.add("복수 품목을 각각 분리해 감지했으며 총중량은 품목별 중량을 합산했습니다.");
        }
        return List.copyOf(warnings);
    }

    private String extractHandlingNote(String text, List<String> specialCodes) {
        List<String> notes = new ArrayList<>(specialCodes.stream().map(code -> switch (code) {
            case "OVERWEIGHT" -> "초과 중량 화물";
            case "FRAGILE" -> "파손 주의 화물";
            case "ODOROUS" -> "악취·오염 주의 화물";
            case "LIQUID_BULK" -> "액상 벌크 화물";
            default -> code;
        }).toList());
        if (containsAny(text, "상단 적재 금지", "상단적재금지")) notes.add("상단 적재 금지");
        if (containsAny(text, "충격", "취급주의", "취급 주의")) notes.add("충격·취급 주의");
        return notes.isEmpty() ? null : String.join(", ", notes);
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private record ColdChainResult(TemperatureCondition condition, BigDecimal surchargeRate,
                                   BigDecimal fixedPowerFee) { }
    private record HazardResult(boolean detected, String classCode, String className, HazardGrade grade,
                                boolean rejected, boolean requiresMsds, BigDecimal surchargeRate) { }
    private record SpecialCargoResult(List<String> codes, BigDecimal maxRate) { }
}
