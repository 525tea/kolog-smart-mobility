package com.smbility.railcargo.operator.controller;

import com.smbility.railcargo.consolidation.service.ConsolidationService;
import com.smbility.railcargo.operator.dto.OperatorDashboardResponse;
import com.smbility.railcargo.train.service.TrainService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 코레일 운영 대시보드 (기획안 화면 O1). ROLE_OPERATOR 전용, SecurityConfig에서 제한. */
@Tag(name = "Operator - Dashboard", description = "운영 대시보드")
@RestController
@RequestMapping("/api/v1/operator/dashboard")
@RequiredArgsConstructor
public class OperatorDashboardController {

    private final TrainService trainService;
    private final ConsolidationService consolidationService;

    @GetMapping
    public OperatorDashboardResponse getDashboard() {
        return new OperatorDashboardResponse(
                trainService.getUpcomingTrains(),
                consolidationService.getReviewQueue());
    }
}
