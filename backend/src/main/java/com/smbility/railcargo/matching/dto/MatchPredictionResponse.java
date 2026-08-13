package com.smbility.railcargo.matching.dto;

import com.smbility.railcargo.matching.domain.MatchPrediction;
import java.math.BigDecimal;

public record MatchPredictionResponse(
        Long id,
        Long consolidatedCargoId,
        Long wagonId,
        String wagonNumber,
        BigDecimal successProbability,
        BigDecimal expectedRevenue,
        BigDecimal expectedLoadFactor,
        BigDecimal contributionMargin
) {
    public static MatchPredictionResponse from(MatchPrediction prediction) {
        return new MatchPredictionResponse(
                prediction.getId(),
                prediction.getConsolidatedCargo().getId(),
                prediction.getWagon().getId(),
                prediction.getWagon().getWagonNumber(),
                prediction.getSuccessProbability(),
                prediction.getExpectedRevenue(),
                prediction.getExpectedLoadFactor(),
                prediction.getContributionMargin());
    }
}
