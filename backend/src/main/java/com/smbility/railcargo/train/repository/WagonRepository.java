package com.smbility.railcargo.train.repository;

import com.smbility.railcargo.train.domain.Wagon;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WagonRepository extends JpaRepository<Wagon, Long> {

    List<Wagon> findAllByTrainId(Long trainId);

    List<Wagon> findAllByTrain_OriginStationAndTrain_DestinationStationAndRemainingWeightKgGreaterThanEqual(
            String originStation, String destinationStation, BigDecimal minRemainingWeightKg);

    @Query("""
            select w from Wagon w join fetch w.train t
            where t.originStation = :originStation
              and t.destinationStation = :destinationStation
              and t.status = com.smbility.railcargo.train.domain.TrainStatus.SCHEDULED
              and t.departureAt >= :dayStart
              and t.departureAt < :dayEnd
              and t.reservationDeadline > :now
              and w.remainingWeightKg >= :minRemainingWeightKg
            order by t.departureAt asc
            """)
    List<Wagon> findEligibleForRouteAndDate(
            @Param("originStation") String originStation,
            @Param("destinationStation") String destinationStation,
            @Param("dayStart") LocalDateTime dayStart,
            @Param("dayEnd") LocalDateTime dayEnd,
            @Param("now") LocalDateTime now,
            @Param("minRemainingWeightKg") BigDecimal minRemainingWeightKg);
}
