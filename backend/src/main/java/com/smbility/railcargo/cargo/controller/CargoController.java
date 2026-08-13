package com.smbility.railcargo.cargo.controller;

import com.smbility.railcargo.auth.jwt.LoginMember;
import com.smbility.railcargo.cargo.dto.CargoCorrectionRequest;
import com.smbility.railcargo.cargo.dto.CargoRegisterRequest;
import com.smbility.railcargo.cargo.dto.CargoResponse;
import com.smbility.railcargo.cargo.service.CargoService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Cargo", description = "화주 화물 등록 및 AI 운송조건 분석")
@RestController
@RequestMapping("/api/v1/cargo-orders")
@RequiredArgsConstructor
public class CargoController {

    private final CargoService cargoService;

    @PostMapping
    public ResponseEntity<CargoResponse> register(
            @AuthenticationPrincipal LoginMember loginMember,
            @Valid @RequestBody CargoRegisterRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cargoService.register(loginMember.memberId(), request));
    }

    @PostMapping("/{cargoOrderId}/ai-analysis")
    public ResponseEntity<CargoResponse> runAiAnalysis(@PathVariable Long cargoOrderId) {
        return ResponseEntity.ok(cargoService.runAiAnalysis(cargoOrderId));
    }

    @PatchMapping("/{cargoOrderId}")
    public ResponseEntity<CargoResponse> correct(
            @PathVariable Long cargoOrderId,
            @RequestBody CargoCorrectionRequest request
    ) {
        return ResponseEntity.ok(cargoService.correct(cargoOrderId, request));
    }

    @GetMapping("/{cargoOrderId}")
    public ResponseEntity<CargoResponse> getCargo(@PathVariable Long cargoOrderId) {
        return ResponseEntity.ok(cargoService.getCargo(cargoOrderId));
    }

    @GetMapping("/me")
    public ResponseEntity<List<CargoResponse>> getMyCargoOrders(@AuthenticationPrincipal LoginMember loginMember) {
        return ResponseEntity.ok(cargoService.getMyCargoOrders(loginMember.memberId()));
    }
}
