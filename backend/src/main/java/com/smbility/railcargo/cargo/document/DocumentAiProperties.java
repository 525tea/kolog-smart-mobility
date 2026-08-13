package com.smbility.railcargo.cargo.document;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.ai.document-ai")
public record DocumentAiProperties(
        boolean enabled,
        String projectId,
        String location,
        String processorId
) {
}
