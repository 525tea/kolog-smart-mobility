package com.smbility.railcargo.common;

import static org.assertj.core.api.Assertions.assertThat;

import com.smbility.railcargo.cargo.domain.HazardGrade;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class PricingPolicyTest {

    @Test
    void 위험물_등급이_없으면_할증률은_0이다() {
        assertThat(PricingPolicy.hazardSurchargeRate(null)).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void 운송_가능한_위험물은_등급과_무관하게_20퍼센트다() {
        assertThat(PricingPolicy.hazardSurchargeRate(HazardGrade.A)).isEqualByComparingTo(BigDecimal.valueOf(0.20));
        assertThat(PricingPolicy.hazardSurchargeRate(HazardGrade.D)).isEqualByComparingTo(BigDecimal.valueOf(0.20));
    }

    @Test
    void 화물가액을_신고하지_않으면_보험료는_0원이다() {
        assertThat(PricingPolicy.insurancePremium(null)).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(PricingPolicy.insurancePremium(BigDecimal.ZERO)).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void 화물가액이_충분히_크면_요율대로_보험료를_계산한다() {
        // 1000만원 * 0.3% = 30,000원 (최소 보험료 1,000원보다 크므로 요율 그대로 적용)
        BigDecimal premium = PricingPolicy.insurancePremium(BigDecimal.valueOf(10_000_000));

        assertThat(premium).isEqualByComparingTo(BigDecimal.valueOf(30_000));
    }

    @Test
    void 화물가액이_작으면_최소_보험료가_적용된다() {
        // 1만원 * 0.3% = 30원 → 최소 보험료 1,000원이 적용돼야 한다
        BigDecimal premium = PricingPolicy.insurancePremium(BigDecimal.valueOf(10_000));

        assertThat(premium).isEqualByComparingTo(PricingPolicy.MIN_INSURANCE_PREMIUM);
    }

    @Test
    void 운임의_5퍼센트가_플랫폼_수수료로_계산된다() {
        BigDecimal platformFee = PricingPolicy.platformFee(BigDecimal.valueOf(100_000));

        assertThat(platformFee).isEqualByComparingTo(BigDecimal.valueOf(5_000));
    }

    @Test
    void 운임이_없으면_플랫폼_수수료도_0이다() {
        assertThat(PricingPolicy.platformFee(null)).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
