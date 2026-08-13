package com.smbility.railcargo.common;

import com.smbility.railcargo.cargo.domain.HazardGrade;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

/**
 * 운임/수익성 계산에 쓰이는 mock 단가 정책.
 * 실제 코레일 운임 체계가 확정되면 이 상수들을 설정값(application.yml)이나 운임 테이블로 옮긴다.
 */
public final class PricingPolicy {

    /** 중량(kg) 당 공동운송 기준운임(원). 동적 가격은 이 값을 기준으로 할인율을 적용한다. */
    public static final BigDecimal RATE_PER_KG = BigDecimal.valueOf(500);

    /** kg당 최저 판매가(원). 동적 할인이 이 밑으로 내려가야 한다면 상품을 미성립 처리한다(가격 하한 정책). */
    public static final BigDecimal MIN_RATE_PER_KG = RATE_PER_KG.multiply(BigDecimal.valueOf(0.7));

    /** 개별 트럭 운송 대비 배수 (공동화물 운임 대비 트럭 단독 운송 비용). 절감액 산정에 사용. */
    public static final BigDecimal SOLO_TRUCK_COST_MULTIPLIER = BigDecimal.valueOf(1.4);

    /** 공헌이익률. */
    public static final BigDecimal CONTRIBUTION_MARGIN_RATE = BigDecimal.valueOf(0.35);

    /** kg당 대략적인 탄소배출 절감량(kg-CO2). 트럭 대비 철도 운송 절감 추정치. */
    public static final BigDecimal CARBON_REDUCTION_PER_KG = BigDecimal.valueOf(0.05);

    /**
     * Notion 기획안 기준으로 운송 가능한 2~9급 위험물은 등급과 무관하게 20%를 적용한다.
     */
    private static final Map<HazardGrade, BigDecimal> HAZARD_SURCHARGE_RATES = Map.of(
            HazardGrade.A, BigDecimal.valueOf(0.20),
            HazardGrade.B, BigDecimal.valueOf(0.20),
            HazardGrade.C, BigDecimal.valueOf(0.20),
            HazardGrade.D, BigDecimal.valueOf(0.20)
    );

    /** @return 위험물 등급이 없으면 0, 운송 가능한 위험물이면 20% */
    public static BigDecimal hazardSurchargeRate(HazardGrade hazardGrade) {
        if (hazardGrade == null) {
            return BigDecimal.ZERO;
        }
        return HAZARD_SURCHARGE_RATES.getOrDefault(hazardGrade, BigDecimal.ZERO);
    }

    /**
     * 적재보험료율(화주 신고 화물가액 대비). 국내 적하보험 실무 요율(통상 0.05~0.3%대로 알려짐)을 참고해
     * 보수적으로 가정한 값이며, 실제 보험사 요율표를 반영한 것은 아니다.
     */
    public static final BigDecimal INSURANCE_PREMIUM_RATE = BigDecimal.valueOf(0.003);

    /** 화물가액이 아주 적어도 최소한의 관리비용은 보전한다는 가정 하의 최소 보험료(원). */
    public static final BigDecimal MIN_INSURANCE_PREMIUM = BigDecimal.valueOf(1000);

    /**
     * 플랫폼 이용 수수료율(성사된 운임 대비). 기획안 보충의 "플랫폼 수수료" 항목을 반영하기 위한 가정 값이며,
     * 실제 서비스화 시 별도 정책 수립이 필요하다.
     */
    public static final BigDecimal PLATFORM_FEE_RATE = BigDecimal.valueOf(0.05);

    /**
     * 화주가 신고한 화물가액을 기준으로 적재보험료를 계산한다.
     *
     * @param declaredValueKrw 화주가 등록 시 신고한 화물가액(원). 신고하지 않았으면(null 또는 0 이하) 보험료는 0원.
     */
    public static BigDecimal insurancePremium(BigDecimal declaredValueKrw) {
        if (declaredValueKrw == null || declaredValueKrw.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal premium = declaredValueKrw.multiply(INSURANCE_PREMIUM_RATE).setScale(2, RoundingMode.HALF_UP);
        return premium.max(MIN_INSURANCE_PREMIUM);
    }

    /** 성사된 운임(위험물 할증 반영 후) 기준의 플랫폼 이용 수수료. */
    public static BigDecimal platformFee(BigDecimal freight) {
        if (freight == null) {
            return BigDecimal.ZERO;
        }
        return freight.multiply(PLATFORM_FEE_RATE).setScale(2, RoundingMode.HALF_UP);
    }

    private PricingPolicy() {
    }
}
