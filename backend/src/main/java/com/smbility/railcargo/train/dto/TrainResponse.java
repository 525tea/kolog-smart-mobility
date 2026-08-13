package com.smbility.railcargo.train.dto;

import com.smbility.railcargo.train.domain.Train;
import com.smbility.railcargo.train.domain.TrainStatus;
import java.time.LocalDateTime;
import java.util.List;

public record TrainResponse(
        Long id,
        String trainNumber,
        String originStation,
        String destinationStation,
        LocalDateTime departureAt,
        LocalDateTime arrivalAt,
        LocalDateTime reservationDeadline,
        TrainStatus status,
        List<WagonResponse> wagons
) {
    public static TrainResponse from(Train train, List<WagonResponse> wagons) {
        return new TrainResponse(
                train.getId(),
                train.getTrainNumber(),
                train.getOriginStation(),
                train.getDestinationStation(),
                train.getDepartureAt(),
                train.getArrivalAt(),
                train.getReservationDeadline(),
                train.getStatus(),
                wagons);
    }
}
