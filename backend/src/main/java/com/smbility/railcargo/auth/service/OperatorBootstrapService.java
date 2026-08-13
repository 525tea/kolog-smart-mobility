package com.smbility.railcargo.auth.service;

import com.smbility.railcargo.auth.domain.Member;
import com.smbility.railcargo.auth.domain.MemberRole;
import com.smbility.railcargo.auth.repository.MemberRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** 배포 환경에서만 명시적으로 켜는 최초 운영자 계정 생성기. 공개 회원가입으로는 운영자를 만들 수 없다. */
@Component
@ConditionalOnProperty(prefix = "app.bootstrap.operator", name = "enabled", havingValue = "true")
public class OperatorBootstrapService implements ApplicationRunner {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final String email;
    private final String password;

    public OperatorBootstrapService(MemberRepository memberRepository, PasswordEncoder passwordEncoder,
                                    @Value("${app.bootstrap.operator.email:}") String email,
                                    @Value("${app.bootstrap.operator.password:}") String password) {
        this.memberRepository = memberRepository;
        this.passwordEncoder = passwordEncoder;
        this.email = email;
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (email.isBlank() || password.length() < 12) {
            throw new IllegalStateException("운영자 부트스트랩을 켰다면 OPERATOR_EMAIL과 12자 이상 OPERATOR_PASSWORD가 필요합니다.");
        }
        if (!memberRepository.existsByEmail(email)) {
            memberRepository.save(Member.of(email, passwordEncoder.encode(password), MemberRole.OPERATOR));
        }
    }
}
