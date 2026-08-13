package com.smbility.railcargo.reservation.service;

import com.smbility.railcargo.cargo.domain.CargoOrder;
import com.smbility.railcargo.cargo.domain.CargoOrderStatus;
import com.smbility.railcargo.cargo.repository.CargoOrderRepository;
import com.smbility.railcargo.common.exception.BusinessException;
import com.smbility.railcargo.common.exception.ErrorCode;
import com.smbility.railcargo.consolidation.domain.CargoParticipation;
import com.smbility.railcargo.consolidation.domain.ConsolidatedCargo;
import com.smbility.railcargo.consolidation.repository.CargoParticipationRepository;
import com.smbility.railcargo.matching.domain.MatchPrediction;
import com.smbility.railcargo.matching.repository.MatchPredictionRepository;
import com.smbility.railcargo.reservation.domain.Reservation;
import com.smbility.railcargo.reservation.dto.ReservationResponse;
import com.smbility.railcargo.reservation.dto.ParticipationPaymentResponse;
import com.smbility.railcargo.reservation.repository.ReservationRepository;
import com.smbility.railcargo.shipper.domain.Shipper;
import com.smbility.railcargo.shipper.service.ShipperService;
import com.smbility.railcargo.tracking.dto.TrackingResponse;
import com.smbility.railcargo.tracking.service.TrackingSimulationService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final MatchPredictionRepository matchPredictionRepository;
    private final CargoOrderRepository cargoOrderRepository;
    private final CargoParticipationRepository cargoParticipationRepository;
    private final ShipperService shipperService;
    private final TrackingSimulationService trackingSimulationService;

    /** 승인(ApprovalService)에서 호출한다. 이어서 성과 리포트 생성을 위해 엔티티를 그대로 반환한다. */
    @Transactional
    public Reservation createFromApproval(ConsolidatedCargo group) {
        MatchPrediction prediction = matchPredictionRepository
                .findTopByConsolidatedCargoIdOrderByIdDesc(group.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND, "매칭 예측 결과를 찾을 수 없습니다."));

        Reservation reservation = Reservation.confirm(group, group.getMatchedWagon(), prediction.getExpectedRevenue());
        return reservationRepository.save(reservation);
    }

    public Optional<Reservation> findByConsolidatedCargoId(Long consolidatedCargoId) {
        return reservationRepository.findByConsolidatedCargoId(consolidatedCargoId);
    }

    public ReservationResponse getById(Long memberId, Long reservationId) {
        return ReservationResponse.from(getOwnedEntity(memberId, reservationId));
    }

    public Reservation getEntity(Long reservationId) {
        return reservationRepository.findById(reservationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND, "예약을 찾을 수 없습니다."));
    }

    /** 운송현황 화면의 "실시간 위치추적". 실제 GPS 연동이 없어 열차 시간표 기준으로 시뮬레이션한다. */
    public TrackingResponse getTracking(Long memberId, Long reservationId) {
        return trackingSimulationService.simulate(getOwnedEntity(memberId, reservationId));
    }

    /** 화주 본인의 확정된 예약 목록 (내 운송 목록 화면). */
    public List<ReservationResponse> getMyReservations(Long memberId) {
        Shipper shipper = shipperService.getEntityByMemberId(memberId);
        List<CargoOrder> reservedOrders = cargoOrderRepository
                .findAllByShipperIdAndStatus(shipper.getId(), CargoOrderStatus.RESERVED);

        Map<Long, ReservationResponse> reservationsById = new LinkedHashMap<>();
        for (CargoOrder order : reservedOrders) {
            List<CargoParticipation> participations = cargoParticipationRepository.findAllByCargoOrderId(order.getId());
            for (CargoParticipation participation : participations) {
                reservationRepository.findByConsolidatedCargoId(participation.getConsolidatedCargo().getId())
                        .ifPresent(reservation ->
                                reservationsById.putIfAbsent(reservation.getId(), ReservationResponse.from(reservation)));
            }
        }
        return List.copyOf(reservationsById.values());
    }

    /** 공동화 참여 버튼에서 처리된 가상 결제 내역. 코레일 승인 전에도 마이페이지에 표시한다. */
    public List<ParticipationPaymentResponse> getMyPayments(Long memberId) {
        return cargoParticipationRepository.findAllByCargoOrderShipperMemberIdOrderByIdDesc(memberId).stream()
                .filter(participation -> !participation.isSuperseded())
                .map(ParticipationPaymentResponse::from)
                .toList();
    }

    private Reservation getOwnedEntity(Long memberId, Long reservationId) {
        Reservation reservation = getEntity(reservationId);
        boolean participant = cargoParticipationRepository
                .findAllByConsolidatedCargoId(reservation.getConsolidatedCargo().getId()).stream()
                .filter(participation -> !participation.isSuperseded())
                .anyMatch(participation -> participation.getCargoOrder().getShipper().getMember().getId().equals(memberId));
        if (!participant) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED, "본인이 참여한 예약만 조회할 수 있습니다.");
        }
        return reservation;
    }
}
