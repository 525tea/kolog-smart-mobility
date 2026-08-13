package com.smbility.railcargo.consolidation.repository;

import com.smbility.railcargo.cargo.domain.TemperatureCondition;
import com.smbility.railcargo.consolidation.domain.ConsolidatedCargo;
import com.smbility.railcargo.consolidation.domain.ConsolidationStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConsolidatedCargoRepository extends JpaRepository<ConsolidatedCargo, Long> {

    List<ConsolidatedCargo> findAllByOriginStationAndDestinationStationAndTemperatureConditionAndHazardousAndStatus(
            String originStation, String destinationStation, TemperatureCondition temperatureCondition,
            boolean hazardous, ConsolidationStatus status);

    List<ConsolidatedCargo> findAllByOriginStationAndDestinationStationAndTemperatureConditionAndHazardousAndDesiredDateAndStatus(
            String originStation, String destinationStation, TemperatureCondition temperatureCondition,
            boolean hazardous, LocalDate desiredDate, ConsolidationStatus status);

    List<ConsolidatedCargo> findAllByStatus(ConsolidationStatus status);

    /** 거래소/잔여용량 조회 화면에서 특정 노선의 모집 중인 공동화물을 둘러볼 때 사용한다. */
    List<ConsolidatedCargo> findAllByOriginStationAndDestinationStationAndStatus(
            String originStation, String destinationStation, ConsolidationStatus status);

    /** 공동화 실패(마감 경과) 처리 대상을 찾을 때 사용한다. */
    List<ConsolidatedCargo> findAllByStatusAndRecruitmentDeadlineBefore(ConsolidationStatus status, LocalDateTime deadline);
}
