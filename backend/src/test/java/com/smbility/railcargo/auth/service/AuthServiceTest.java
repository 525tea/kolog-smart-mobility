package com.smbility.railcargo.auth.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.smbility.railcargo.auth.domain.MemberRole;
import com.smbility.railcargo.auth.dto.SignupRequest;
import com.smbility.railcargo.auth.jwt.JwtProperties;
import com.smbility.railcargo.auth.jwt.JwtTokenProvider;
import com.smbility.railcargo.auth.repository.MemberRepository;
import com.smbility.railcargo.common.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock MemberRepository memberRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtTokenProvider jwtTokenProvider;
    @Mock JwtProperties jwtProperties;
    @InjectMocks AuthService authService;

    @Test
    void 공개_회원가입으로_운영자_계정을_만들_수_없다() {
        SignupRequest request = new SignupRequest("operator@example.com", "password-123", MemberRole.OPERATOR);

        assertThatThrownBy(() -> authService.signup(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("운영자 계정");
        verify(memberRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }
}
