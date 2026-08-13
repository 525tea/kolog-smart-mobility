package com.smbility.railcargo.consolidation.controller;

import com.smbility.railcargo.auth.jwt.LoginMember;
import com.smbility.railcargo.consolidation.dto.ConsolidationCandidateResponse;
import com.smbility.railcargo.consolidation.dto.ConsolidationDetailResponse;
import com.smbility.railcargo.consolidation.dto.JoinConsolidationRequest;
import com.smbility.railcargo.consolidation.service.ConsolidationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Consolidation", description = "공동화물 추천/참여/모집현황")
@RestController
@RequiredArgsConstructor
public class ConsolidationController {

    private final ConsolidationService consolidationService;

    @GetMapping("/api/v1/cargo-orders/{cargoOrderId}/consolidation-candidates")
    public List<ConsolidationCandidateResponse> getCandidates(
            @AuthenticationPrincipal LoginMember loginMember,
            @PathVariable Long cargoOrderId
    ) {
        return consolidationService.getCandidates(loginMember.memberId(), cargoOrderId);
    }

    @PostMapping("/api/v1/consolidated-cargos/{consolidatedCargoId}/participations")
    public ResponseEntity<ConsolidationDetailResponse> join(
            @AuthenticationPrincipal LoginMember loginMember,
            @PathVariable Long consolidatedCargoId,
            @Valid @RequestBody JoinConsolidationRequest request
    ) {
        return ResponseEntity.ok(consolidationService.join(loginMember.memberId(), consolidatedCargoId, request.cargoOrderId(),
                request.failurePreference(), Boolean.TRUE.equals(request.firstMile()), Boolean.TRUE.equals(request.lastMile())));
    }

    @GetMapping("/api/v1/consolidated-cargos/{consolidatedCargoId}")
    public ConsolidationDetailResponse getDetail(@PathVariable Long consolidatedCargoId) {
        return consolidationService.getDetail(consolidatedCargoId);
    }

    /** 화면 "잔여용량 조회 / 거래소": 노선을 지정하면 그 노선만, 생략하면 모집 중인 공동화물 전체를 보여준다. */
    @GetMapping("/api/v1/consolidated-cargos")
    public List<ConsolidationDetailResponse> browse(
            @RequestParam(required = false) String originStation,
            @RequestParam(required = false) String destinationStation
    ) {
        return consolidationService.browseRecruitingGroups(originStation, destinationStation);
    }

    @GetMapping("/api/v1/consolidated-cargos/me")
    public List<ConsolidationDetailResponse> getMyGroups(@AuthenticationPrincipal LoginMember loginMember) {
        return consolidationService.getMyGroups(loginMember.memberId());
    }
}
