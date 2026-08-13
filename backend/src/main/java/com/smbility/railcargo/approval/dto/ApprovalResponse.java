package com.smbility.railcargo.approval.dto;

import com.smbility.railcargo.approval.domain.ApprovalDecision;
import com.smbility.railcargo.approval.domain.ApprovalStatus;

public record ApprovalResponse(
        Long id,
        Long consolidatedCargoId,
        Long operatorId,
        ApprovalStatus status,
        String memo
) {
    public static ApprovalResponse from(ApprovalDecision decision) {
        return new ApprovalResponse(
                decision.getId(),
                decision.getConsolidatedCargo().getId(),
                decision.getOperator().getId(),
                decision.getStatus(),
                decision.getMemo());
    }
}
