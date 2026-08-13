package com.smbility.railcargo.approval.service;

import com.smbility.railcargo.approval.domain.ApprovalDecision;
import com.smbility.railcargo.approval.domain.ApprovalStatus;
import com.smbility.railcargo.approval.repository.ApprovalDecisionRepository;
import com.smbility.railcargo.auth.domain.Member;
import com.smbility.railcargo.auth.repository.MemberRepository;
import com.smbility.railcargo.cargo.domain.CargoOrder;
import com.smbility.railcargo.common.exception.BusinessException;
import com.smbility.railcargo.common.exception.ErrorCode;
import com.smbility.railcargo.consolidation.domain.CargoParticipation;
import com.smbility.railcargo.consolidation.domain.ConsolidatedCargo;
import com.smbility.railcargo.consolidation.repository.CargoParticipationRepository;
import com.smbility.railcargo.consolidation.service.ConsolidationService;
import com.smbility.railcargo.notification.domain.NotificationType;
import com.smbility.railcargo.notification.service.NotificationService;
import com.smbility.railcargo.report.service.PerformanceReportService;
import com.smbility.railcargo.reservation.domain.Reservation;
import com.smbility.railcargo.reservation.dto.ReservationResponse;
import com.smbility.railcargo.reservation.service.ReservationService;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 코레일 담당자의 공동화물 검토/승인/반려 (기획안 화면 O2). */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ApprovalService {

    private final ApprovalDecisionRepository approvalDecisionRepository;
    private final MemberRepository memberRepository;
    private final ConsolidationService consolidationService;
    private final ReservationService reservationService;
    private final PerformanceReportService performanceReportService;
    private final CargoParticipationRepository cargoParticipationRepository;
    private final NotificationService notificationService;

    @Transactional
    public ReservationResponse approve(Long consolidatedCargoId, Long operatorMemberId, String memo) {
        ConsolidatedCargo group = consolidationService.getEntity(consolidatedCargoId);
        Member operator = getOperator(operatorMemberId);
        Optional<Reservation> existingReservation =
                reservationService.findByConsolidatedCargoId(consolidatedCargoId);

        group.approve();
        approvalDecisionRepository.save(ApprovalDecision.of(group, operator, ApprovalStatus.APPROVED, memo));

        Reservation reservation = existingReservation.orElseGet(() -> reservationService.createFromApproval(group));

        for (CargoParticipation participation : cargoParticipationRepository.findAllByConsolidatedCargoId(group.getId())) {
            CargoOrder order = participation.getCargoOrder();
            order.markReserved();
            notificationService.notify(order.getShipper().getMember().getId(), NotificationType.APPROVAL,
                    "코레일 승인 완료",
                    group.getOriginStation() + " → " + group.getDestinationStation() + " 공동화물 운송이 확정됐어요.");
        }
        group.confirm();

        // 운송 확정과 동시에 성과 리포트를 생성한다 (기획안 "성과 리포트로 마무리한다").
        if (existingReservation.isEmpty()) {
            performanceReportService.generateForReservation(reservation);
        }

        return ReservationResponse.from(reservation);
    }

    @Transactional
    public void reject(Long consolidatedCargoId, Long operatorMemberId, String memo) {
        ConsolidatedCargo group = consolidationService.getEntity(consolidatedCargoId);
        Member operator = getOperator(operatorMemberId);

        if (group.getMatchedWagon() != null) {
            group.getMatchedWagon().release(group.getRecruitedWeightKg());
        }
        // 참여 중이던 화주들을 각자 선택해둔 처리방식(다음 열차 이월/자동환불)에 따라 정리한다
        // (기획안 화면3 "미성립 시 다음 열차 이월 여부 선택" / 보충 "공동화 실패시 대응").
        consolidationService.processFailedGroup(group);
        group.reject();
        approvalDecisionRepository.save(ApprovalDecision.of(group, operator, ApprovalStatus.REJECTED, memo));
    }

    private Member getOperator(Long operatorMemberId) {
        return memberRepository.findById(operatorMemberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND, "운영자 계정을 찾을 수 없습니다."));
    }
}
