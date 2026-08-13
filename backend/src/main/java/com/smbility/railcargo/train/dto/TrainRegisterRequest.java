package com.smbility.railcargo.train.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record TrainRegisterRequest(

        @NotBlank
        String trainNumber,

        @NotBlank
        String originStation,

        @NotBlank
        String destinationStation,

        @NotNull @Future
        LocalDateTime departureAt,

        @NotNull @Future
        LocalDateTime arrivalAt,

        @NotNull @Future
        LocalDateTime reservationDeadline
) {
}
