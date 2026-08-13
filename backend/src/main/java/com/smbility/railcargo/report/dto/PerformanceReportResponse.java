package com.smbility.railcargo.report.dto;

import com.smbility.railcargo.report.domain.PerformanceReport;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PerformanceReportResponse(
        Long id,
        Long trainId,
        Long wagonId,
        BigDecimal beforeLoadFactor,
        BigDecimal afterLoadFactor,
        BigDecimal loadFactorImprovement,
        BigDecimal newRevenue,
        BigDecimal costSavings,
        BigDecimal carbonReductionKg,
        LocalDateTime generatedAt
) {
    public static PerformanceReportResponse from(PerformanceReport report) {
        return new PerformanceReportResponse(
                report.getId(),
                report.getTrain().getId(),
                report.getWagon().getId(),
                report.getBeforeLoadFactor(),
                report.getAfterLoadFactor(),
                report.getAfterLoadFactor().subtract(report.getBeforeLoadFactor()),
                report.getNewRevenue(),
                report.getCostSavings(),
                report.getCarbonReductionKg(),
                report.getCreatedAt());
    }
}
