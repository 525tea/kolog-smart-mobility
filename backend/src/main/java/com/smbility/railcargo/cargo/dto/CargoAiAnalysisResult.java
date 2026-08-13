package com.smbility.railcargo.cargo.dto;

import com.smbility.railcargo.cargo.domain.TemperatureCondition;
import java.math.BigDecimal;

/**
 * AI 운송조건 추출 결과.
 * 실제 모델이 붙기 전까지는 {@code RuleBasedCargoAiAnalysisService}가 이 결과를 규칙 기반으로 생성한다.
 */
public record CargoAiAnalysisResult(
        BigDecimal weightKg,
        BigDecimal volumeCbm,
        TemperatureCondition temperatureCondition,
        boolean hazardous,
        String packagingType,
        String handlingNote,
        boolean estimated
) {
}
