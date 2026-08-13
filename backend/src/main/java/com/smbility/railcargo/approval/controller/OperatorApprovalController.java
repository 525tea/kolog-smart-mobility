package com.smbility.railcargo.approval.controller;

import com.smbility.railcargo.approval.dto.ApprovalRequest;
import com.smbility.railcargo.approval.service.ApprovalService;
import com.smbility.railcargo.auth.jwt.LoginMember;
import com.smbility.railcargo.consolidation.dto.ConsolidationDetailResponse;
import com.smbility.railcargo.consolidation.service.ConsolidationService;
import com.smbility.railcargo.reservation.dto.ReservationResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 코레일 운영자용 공동화물 검토/승인/반려 화면 (ROLE_OPERATOR 전용). */
@Tag(name = "Operator - Approval", description = "공동화물 검토/승인/반려")
@RestController
@RequestMapping("/api/v1/operator/consolidated-cargos")
@RequiredArgsConstructor
public class OperatorApprovalController {

    private final ApprovalService approvalService;
    private final ConsolidationService consolidationService;

    @GetMapping("/{consolidatedCargoId}")
    public ConsolidationDetailResponse getDetail(@PathVariable Long consolidatedCargoId) {
        return consolidationService.getDetail(consolidatedCargoId);
    }

    @PostMapping("/{consolidatedCargoId}/approve")
    public ResponseEntity<ReservationResponse> approve(
            @PathVariable Long consolidatedCargoId,
            @AuthenticationPrincipal LoginMember loginMember,
            @RequestBody(required = false) ApprovalRequest request
    ) {
        String memo = request != null ? request.memo() : null;
        return ResponseEntity.ok(approvalService.approve(consolidatedCargoId, loginMember.memberId(), memo));
    }

    @PostMapping("/{consolidatedCargoId}/reject")
    public ResponseEntity<Void> reject(
            @PathVariable Long consolidatedCargoId,
            @AuthenticationPrincipal LoginMember loginMember,
            @RequestBody(required = false) ApprovalRequest request
    ) {
        String memo = request != null ? request.memo() : null;
        approvalService.reject(consolidatedCargoId, loginMember.memberId(), memo);
        return ResponseEntity.ok().build();
    }
}
