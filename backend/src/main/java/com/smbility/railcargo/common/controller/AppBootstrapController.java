package com.smbility.railcargo.common.controller;

import com.smbility.railcargo.common.dto.AppBootstrapResponse;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/v1/app", "/app"})
public class AppBootstrapController {
    @Value("${app.client.min-supported-version}") private String minSupportedVersion;
    @Value("${app.client.latest-version}") private String latestVersion;
    @Value("${app.client.force-update}") private boolean forceUpdate;
    @Value("${app.client.maintenance-active}") private boolean maintenanceActive;
    @Value("${app.client.maintenance-message:}") private String maintenanceMessage;
    @Value("${app.client.container-capacity-cbm}") private BigDecimal containerCapacityCbm;
    @Value("${app.client.hold-minutes}") private int holdMinutes;
    @Value("${app.client.classify-confidence-threshold}") private BigDecimal classifyConfidenceThreshold;
    @Value("${app.client.rule-db-version}") private String ruleDbVersion;

    @GetMapping("/bootstrap")
    public ResponseEntity<AppBootstrapResponse> bootstrap(
            @RequestParam(defaultValue = "web") String platform,
            @RequestParam(defaultValue = "1.4.0") String appVersion) {
        Map<String, Object> config = new LinkedHashMap<>();
        config.put("containerCapacityCbm", containerCapacityCbm);
        config.put("holdMinutes", holdMinutes);
        config.put("classifyConfidenceThreshold", classifyConfidenceThreshold);
        config.put("ruleDbVersion", ruleDbVersion);
        config.put("platform", platform);
        config.put("requestedAppVersion", appVersion);
        return ResponseEntity.ok(new AppBootstrapResponse(
                minSupportedVersion, latestVersion, forceUpdate, null,
                new AppBootstrapResponse.Maintenance(maintenanceActive,
                        maintenanceMessage == null || maintenanceMessage.isBlank() ? null : maintenanceMessage, null),
                config));
    }
}
