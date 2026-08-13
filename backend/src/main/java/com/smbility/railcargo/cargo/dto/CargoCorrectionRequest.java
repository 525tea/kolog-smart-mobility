package com.smbility.railcargo.cargo.dto;

import com.smbility.railcargo.cargo.domain.HazardGrade;
import com.smbility.railcargo.cargo.domain.TemperatureCondition;
import java.math.BigDecimal;

/** 화주가 AI 분석 결과를 확인 후 수정하는 요청 (모든 필드 선택적). */
public record CargoCorrectionRequest(
        BigDecimal weightKg,
        BigDecimal volumeCbm,
        TemperatureCondition temperatureCondition,
        Boolean hazardous,
        HazardGrade hazardGrade,
        String packagingType,
        String handlingNote
) {
}
