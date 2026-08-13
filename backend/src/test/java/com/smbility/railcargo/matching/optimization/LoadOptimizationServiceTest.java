package com.smbility.railcargo.matching.optimization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.smbility.railcargo.cargo.domain.TemperatureCondition;
import com.smbility.railcargo.consolidation.domain.ConsolidatedCargo;
import com.smbility.railcargo.consolidation.domain.ConsolidationStatus;
import com.smbility.railcargo.consolidation.repository.ConsolidatedCargoRepository;
import com.smbility.railcargo.matching.dto.MatchPredictionResponse;
import com.smbility.railcargo.matching.repository.MatchPredictionRepository;
import com.smbility.railcargo.matching.service.MatchPredictionCalculator;
import com.smbility.railcargo.pricing.DynamicPricingService;
import com.smbility.railcargo.pricing.PriceQuote;
import com.smbility.railcargo.train.domain.Train;
import com.smbility.railcargo.train.domain.Wagon;
import com.smbility.railcargo.train.domain.WagonType;
import com.smbility.railcargo.train.service.TrainService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class LoadOptimizationServiceTest {

    @Mock
    private ConsolidatedCargoRepository consolidatedCargoRepository;

    @Mock
    private TrainService trainService;

    @Mock
    private MatchPredictionRepository matchPredictionRepository;

    @Mock
    private DynamicPricingService dynamicPricingService;

    // @InjectMocks는 @Mock 필드만 생성자에 채워주므로, 순수 객체인 계산기는 직접 생성해서 수동으로 조립한다.
    private LoadOptimizationService loadOptimizationService;

    @BeforeEach
    void setUp() {
        lenient().when(dynamicPricingService.quote(any()))
                .thenReturn(new PriceQuote(BigDecimal.valueOf(500), BigDecimal.ZERO, true, "기준가격"));
        loadOptimizationService = new LoadOptimizationService(consolidatedCargoRepository, trainService,
                matchPredictionRepository, new MatchPredictionCalculator(dynamicPricingService));
    }

    /** JPA가 없는 순수 단위 테스트라 실제 저장 없이 id를 직접 채워준다 (IDENTITY 전략을 흉내). */
    private final AtomicLong idSequence = new AtomicLong(1);

    private Train train() {
        return Train.of("KTX-1", "천안", "서울",
                LocalDateTime.now().plusHours(8), LocalDateTime.now().plusHours(9), LocalDateTime.now().plusHours(6));
    }

    private Wagon wagon(BigDecimal maxWeightKg) {
        Wagon wagon = Wagon.of(train(), "W-" + idSequence.get(), WagonType.CONTAINER, maxWeightKg, false);
        ReflectionTestUtils.setField(wagon, "id", idSequence.getAndIncrement());
        return wagon;
    }

    private ConsolidatedCargo readyGroup(BigDecimal weightKg) {
        ConsolidatedCargo group = ConsolidatedCargo.open("천안", "서울", TemperatureCondition.ROOM, false,
                weightKg, LocalDateTime.now().plusDays(1));
        ReflectionTestUtils.setField(group, "id", idSequence.getAndIncrement());
        group.addParticipation(weightKg);
        group.markReadyForMatching();
        return group;
    }

    @Test
    void 대기중인_그룹이_없으면_아무것도_하지_않는다() {
        when(consolidatedCargoRepository.findAllByStatus(ConsolidationStatus.READY_FOR_MATCHING)).thenReturn(List.of());

        List<MatchPredictionResponse> results = loadOptimizationService.optimizeReadyGroups();

        assertThat(results).isEmpty();
    }

    @Test
    void 이용가능한_화차가_없으면_그룹은_대기_상태로_남는다() {
        ConsolidatedCargo group = readyGroup(BigDecimal.valueOf(500));
        when(consolidatedCargoRepository.findAllByStatus(ConsolidationStatus.READY_FOR_MATCHING)).thenReturn(List.of(group));
        when(trainService.findEligibleWagons(any(), any(), any(), any(), anyBoolean(), any())).thenReturn(List.of());

        List<MatchPredictionResponse> results = loadOptimizationService.optimizeReadyGroups();

        assertThat(results).isEmpty();
        assertThat(group.getStatus()).isEqualTo(ConsolidationStatus.READY_FOR_MATCHING);
    }

    @Test
    void 화차를_찾으면_배정하고_승인대기로_전환한다() {
        ConsolidatedCargo group = readyGroup(BigDecimal.valueOf(500));
        Wagon wagon = wagon(BigDecimal.valueOf(1000));

        when(consolidatedCargoRepository.findAllByStatus(ConsolidationStatus.READY_FOR_MATCHING)).thenReturn(List.of(group));
        when(trainService.findEligibleWagons(any(), any(), any(), any(), anyBoolean(), any())).thenReturn(List.of(wagon));
        when(matchPredictionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        List<MatchPredictionResponse> results = loadOptimizationService.optimizeReadyGroups();

        assertThat(results).hasSize(1);
        assertThat(group.getStatus()).isEqualTo(ConsolidationStatus.PENDING_APPROVAL);
        assertThat(group.getMatchedWagon()).isEqualTo(wagon);
        assertThat(wagon.getRemainingWeightKg()).isEqualByComparingTo(BigDecimal.valueOf(500));
    }

    @Test
    void 여러_그룹을_하나의_화차_용량에_맞춰_함께_배정한다() {
        ConsolidatedCargo groupA = readyGroup(BigDecimal.valueOf(700));
        ConsolidatedCargo groupB = readyGroup(BigDecimal.valueOf(300));
        Wagon wagon = wagon(BigDecimal.valueOf(1000));

        when(consolidatedCargoRepository.findAllByStatus(ConsolidationStatus.READY_FOR_MATCHING))
                .thenReturn(List.of(groupA, groupB));
        when(trainService.findEligibleWagons(any(), any(), any(), any(), anyBoolean(), any())).thenReturn(List.of(wagon));
        when(matchPredictionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        List<MatchPredictionResponse> results = loadOptimizationService.optimizeReadyGroups();

        assertThat(results).hasSize(2);
        assertThat(groupA.getStatus()).isEqualTo(ConsolidationStatus.PENDING_APPROVAL);
        assertThat(groupB.getStatus()).isEqualTo(ConsolidationStatus.PENDING_APPROVAL);
        assertThat(wagon.getRemainingWeightKg()).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
