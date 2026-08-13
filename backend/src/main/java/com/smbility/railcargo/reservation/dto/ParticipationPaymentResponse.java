package com.smbility.railcargo.reservation.dto;

import com.smbility.railcargo.consolidation.domain.CargoParticipation;
import com.smbility.railcargo.reservation.domain.PaymentStatus;
import java.math.BigDecimal;

/** 승인 전 공동화 참여 단계에서 이미 완료된 가상 결제 내역. */
public record ParticipationPaymentResponse(
        Long id,
        Long cargoOrderId,
        Long consolidatedCargoId,
        BigDecimal totalCost,
        PaymentStatus paymentStatus
) {
    public static ParticipationPaymentResponse from(CargoParticipation participation) {
        return new ParticipationPaymentResponse(
                participation.getId(),
                participation.getCargoOrder().getId(),
                participation.getConsolidatedCargo().getId(),
                participation.getAllocatedCost(),
                PaymentStatus.VIRTUAL_PAID);
    }
}
