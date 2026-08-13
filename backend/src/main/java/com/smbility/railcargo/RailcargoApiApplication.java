package com.smbility.railcargo;

import com.smbility.railcargo.auth.jwt.JwtProperties;
import com.smbility.railcargo.cargo.ai.GeminiProperties;
import com.smbility.railcargo.cargo.document.DocumentAiProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

// 자체 JWT 인증(auth.jwt 패키지)만 사용하므로 Spring Boot의 기본 인메모리 사용자 자동 구성은 제외한다.
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@EnableConfigurationProperties({JwtProperties.class, GeminiProperties.class, DocumentAiProperties.class})
@EnableScheduling
public class RailcargoApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(RailcargoApiApplication.class, args);
	}

}
