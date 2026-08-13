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
        if (request.role() != com.smbility.railcargo.auth.domain.MemberRole.SHIPPER) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED,
                    "운영자 계정은 공개 회원가입으로 생성할 수 없습니다.");
        }
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

        return issueTokens(member);
    }

    public TokenResponse refresh(String refreshToken) {
        try {
            var loginMember = jwtTokenProvider.parseRefreshToken(refreshToken);
            Member member = memberRepository.findById(loginMember.memberId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS));
            return issueTokens(member);
        } catch (io.jsonwebtoken.JwtException | IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS, "갱신 토큰이 만료되었거나 올바르지 않습니다.");
        }
    }

    private TokenResponse issueTokens(Member member) {
        return TokenResponse.bearer(
                jwtTokenProvider.createToken(member),
                jwtTokenProvider.createRefreshToken(member),
                jwtProperties.expirationSeconds());
    }

    public MemberResponse getMember(Long memberId) {
        return memberRepository.findById(memberId)
                .map(MemberResponse::from)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND, "회원을 찾을 수 없습니다."));
    }
}
