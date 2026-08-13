package com.smbility.railcargo.matching.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.smbility.railcargo.auth.domain.Member;
import com.smbility.railcargo.auth.domain.MemberRole;
import com.smbility.railcargo.cargo.domain.CargoOrder;
import com.smbility.railcargo.cargo.domain.TemperatureCondition;
import com.smbility.railcargo.cargo.repository.CargoOrderRepository;
import com.smbility.railcargo.consolidation.domain.ConsolidatedCargo;
import com.smbility.railcargo.shipper.domain.Shipper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MonteCarloRecruitmentSimulationServiceTest {

    @Mock
    private CargoOrderRepository cargoOrderRepository;

    @InjectMocks
    private MonteCarloRecruitmentSimulationService simulationService;

    private CargoOrder historicalOrder(BigDecimal weightKg) {
        Member member = Member.of("s@example.com", "encoded", MemberRole.SHIPPER);
        Shipper shipper = Shipper.of(member, "123-45-67890", "상사", "홍길동", "010-0000-0000");
        CargoOrder order = CargoOrder.register(shipper, "화물", null, "천안", "서울", LocalDate.now().plusDays(1));
        order.applyAiAnalysis(weightKg, BigDecimal.ONE, TemperatureCondition.ROOM, false, null, "박스", null);
        return order;
    }

    @Test
    void 목표중량에_이미_도달했으면_100을_반환한다() {
        ConsolidatedCargo group = ConsolidatedCargo.open("천안", "서울", TemperatureCondition.ROOM, false,
                BigDecimal.valueOf(500), LocalDateTime.now().plusHours(5));
        group.addParticipation(BigDecimal.valueOf(500));

        BigDecimal probability = simulationService.estimateSuccessProbability(group);

        assertThat(probability).isEqualByComparingTo(BigDecimal.valueOf(100));
    }

    @Test
    void 마감시간이_지났으면_0을_반환한다() {
        ConsolidatedCargo group = ConsolidatedCargo.open("천안", "서울", TemperatureCondition.ROOM, false,
                BigDecimal.valueOf(500), LocalDateTime.now().minusMinutes(1));

        BigDecimal probability = simulationService.estimateSuccessProbability(group);

        assertThat(probability).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void 수요가_충분하고_시간이_넉넉하면_성립확률이_높게_나온다() {
        List<CargoOrder> busyHistory = java.util.stream.IntStream.range(0, 30)
                .mapToObj(i -> historicalOrder(BigDecimal.valueOf(300)))
                .toList();
        when(cargoOrderRepository.findAllByOriginStationAndDestinationStationAndCreatedAtAfter(
                anyString(), anyString(), any())).thenReturn(busyHistory);

        ConsolidatedCargo group = ConsolidatedCargo.open("천안", "서울", TemperatureCondition.ROOM, false,
                BigDecimal.valueOf(500), LocalDateTime.now().plusDays(3));
        group.addParticipation(BigDecimal.valueOf(100)); // 400kg 부족, 3일 동안 시간당 30/(14*24)건 * 300kg 도착 가정

        BigDecimal probability = simulationService.estimateSuccessProbability(group);

        assertThat(probability.doubleValue()).isGreaterThan(60.0);
    }

    @Test
    void 이력이_없고_마감이_임박한데_부족량이_크면_성립확률이_낮게_나온다() {
        when(cargoOrderRepository.findAllByOriginStationAndDestinationStationAndCreatedAtAfter(
                anyString(), anyString(), any())).thenReturn(List.of());

        ConsolidatedCargo group = ConsolidatedCargo.open("천안", "서울", TemperatureCondition.ROOM, false,
                BigDecimal.valueOf(2000), LocalDateTime.now().plusMinutes(30));
        group.addParticipation(BigDecimal.valueOf(50)); // 1950kg 부족, 30분밖에 안남음

        BigDecimal probability = simulationService.estimateSuccessProbability(group);

        assertThat(probability.doubleValue()).isLessThan(20.0);
    }
}
