package com.smbility.railcargo.matching.optimization;

import com.smbility.railcargo.consolidation.domain.ConsolidatedCargo;
import com.smbility.railcargo.consolidation.domain.ConsolidationStatus;
import com.smbility.railcargo.cargo.domain.TemperatureCondition;
import com.smbility.railcargo.consolidation.repository.ConsolidatedCargoRepository;
import com.smbility.railcargo.matching.domain.MatchPrediction;
import com.smbility.railcargo.matching.dto.MatchPredictionResponse;
import com.smbility.railcargo.matching.repository.MatchPredictionRepository;
import com.smbility.railcargo.matching.service.MatchPredictionCalculator;
import com.smbility.railcargo.train.domain.Wagon;
import com.smbility.railcargo.train.service.TrainService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 목표중량을 채운(READY_FOR_MATCHING) 공동화물들을 노선별로 묶어, 그 노선에서 이용 가능한 화차들과
 * 함께 {@link LoadAssignmentSolver}(OR-Tools CP-SAT)로 최적 배정을 계산한다.
 * 기획안 보충 "최적 적재 조합[AI]" 기능의 실제 구현체. 화주가 참여를 신청해 그룹이 목표중량에
 * 도달할 때마다({@code ConsolidationService.join}) 자동으로 호출되며, 동시에 여러 그룹이 대기 중이면
 * 하나의 배정 문제로 함께 풀어 개별적으로 그리디하게 배정할 때보다 전체 적재 효율을 높인다.
 */
@Service
@RequiredArgsConstructor
public class LoadOptimizationService {

    private final ConsolidatedCargoRepository consolidatedCargoRepository;
    private final TrainService trainService;
    private final MatchPredictionRepository matchPredictionRepository;
    private final MatchPredictionCalculator matchPredictionCalculator;
    private final LoadAssignmentSolver loadAssignmentSolver = new LoadAssignmentSolver();

    private record RouteKey(String originStation, String destinationStation, LocalDate desiredDate,
                            TemperatureCondition temperatureCondition, boolean hazardous) {
    }

    @Transactional
    public List<MatchPredictionResponse> optimizeReadyGroups() {
        List<ConsolidatedCargo> readyGroups =
                consolidatedCargoRepository.findAllByStatus(ConsolidationStatus.READY_FOR_MATCHING);
        if (readyGroups.isEmpty()) {
            return List.of();
        }

        Map<RouteKey, List<ConsolidatedCargo>> groupsByRoute = readyGroups.stream()
                .collect(Collectors.groupingBy(g -> new RouteKey(
                        g.getOriginStation(), g.getDestinationStation(), g.getDesiredDate(),
                        g.getTemperatureCondition(), g.isHazardous())));

        List<MatchPredictionResponse> results = new ArrayList<>();
        groupsByRoute.forEach((route, groups) -> results.addAll(optimizeRoute(route, groups)));
        return results;
    }

    private List<MatchPredictionResponse> optimizeRoute(RouteKey route, List<ConsolidatedCargo> groups) {
        List<Wagon> wagons = trainService.findEligibleWagons(
                route.originStation(), route.destinationStation(), route.desiredDate(),
                route.temperatureCondition(), route.hazardous(), BigDecimal.valueOf(0.01));
        if (wagons.isEmpty()) {
            return List.of();
        }

        List<LoadAssignmentSolver.LoadItem> items = groups.stream()
                .map(g -> new LoadAssignmentSolver.LoadItem(String.valueOf(g.getId()), g.getRecruitedWeightKg(), g.isHazardous()))
                .toList();
        List<LoadAssignmentSolver.BinCapacity> bins = wagons.stream()
                .map(w -> new LoadAssignmentSolver.BinCapacity(String.valueOf(w.getId()), w.getRemainingWeightKg(), w.isHazardousAllowed()))
                .toList();

        Map<String, String> assignment = loadAssignmentSolver.solve(items, bins);
        if (assignment.isEmpty()) {
            return List.of();
        }

        Map<Long, ConsolidatedCargo> groupById = groups.stream()
                .collect(Collectors.toMap(ConsolidatedCargo::getId, Function.identity()));
        Map<Long, Wagon> wagonById = wagons.stream()
                .collect(Collectors.toMap(Wagon::getId, Function.identity()));

        List<MatchPredictionResponse> results = new ArrayList<>();
        assignment.forEach((itemId, binId) -> {
            ConsolidatedCargo group = groupById.get(Long.valueOf(itemId));
            Wagon wagon = wagonById.get(Long.valueOf(binId));

            wagon.allocate(group.getRecruitedWeightKg());
            MatchPrediction prediction = matchPredictionCalculator.calculate(group, wagon);
            matchPredictionRepository.save(prediction);

            group.markMatched(wagon);
            group.markPendingApproval();

            results.add(MatchPredictionResponse.from(prediction));
        });
        return results;
    }
}
