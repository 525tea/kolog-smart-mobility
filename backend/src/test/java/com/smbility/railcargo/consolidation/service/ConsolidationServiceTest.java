package com.smbility.railcargo.consolidation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.smbility.railcargo.auth.domain.Member;
import com.smbility.railcargo.auth.domain.MemberRole;
import com.smbility.railcargo.cargo.domain.CargoOrder;
import com.smbility.railcargo.cargo.domain.TemperatureCondition;
import com.smbility.railcargo.cargo.service.CargoService;
import com.smbility.railcargo.common.PricingPolicy;
import com.smbility.railcargo.consolidation.domain.CargoParticipation;
import com.smbility.railcargo.consolidation.domain.ConsolidatedCargo;
import com.smbility.railcargo.consolidation.domain.ConsolidationStatus;
import com.smbility.railcargo.consolidation.domain.FailurePreference;
import com.smbility.railcargo.consolidation.dto.ConsolidationDetailResponse;
import com.smbility.railcargo.consolidation.repository.CargoParticipationRepository;
import com.smbility.railcargo.consolidation.repository.ConsolidatedCargoRepository;
import com.smbility.railcargo.matching.optimization.LoadOptimizationService;
import com.smbility.railcargo.matching.service.RecruitmentSimulationService;
import com.smbility.railcargo.notification.service.NotificationService;
import com.smbility.railcargo.pricing.DynamicPricingService;
import com.smbility.railcargo.pricing.PriceQuote;
import com.smbility.railcargo.shipper.domain.Shipper;
import com.smbility.railcargo.train.domain.Train;
import com.smbility.railcargo.train.domain.Wagon;
import com.smbility.railcargo.train.service.TrainService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ConsolidationServiceTest {

    @Mock
    private ConsolidatedCargoRepository consolidatedCargoRepository;

    @Mock
    private CargoParticipationRepository cargoParticipationRepository;

    @Mock
    private CargoService cargoService;

    @Mock
    private LoadOptimizationService loadOptimizationService;

    @Mock
    private RecruitmentSimulationService recruitmentSimulationService;

    @Mock
    private DynamicPricingService dynamicPricingService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private TrainService trainService;

    @Mock
    private Wagon eligibleWagon;

    @Mock
    private Train eligibleTrain;

    @InjectMocks
    private ConsolidationService consolidationService;

    @org.junit.jupiter.api.BeforeEach
    void setUpPricing() {
        lenient().when(dynamicPricingService.quote(any()))
                .thenReturn(new PriceQuote(BigDecimal.valueOf(500), BigDecimal.ZERO, true, "기준가격"));
        lenient().when(trainService.findEligibleWagons(any(), any(), any(), any(),
                        org.mockito.ArgumentMatchers.anyBoolean(), any()))
                .thenReturn(List.of(eligibleWagon));
        lenient().when(eligibleWagon.getTrain()).thenReturn(eligibleTrain);
        lenient().when(eligibleTrain.getReservationDeadline()).thenReturn(LocalDateTime.now().plusHours(12));
    }

    private CargoOrder analyzedOrder(BigDecimal weightKg) {
        return analyzedOrder(weightKg, null);
    }

    private CargoOrder analyzedOrder(BigDecimal weightKg, BigDecimal declaredValueKrw) {
        Member member = Member.of("shipper@example.com", "encoded", MemberRole.SHIPPER);
        Shipper shipper = Shipper.of(member, "123-45-67890", "테스트상사", "홍길동", "010-0000-0000");
        CargoOrder order = CargoOrder.register(shipper, "화물", "설명", "천안", "서울", LocalDate.now().plusDays(1),
                declaredValueKrw);
        order.applyAiAnalysis(weightKg, BigDecimal.valueOf(0.5), TemperatureCondition.ROOM, false, null, "박스", null);
        return order;
    }

    @Test
    void 목표중량에_도달하면_매칭대기로_전환하고_최적화를_실행한다() {
        ConsolidatedCargo group = ConsolidatedCargo.open("천안", "서울", TemperatureCondition.ROOM, false,
                BigDecimal.valueOf(500), LocalDateTime.now().plusDays(1));
        CargoOrder order = analyzedOrder(BigDecimal.valueOf(500));

        when(consolidatedCargoRepository.findById(anyLong())).thenReturn(java.util.Optional.of(group));
        when(cargoService.getOwnedEntity(anyLong(), anyLong())).thenReturn(order);
        when(cargoParticipationRepository.findAllByConsolidatedCargoId(any())).thenReturn(List.of());

        ConsolidationDetailResponse response = consolidationService.join(1L, 1L, 1L, null);

        // 실제 배정(READY_FOR_MATCHING -> MATCHED -> PENDING_APPROVAL)은 LoadOptimizationService의 책임이라
        // 여기서는 mock이라 실행되지 않는다. ConsolidationService는 "매칭 대기로 전환 + 최적화 트리거"까지만 검증한다.
        verify(loadOptimizationService).optimizeReadyGroups();
        assertThat(response.status()).isEqualTo(ConsolidationStatus.READY_FOR_MATCHING);
    }

    @Test
    void 참여시_배분비용에_적재보험료와_플랫폼수수료가_포함된다() {
        ConsolidatedCargo group = ConsolidatedCargo.open("천안", "서울", TemperatureCondition.ROOM, false,
                BigDecimal.valueOf(800), LocalDateTime.now().plusDays(1));
        // 화물가액 1000만원 신고 -> 보험료 = 1000만 * 0.3% = 30,000원
        CargoOrder order = analyzedOrder(BigDecimal.valueOf(200), BigDecimal.valueOf(10_000_000));

        when(consolidatedCargoRepository.findById(anyLong())).thenReturn(java.util.Optional.of(group));
        when(cargoService.getOwnedEntity(anyLong(), anyLong())).thenReturn(order);
        when(cargoParticipationRepository.findAllByConsolidatedCargoId(any())).thenReturn(List.of());

        consolidationService.join(1L, 1L, 1L, null);

        ArgumentCaptor<CargoParticipation> captor = ArgumentCaptor.forClass(CargoParticipation.class);
        verify(cargoParticipationRepository).save(captor.capture());

        // 기준가격 quote(500원/kg, 위험물 아님) * 200kg = 100,000원(운임)
        BigDecimal freight = BigDecimal.valueOf(100_000);
        BigDecimal insuranceFee = PricingPolicy.insurancePremium(BigDecimal.valueOf(10_000_000));
        BigDecimal platformFee = PricingPolicy.platformFee(freight);
        BigDecimal expectedTotal = freight.add(insuranceFee).add(platformFee);

        assertThat(captor.getValue().getAllocatedCost()).isEqualByComparingTo(expectedTotal);
    }

    @Test
    void 목표중량_미달이면_최적화를_실행하지_않는다() {
        ConsolidatedCargo group = ConsolidatedCargo.open("천안", "서울", TemperatureCondition.ROOM, false,
                BigDecimal.valueOf(800), LocalDateTime.now().plusDays(1));
        CargoOrder order = analyzedOrder(BigDecimal.valueOf(200));

        when(consolidatedCargoRepository.findById(anyLong())).thenReturn(java.util.Optional.of(group));
        when(cargoService.getOwnedEntity(anyLong(), anyLong())).thenReturn(order);
        when(cargoParticipationRepository.findAllByConsolidatedCargoId(any())).thenReturn(List.of());

        ConsolidationDetailResponse response = consolidationService.join(1L, 1L, 1L, null);

        verify(loadOptimizationService, never()).optimizeReadyGroups();
        assertThat(response.status()).isEqualTo(ConsolidationStatus.RECRUITING);
    }

    @Test
    void 운송조건이_다르면_참여할_수_없다() {
        ConsolidatedCargo group = ConsolidatedCargo.open("부산", "서울", TemperatureCondition.ROOM, false,
                BigDecimal.valueOf(500), LocalDateTime.now().plusDays(1));
        CargoOrder order = analyzedOrder(BigDecimal.valueOf(500)); // origin: 천안 (다름)

        when(consolidatedCargoRepository.findById(anyLong())).thenReturn(java.util.Optional.of(group));
        when(cargoService.getOwnedEntity(anyLong(), anyLong())).thenReturn(order);

        org.junit.jupiter.api.Assertions.assertThrows(
                com.smbility.railcargo.common.exception.BusinessException.class,
                () -> consolidationService.join(1L, 1L, 1L, null));
    }

    @Test
    void 최소중량_미만인_화물은_참여할_수_없다() {
        ConsolidatedCargo group = ConsolidatedCargo.open("천안", "서울", TemperatureCondition.ROOM, false,
                BigDecimal.valueOf(800), LocalDateTime.now().plusDays(1));
        CargoOrder order = analyzedOrder(BigDecimal.valueOf(50)); // 최소 기준(100kg) 미만

        when(consolidatedCargoRepository.findById(anyLong())).thenReturn(java.util.Optional.of(group));
        when(cargoService.getOwnedEntity(anyLong(), anyLong())).thenReturn(order);

        org.junit.jupiter.api.Assertions.assertThrows(
                com.smbility.railcargo.common.exception.BusinessException.class,
                () -> consolidationService.join(1L, 1L, 1L, null));
    }

    @Test
    void AUTO_REFUND_참여자는_실패시_주문이_취소된다() {
        ConsolidatedCargo group = ConsolidatedCargo.open("천안", "서울", TemperatureCondition.ROOM, false,
                BigDecimal.valueOf(800), LocalDateTime.now().plusDays(1));
        ReflectionTestUtils.setField(group, "id", 1L);
        CargoOrder order = analyzedOrder(BigDecimal.valueOf(200));
        order.markParticipating();
        CargoParticipation participation = CargoParticipation.of(order, group, BigDecimal.valueOf(100_000),
                FailurePreference.AUTO_REFUND);

        when(cargoParticipationRepository.findAllByConsolidatedCargoId(group.getId())).thenReturn(List.of(participation));

        consolidationService.processFailedGroup(group);

        assertThat(order.getStatus().name()).isEqualTo("CANCELLED");
    }

    @Test
    void NEXT_TRAIN_참여자는_다른_모집그룹으로_이월된다() {
        ConsolidatedCargo failedGroup = ConsolidatedCargo.open("천안", "서울", TemperatureCondition.ROOM, false,
                BigDecimal.valueOf(800), LocalDateTime.now().plusDays(1));
        ReflectionTestUtils.setField(failedGroup, "id", 1L);

        ConsolidatedCargo nextGroup = ConsolidatedCargo.open("천안", "서울", TemperatureCondition.ROOM, false,
                BigDecimal.valueOf(800), orderDesiredDate(), LocalDateTime.now().plusDays(2));
        ReflectionTestUtils.setField(nextGroup, "id", 2L);

        CargoOrder order = analyzedOrder(BigDecimal.valueOf(200));
        order.markParticipating();
        CargoParticipation participation = CargoParticipation.of(order, failedGroup, BigDecimal.valueOf(100_000),
                FailurePreference.NEXT_TRAIN);

        when(cargoParticipationRepository.findAllByConsolidatedCargoId(failedGroup.getId())).thenReturn(List.of(participation));
        when(consolidatedCargoRepository
                .findAllByOriginStationAndDestinationStationAndTemperatureConditionAndHazardousAndDesiredDateAndStatus(
                        "천안", "서울", TemperatureCondition.ROOM, false, orderDesiredDate(),
                        ConsolidationStatus.RECRUITING))
                .thenReturn(List.of(nextGroup));

        consolidationService.processFailedGroup(failedGroup);

        assertThat(participation.isSuperseded()).isTrue();
        assertThat(nextGroup.getRecruitedWeightKg()).isEqualByComparingTo(BigDecimal.valueOf(200));
        assertThat(order.getStatus().name()).isEqualTo("PARTICIPATING"); // 취소되지 않고 유지됨
    }

    private LocalDate orderDesiredDate() {
        return LocalDate.now().plusDays(1);
    }

    @Test
    void 마감이_지난_모집중_그룹을_찾아_실패처리하고_취소한다() {
        ConsolidatedCargo expiredGroup = ConsolidatedCargo.open("천안", "서울", TemperatureCondition.ROOM, false,
                BigDecimal.valueOf(800), LocalDateTime.now().minusHours(1));
        ReflectionTestUtils.setField(expiredGroup, "id", 1L);

        when(consolidatedCargoRepository.findAllByStatusAndRecruitmentDeadlineBefore(
                org.mockito.ArgumentMatchers.eq(ConsolidationStatus.RECRUITING), any()))
                .thenReturn(List.of(expiredGroup));
        when(cargoParticipationRepository.findAllByConsolidatedCargoId(expiredGroup.getId())).thenReturn(List.of());

        int processed = consolidationService.handleExpiredRecruitingGroups();

        assertThat(processed).isEqualTo(1);
        assertThat(expiredGroup.getStatus()).isEqualTo(ConsolidationStatus.CANCELLED);
    }
}
