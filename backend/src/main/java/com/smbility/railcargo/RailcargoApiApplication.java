package com.smbility.railcargo;

import com.smbility.railcargo.auth.jwt.JwtProperties;
import com.smbility.railcargo.cargo.ai.GeminiProperties;
import com.smbility.railcargo.cargo.document.DocumentAiProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@EnableConfigurationProperties({JwtProperties.class, GeminiProperties.class, DocumentAiProperties.class})
public class RailcargoApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(RailcargoApiApplication.class, args);
    }
}
