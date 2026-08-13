package com.smbility.railcargo.matching.optimization;

import com.smbility.railcargo.consolidation.service.ConsolidationService;
import com.smbility.railcargo.matching.dto.MatchPredictionResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 화차 부족 등으로 배정을 기다리던(READY_FOR_MATCHING) 공동화물이 있을 때, 운영자가 최적 적재 조합
 * 계산(OR-Tools)을 수동으로 다시 실행할 수 있는 엔드포인트. 평소에는 화주가 참여할 때마다 자동으로
 * 실행되므로({@code ConsolidationService.join}), 이 API는 보조 수단이다.
 */
@Tag(name = "Operator - Load Optimization", description = "최적 적재 조합 재계산")
@RestController
@RequestMapping("/api/v1/operator/load-optimization")
@RequiredArgsConstructor
public class OperatorLoadOptimizationController {

    private final ConsolidationService consolidationService;

    @PostMapping("/run")
    public List<MatchPredictionResponse> run() {
        return consolidationService.runLoadOptimization();
    }
}
