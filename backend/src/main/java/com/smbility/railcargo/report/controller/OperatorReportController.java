package com.smbility.railcargo.report.controller;

import com.smbility.railcargo.report.dto.PerformanceReportResponse;
import com.smbility.railcargo.report.service.PerformanceReportService;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 운송 성과 리포트 조회 (기획안 화면 O3). 승인 시점에 자동 생성된다. */
@Tag(name = "Operator - Report", description = "운송 성과 리포트")
@RestController
@RequestMapping("/api/v1/operator/trains/{trainId}/report")
@RequiredArgsConstructor
public class OperatorReportController {

    private final PerformanceReportService performanceReportService;

    @GetMapping
    public List<PerformanceReportResponse> getReports(@PathVariable Long trainId) {
        return performanceReportService.getReportsForTrain(trainId);
    }
}
