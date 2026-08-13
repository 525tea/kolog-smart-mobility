package com.smbility.railcargo.approval.dto;

import jakarta.validation.constraints.Size;

public record ApprovalRequest(
        @Size(max = 500)
        String memo
) {
}
