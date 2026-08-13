package com.smbility.railcargo.reservation.controller;

import com.smbility.railcargo.auth.jwt.LoginMember;
import com.smbility.railcargo.reservation.dto.ReservationResponse;
import com.smbility.railcargo.reservation.dto.ParticipationPaymentResponse;
import com.smbility.railcargo.reservation.service.ReservationService;
import com.smbility.railcargo.tracking.dto.TrackingResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Reservation", description = "예약 조회 (화주 내 운송 목록)")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;

    @GetMapping("/reservations/{reservationId}")
    public ReservationResponse getReservation(
            @AuthenticationPrincipal LoginMember loginMember,
            @PathVariable Long reservationId
    ) {
        return reservationService.getById(loginMember.memberId(), reservationId);
    }

    @GetMapping("/shippers/me/reservations")
    public List<ReservationResponse> getMyReservations(@AuthenticationPrincipal LoginMember loginMember) {
        return reservationService.getMyReservations(loginMember.memberId());
    }

    @GetMapping("/shippers/me/payments")
    public List<ParticipationPaymentResponse> getMyPayments(@AuthenticationPrincipal LoginMember loginMember) {
        return reservationService.getMyPayments(loginMember.memberId());
    }

    @GetMapping("/reservations/{reservationId}/tracking")
    public TrackingResponse getTracking(
            @AuthenticationPrincipal LoginMember loginMember,
            @PathVariable Long reservationId
    ) {
        return reservationService.getTracking(loginMember.memberId(), reservationId);
    }
}
