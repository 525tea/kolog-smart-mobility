package com.smbility.railcargo.consolidation.dto;

import com.smbility.railcargo.cargo.domain.TemperatureCondition;
import com.smbility.railcargo.consolidation.domain.CargoParticipation;
import com.smbility.railcargo.consolidation.domain.ConsolidatedCargo;
import com.smbility.railcargo.consolidation.domain.ConsolidationStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;

public record ConsolidationDetailResponse(
        Long id,
        String originStation,
        String destinationStation,
        BigDecimal targetWeightKg,
        BigDecimal recruitedWeightKg,
        BigDecimal recruitmentRatePercent,
        LocalDate desiredDate,
        LocalDateTime recruitmentDeadline,
        ConsolidationStatus status,
        int participantCount,
        Long matchedWagonId,
        List<ParticipantSummary> participants
) {
    /** 화면 "공동화물 상세"의 "함께 운송되는 화물" 목록에 대응 (기획안 화면 7번). */
    public record ParticipantSummary(
            String companyName,
            String cargoName,
            BigDecimal weightKg,
            BigDecimal volumeCbm,
            TemperatureCondition temperatureCondition,
            boolean hazardous
    ) {
        static ParticipantSummary from(CargoParticipation participation) {
            var order = participation.getCargoOrder();
            return new ParticipantSummary(
                    order.getShipper().getCompanyName(),
                    order.getCargoName(),
                    order.getWeightKg(),
                    order.getVolumeCbm(),
                    order.getTemperatureCondition(),
                    order.isHazardous());
        }
    }

    public static ConsolidationDetailResponse from(ConsolidatedCargo cargo, List<CargoParticipation> participations) {
        Long wagonId = cargo.getMatchedWagon() != null ? cargo.getMatchedWagon().getId() : null;
        List<ParticipantSummary> participants = participations.stream()
                .filter(p -> !p.isSuperseded())
                .map(ParticipantSummary::from)
                .toList();
        return new ConsolidationDetailResponse(
                cargo.getId(),
                cargo.getOriginStation(),
                cargo.getDestinationStation(),
                cargo.getTargetWeightKg(),
                cargo.getRecruitedWeightKg(),
                cargo.getRecruitmentRatePercent(),
                cargo.getDesiredDate(),
                cargo.getRecruitmentDeadline(),
                cargo.getStatus(),
                participants.size(),
                wagonId,
                participants);
    }
}
