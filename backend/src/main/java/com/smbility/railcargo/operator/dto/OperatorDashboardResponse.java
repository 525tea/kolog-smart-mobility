package com.smbility.railcargo.operator.dto;

import com.smbility.railcargo.consolidation.dto.ConsolidationDetailResponse;
import com.smbility.railcargo.train.dto.TrainResponse;
import java.util.List;

/** 코레일 운영 대시보드 화면(O1) 응답. */
public record OperatorDashboardResponse(
        List<TrainResponse> upcomingTrains,
        List<ConsolidationDetailResponse> reviewQueue
) {
}
