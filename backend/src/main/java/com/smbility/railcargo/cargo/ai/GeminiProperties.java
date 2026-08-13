package com.smbility.railcargo.cargo.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Google Gemini API 연동 설정. 무료 등급(Google AI Studio) API 키를 사용한다.
 * https://aistudio.google.com/apikey 에서 발급받아 {@code GEMINI_API_KEY} 환경변수로 주입한다.
 */
@ConfigurationProperties(prefix = "app.ai.gemini")
public record GeminiProperties(
        String apiKey,
        String model,
        String endpoint
) {
    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }
}
