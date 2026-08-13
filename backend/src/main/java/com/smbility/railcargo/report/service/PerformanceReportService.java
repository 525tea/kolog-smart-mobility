package com.smbility.railcargo.report.service;

import com.smbility.railcargo.common.PricingPolicy;
import com.smbility.railcargo.consolidation.domain.CargoParticipation;
import com.smbility.railcargo.consolidation.repository.CargoParticipationRepository;
import com.smbility.railcargo.report.domain.PerformanceReport;
import com.smbility.railcargo.report.dto.PerformanceReportResponse;
import com.smbility.railcargo.report.repository.PerformanceReportRepository;
import com.smbility.railcargo.reservation.domain.Reservation;
import com.smbility.railcargo.train.domain.Wagon;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 열차/화차 단위 운송 성과 리포트 계산 (기획안 화면 O3, 6번 화면 설계 참고).
 * "before" 적재율은, 이번 예약으로 새로 채운 중량(recruitedWeightKg)을 현재 잔여용량에 되돌려 역산한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PerformanceReportService {

    private final PerformanceReportRepository performanceReportRepository;
    private final CargoParticipationRepository cargoParticipationRepository;

    @Transactional
    public PerformanceReportResponse generateForReservation(Reservation reservation) {
        Wagon wagon = reservation.getWagon();
        BigDecimal recruitedWeightKg = reservation.getConsolidatedCargo().getRecruitedWeightKg();

        BigDecimal afterLoadFactor = wagon.getLoadFactorPercent();
        BigDecimal beforeRemaining = wagon.getRemainingWeightKg().add(recruitedWeightKg);
        BigDecimal beforeLoadedWeight = wagon.getMaxWeightKg().subtract(beforeRemaining);
        BigDecimal beforeLoadFactor = wagon.getMaxWeightKg().signum() == 0
                ? BigDecimal.ZERO
                : beforeLoadedWeight.multiply(BigDecimal.valueOf(100))
                        .divide(wagon.getMaxWeightKg(), 1, RoundingMode.HALF_UP);

        BigDecimal costSavings = calculateCostSavings(reservation.getConsolidatedCargo().getId());
        BigDecimal carbonReductionKg = recruitedWeightKg.multiply(PricingPolicy.CARBON_REDUCTION_PER_KG)
                .setScale(2, RoundingMode.HALF_UP);

        PerformanceReport report = PerformanceReport.of(wagon.getTrain(), wagon, beforeLoadFactor, afterLoadFactor,
                reservation.getTotalCost(), costSavings, carbonReductionKg);

        return PerformanceReportResponse.from(performanceReportRepository.save(report));
    }

    public List<PerformanceReportResponse> getReportsForTrain(Long trainId) {
        return performanceReportRepository.findAllByTrainIdOrderByCreatedAtDesc(trainId).stream()
                .map(PerformanceReportResponse::from)
                .toList();
    }

    private BigDecimal calculateCostSavings(Long consolidatedCargoId) {
        List<CargoParticipation> participations =
                cargoParticipationRepository.findAllByConsolidatedCargoId(consolidatedCargoId);

        return participations.stream()
                .map(p -> p.getAllocatedCost().multiply(PricingPolicy.SOLO_TRUCK_COST_MULTIPLIER)
                        .subtract(p.getAllocatedCost()))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }
}
