package com.smbility.railcargo.auth.service;

import com.smbility.railcargo.auth.domain.Member;
import com.smbility.railcargo.auth.dto.LoginRequest;
import com.smbility.railcargo.auth.dto.MemberResponse;
import com.smbility.railcargo.auth.dto.SignupRequest;
import com.smbility.railcargo.auth.dto.TokenResponse;
import com.smbility.railcargo.auth.jwt.JwtProperties;
import com.smbility.railcargo.auth.jwt.JwtTokenProvider;
import com.smbility.railcargo.auth.repository.MemberRepository;
import com.smbility.railcargo.common.exception.BusinessException;
import com.smbility.railcargo.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;

    @Transactional
    public MemberResponse signup(SignupRequest request) {
        if (memberRepository.existsByEmail(request.email())) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }

        Member member = Member.of(request.email(), passwordEncoder.encode(request.password()), request.role());
        return MemberResponse.from(memberRepository.save(member));
    }

    public TokenResponse login(LoginRequest request) {
        Member member = memberRepository.findByEmail(request.email())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS));

        if (!passwordEncoder.matches(request.password(), member.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        String token = jwtTokenProvider.createToken(member);
        return TokenResponse.bearer(token, jwtProperties.expirationSeconds());
    }
}
