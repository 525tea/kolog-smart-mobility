package com.smbility.railcargo.reservation.dto;

import com.smbility.railcargo.reservation.domain.PaymentStatus;
import com.smbility.railcargo.reservation.domain.Reservation;
import java.math.BigDecimal;

public record ReservationResponse(
        Long id,
        Long consolidatedCargoId,
        Long wagonId,
        BigDecimal totalCost,
        PaymentStatus paymentStatus
) {
    public static ReservationResponse from(Reservation reservation) {
        return new ReservationResponse(
                reservation.getId(),
                reservation.getConsolidatedCargo().getId(),
                reservation.getWagon().getId(),
                reservation.getTotalCost(),
                reservation.getPaymentStatus());
    }
}
