package com.smbility.railcargo.pricing;

import com.smbility.railcargo.common.PricingPolicy;
import com.smbility.railcargo.consolidation.domain.ConsolidatedCargo;
import com.smbility.railcargo.matching.service.RecruitmentSimulationService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 기획안 보충 "4) 동적 가격 구현 - 1단계"의 시간대별 규칙을 그대로 구현한 규칙 기반 동적 가격 서비스.
 *
 * <pre>
 * T-24h : 기준가격
 * T-12h : 성립확률이 낮으면 5% 할인
 * T-6h  : 잔여용량이 많으면(모집률이 낮으면) 10% 할인
 * T-4h  : 손익분기(최소 성립 기준) 이상이면 추가 물량 확보를 위해 최대 15% 할인
 * 단, 이미 목표중량을 채워 수요가 충분하면 마감이 임박해도 할인하지 않는다 (코레일 수익 우선).
 * 할인 후 가격이 하한(MIN_RATE_PER_KG) 밑으로 내려가야 한다면 상품을 미성립 처리한다.
 * </pre>
 *
 * <p>"T-N시간"의 기준 시점은 열차 출발시각이 아직 정해지지 않은 모집 단계에서도 계산할 수 있도록
 * {@code recruitmentDeadline}(모집 마감시각)까지 남은 시간으로 해석한다.
 * "성립확률"은 {@link RecruitmentSimulationService}의 몬테카를로 추정치를 그대로 사용한다.
 * "손익분기"는 별도의 원가 모델이 없어, 목표중량의 {@link #BREAKEVEN_RECRUITMENT_RATE_PERCENT}
 * 이상을 채운 상태로 근사했다 — 실제 원가 데이터가 확보되면 이 임계치를 교체해야 한다.
 */
@Service
@RequiredArgsConstructor
public class TieredDynamicPricingService implements DynamicPricingService {

    private static final BigDecimal SUFFICIENT_DEMAND_RECRUITMENT_RATE_PERCENT = BigDecimal.valueOf(100);
    private static final BigDecimal BREAKEVEN_RECRUITMENT_RATE_PERCENT = BigDecimal.valueOf(60);
    private static final BigDecimal LOW_RECRUITMENT_RATE_PERCENT = BigDecimal.valueOf(50);
    private static final BigDecimal LOW_SUCCESS_PROBABILITY_PERCENT = BigDecimal.valueOf(50);

    private static final BigDecimal DISCOUNT_T12 = BigDecimal.valueOf(0.05);
    private static final BigDecimal DISCOUNT_T6 = BigDecimal.valueOf(0.10);
    private static final BigDecimal DISCOUNT_T4 = BigDecimal.valueOf(0.15);
    private static final BigDecimal NO_DISCOUNT = BigDecimal.ZERO;

    private final RecruitmentSimulationService recruitmentSimulationService;

    @Override
    public PriceQuote quote(ConsolidatedCargo consolidatedCargo) {
        BigDecimal recruitmentRate = consolidatedCargo.getRecruitmentRatePercent();

        Tier tier = decideTier(consolidatedCargo, recruitmentRate);

        BigDecimal candidateRate = PricingPolicy.RATE_PER_KG
                .multiply(BigDecimal.ONE.subtract(tier.discountRate()))
                .setScale(2, RoundingMode.HALF_UP);

        if (candidateRate.compareTo(PricingPolicy.MIN_RATE_PER_KG) < 0) {
            return new PriceQuote(PricingPolicy.MIN_RATE_PER_KG, tier.discountRate(), false,
                    "할인 적용 시 가격 하한(" + PricingPolicy.MIN_RATE_PER_KG + "원/kg) 미만이라 상품을 미성립 처리해야 합니다.");
        }
        return new PriceQuote(candidateRate, tier.discountRate(), true, tier.reason());
    }

    private Tier decideTier(ConsolidatedCargo consolidatedCargo, BigDecimal recruitmentRate) {
        if (recruitmentRate.compareTo(SUFFICIENT_DEMAND_RECRUITMENT_RATE_PERCENT) >= 0) {
            return new Tier(NO_DISCOUNT, "목표중량 달성(수요 충분) - 할인 없음, 코레일 수익 우선");
        }

        long hoursUntilDeadline = hoursUntil(consolidatedCargo.getRecruitmentDeadline());

        if (hoursUntilDeadline <= 4 && recruitmentRate.compareTo(BREAKEVEN_RECRUITMENT_RATE_PERCENT) >= 0) {
            return new Tier(DISCOUNT_T4, "T-4h, 손익분기(" + BREAKEVEN_RECRUITMENT_RATE_PERCENT + "%) 이상 - 최대 15% 할인");
        }
        if (hoursUntilDeadline <= 6 && recruitmentRate.compareTo(LOW_RECRUITMENT_RATE_PERCENT) < 0) {
            return new Tier(DISCOUNT_T6, "T-6h, 잔여용량 많음(모집률 " + LOW_RECRUITMENT_RATE_PERCENT + "% 미만) - 10% 할인");
        }
        if (hoursUntilDeadline <= 12
                && recruitmentSimulationService.estimateSuccessProbability(consolidatedCargo)
                        .compareTo(LOW_SUCCESS_PROBABILITY_PERCENT) < 0) {
            return new Tier(DISCOUNT_T12, "T-12h, 성립확률 낮음 - 5% 할인");
        }
        return new Tier(NO_DISCOUNT, "T-24h 이상 남았거나 할인 조건 미충족 - 기준가격");
    }

    private long hoursUntil(LocalDateTime deadline) {
        long hours = Duration.between(LocalDateTime.now(), deadline).toMinutes() / 60;
        return Math.max(hours, 0);
    }

    private record Tier(BigDecimal discountRate, String reason) {
    }
}
