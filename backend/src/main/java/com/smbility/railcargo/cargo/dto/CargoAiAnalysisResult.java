package com.smbility.railcargo.cargo.dto;

import com.smbility.railcargo.cargo.domain.TemperatureCondition;
import java.math.BigDecimal;
import java.util.List;

/**
 * AI 운송조건 추출 결과.
 * {@code RuleBasedCargoAiAnalysisService}(규칙 기반) 또는 {@code GeminiCargoAiAnalysisService}(LLM 기반)가 생성한다.
 *
 * @param lowConfidenceFields 신뢰도가 낮아 화주에게 확인을 받아야 하는 필드명 목록 (기획안 보충 5번
 *                            "신뢰도가 낮은 항목만 화주에게 확인받도록 구현" 참고). 규칙 기반 구현체는
 *                            숫자/키워드를 실제로 텍스트에서 찾지 못한 필드를 채워 넣는다.
 */
public record CargoAiAnalysisResult(
        BigDecimal weightKg,
        BigDecimal volumeCbm,
        TemperatureCondition temperatureCondition,
        boolean hazardous,
        String packagingType,
        String handlingNote,
        List<String> lowConfidenceFields
) {
    public boolean estimated() {
        return !lowConfidenceFields.isEmpty();
    }
}
