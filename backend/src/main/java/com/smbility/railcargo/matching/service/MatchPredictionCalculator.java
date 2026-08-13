package com.smbility.railcargo.matching.service;

import com.smbility.railcargo.common.PricingPolicy;
import com.smbility.railcargo.consolidation.domain.ConsolidatedCargo;
import com.smbility.railcargo.matching.domain.MatchPrediction;
import com.smbility.railcargo.pricing.DynamicPricingService;
import com.smbility.railcargo.train.domain.Wagon;
import java.math.BigDecimal;
import java.math.RoundingMode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * (공동화물, 화차) 배정이 확정된 뒤 성립확률/예상매출/공헌이익을 계산한다.
 * 실제 배정 알고리즘({@link com.smbility.railcargo.matching.optimization.LoadAssignmentSolver})과
 * 분리해두어, 가격·확률 모델만 나중에 교체하기 쉽게 했다.
 *
 * <p>성립확률은 현재 위험물 취급 여부만 반영하는 단순 규칙이다. 목표중량을 이미 채운 뒤의 확률이므로
 * (모집 단계의 성립확률은 {@link RecruitmentSimulationService} 참고) 값이 전반적으로 높게 나온다.
 * 예상매출은 매칭 시점의 동적 가격({@link DynamicPricingService})을 그대로 적용한다.
 */
@Component
@RequiredArgsConstructor
public class MatchPredictionCalculator {

    private static final BigDecimal BASE_SUCCESS_PROBABILITY = BigDecimal.valueOf(90);
    private static final BigDecimal SPECIAL_HANDLING_PENALTY = BigDecimal.valueOf(10);

    private final DynamicPricingService dynamicPricingService;

    /** 반드시 {@code wagon.allocate(...)}로 용량을 반영한 뒤에 호출해야 적재율이 정확히 계산된다. */
    public MatchPrediction calculate(ConsolidatedCargo consolidatedCargo, Wagon wagon) {
        BigDecimal successProbability = calculateSuccessProbability(consolidatedCargo);
        BigDecimal ratePerKg = dynamicPricingService.quote(consolidatedCargo).ratePerKg();
        BigDecimal expectedRevenue = consolidatedCargo.getRecruitedWeightKg().multiply(ratePerKg);
        BigDecimal expectedLoadFactor = wagon.getLoadFactorPercent();
        BigDecimal contributionMargin = expectedRevenue.multiply(PricingPolicy.CONTRIBUTION_MARGIN_RATE)
                .setScale(2, RoundingMode.HALF_UP);

        return MatchPrediction.of(consolidatedCargo, wagon, successProbability, expectedRevenue,
                expectedLoadFactor, contributionMargin);
    }

    private BigDecimal calculateSuccessProbability(ConsolidatedCargo consolidatedCargo) {
        BigDecimal probability = BASE_SUCCESS_PROBABILITY;
        if (consolidatedCargo.isHazardous()) {
            probability = probability.subtract(SPECIAL_HANDLING_PENALTY);
        }
        return probability.max(BigDecimal.ZERO).min(BigDecimal.valueOf(100));
    }
}
