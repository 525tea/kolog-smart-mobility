package com.smbility.railcargo.common.dto;

import java.util.Map;

public record AppBootstrapResponse(
        String minSupportedVersion,
        String latestVersion,
        boolean forceUpdate,
        String storeUrl,
        Maintenance maintenance,
        Map<String, Object> config
) {
    public record Maintenance(boolean active, String message, String until) { }
}
