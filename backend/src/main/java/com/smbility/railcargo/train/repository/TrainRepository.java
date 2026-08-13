package com.smbility.railcargo.train.repository;

import com.smbility.railcargo.train.domain.Train;
import com.smbility.railcargo.train.domain.TrainStatus;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TrainRepository extends JpaRepository<Train, Long> {

    List<Train> findAllByStatusAndDepartureAtAfterOrderByDepartureAtAsc(TrainStatus status, LocalDateTime after);

    List<Train> findAllByOriginStationAndDestinationStationAndStatus(
            String originStation, String destinationStation, TrainStatus status);
}
