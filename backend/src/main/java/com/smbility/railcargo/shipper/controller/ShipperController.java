package com.smbility.railcargo.shipper.controller;

import com.smbility.railcargo.auth.jwt.LoginMember;
import com.smbility.railcargo.shipper.dto.ShipperRegisterRequest;
import com.smbility.railcargo.shipper.dto.ShipperResponse;
import com.smbility.railcargo.shipper.service.ShipperService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Shipper", description = "화주 사업자 등록/조회")
@RestController
@RequestMapping("/api/v1/shippers")
@RequiredArgsConstructor
public class ShipperController {

    private final ShipperService shipperService;

    @PostMapping
    public ResponseEntity<ShipperResponse> register(
            @AuthenticationPrincipal LoginMember loginMember,
            @Valid @RequestBody ShipperRegisterRequest request
    ) {
        ShipperResponse response = shipperService.register(loginMember.memberId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/me")
    public ResponseEntity<ShipperResponse> getMyProfile(@AuthenticationPrincipal LoginMember loginMember) {
        return ResponseEntity.ok(shipperService.getMyProfile(loginMember.memberId()));
    }
}
