package com.smbility.railcargo.auth.controller;

import com.smbility.railcargo.auth.dto.LoginRequest;
import com.smbility.railcargo.auth.dto.MemberResponse;
import com.smbility.railcargo.auth.dto.SignupRequest;
import com.smbility.railcargo.auth.dto.TokenResponse;
import com.smbility.railcargo.auth.dto.RefreshTokenRequest;
import com.smbility.railcargo.auth.service.AuthService;
import com.smbility.railcargo.auth.jwt.LoginMember;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@Tag(name = "Auth", description = "회원가입 / 로그인")
@RestController
@RequestMapping({"/api/v1/auth", "/auth"})
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    public ResponseEntity<MemberResponse> signup(@Valid @RequestBody SignupRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.signup(request));
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refresh(request.refreshToken()));
    }

    @GetMapping("/me")
    public ResponseEntity<MemberResponse> me(@AuthenticationPrincipal LoginMember loginMember) {
        return ResponseEntity.ok(authService.getMember(loginMember.memberId()));
    }
}
