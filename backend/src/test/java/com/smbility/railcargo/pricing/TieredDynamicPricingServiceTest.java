package com.smbility.railcargo.pricing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.smbility.railcargo.cargo.domain.TemperatureCondition;
import com.smbility.railcargo.consolidation.domain.ConsolidatedCargo;
import com.smbility.railcargo.matching.service.RecruitmentSimulationService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TieredDynamicPricingServiceTest {

    @Mock
    private RecruitmentSimulationService recruitmentSimulationService;

    @InjectMocks
    private TieredDynamicPricingService pricingService;

    private ConsolidatedCargo group(BigDecimal targetWeightKg, BigDecimal recruitedWeightKg, LocalDateTime deadline) {
        ConsolidatedCargo group = ConsolidatedCargo.open("천안", "서울", TemperatureCondition.ROOM, false,
                targetWeightKg, deadline);
        if (recruitedWeightKg.signum() > 0) {
            group.addParticipation(recruitedWeightKg);
        }
        return group;
    }

    @Test
    void 마감까지_24시간_넘게_남았으면_기준가격이다() {
        ConsolidatedCargo group = group(BigDecimal.valueOf(1000), BigDecimal.valueOf(100), LocalDateTime.now().plusHours(30));

        PriceQuote quote = pricingService.quote(group);

        assertThat(quote.discountRate()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(quote.ratePerKg()).isEqualByComparingTo(BigDecimal.valueOf(500));
        assertThat(quote.feasible()).isTrue();
    }

    @Test
    void 목표중량을_채우면_마감이_임박해도_할인하지_않는다() {
        ConsolidatedCargo group = group(BigDecimal.valueOf(500), BigDecimal.valueOf(500), LocalDateTime.now().plusHours(1));

        PriceQuote quote = pricingService.quote(group);

        assertThat(quote.discountRate()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void T4시간_이내_손익분기_이상이면_15프로_할인한다() {
        // target 1000, recruited 650 -> 65% (>= 60% breakeven), 마감 3시간 전
        ConsolidatedCargo group = group(BigDecimal.valueOf(1000), BigDecimal.valueOf(650), LocalDateTime.now().plusHours(3));

        PriceQuote quote = pricingService.quote(group);

        assertThat(quote.discountRate()).isEqualByComparingTo(BigDecimal.valueOf(0.15));
        assertThat(quote.ratePerKg()).isEqualByComparingTo(BigDecimal.valueOf(425)); // 500 * 0.85
    }

    @Test
    void T6시간_이내_모집률이_낮으면_10프로_할인한다() {
        // target 1000, recruited 200 -> 20% (< 50%), 마감 5시간 전 (T-4h 조건은 breakeven 미달이라 통과 못함)
        ConsolidatedCargo group = group(BigDecimal.valueOf(1000), BigDecimal.valueOf(200), LocalDateTime.now().plusHours(5));

        PriceQuote quote = pricingService.quote(group);

        assertThat(quote.discountRate()).isEqualByComparingTo(BigDecimal.valueOf(0.10));
    }

    @Test
    void T12시간_이내_성립확률이_낮으면_5프로_할인한다() {
        ConsolidatedCargo group = group(BigDecimal.valueOf(1000), BigDecimal.valueOf(600), LocalDateTime.now().plusHours(10));
        when(recruitmentSimulationService.estimateSuccessProbability(any())).thenReturn(BigDecimal.valueOf(20));

        PriceQuote quote = pricingService.quote(group);

        assertThat(quote.discountRate()).isEqualByComparingTo(BigDecimal.valueOf(0.05));
    }

    @Test
    void 할인후_가격이_하한보다_낮아지지_않는다() {
        // 하한(350원)보다 낮아지는 시나리오는 현재 상수(최대 15% 할인, 하한 30% 여유)로는 발생하지 않지만
        // feasible 플래그 로직 자체는 항상 검증한다.
        ConsolidatedCargo group = group(BigDecimal.valueOf(1000), BigDecimal.valueOf(650), LocalDateTime.now().plusHours(3));

        PriceQuote quote = pricingService.quote(group);

        assertThat(quote.ratePerKg()).isGreaterThanOrEqualTo(BigDecimal.valueOf(350));
        assertThat(quote.feasible()).isTrue();
    }
}
