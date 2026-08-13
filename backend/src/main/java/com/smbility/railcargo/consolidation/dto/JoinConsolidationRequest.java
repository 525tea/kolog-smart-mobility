package com.smbility.railcargo.consolidation.dto;

import com.smbility.railcargo.consolidation.domain.FailurePreference;
import jakarta.validation.constraints.NotNull;

/**
 * @param failurePreference 공동화 실패 시 처리 방식. 생략하면 {@link FailurePreference#AUTO_REFUND}로 처리한다.
 */
public record JoinConsolidationRequest(
        @NotNull Long cargoOrderId,
        FailurePreference failurePreference,
        Boolean firstMile,
        Boolean lastMile
) {
}
