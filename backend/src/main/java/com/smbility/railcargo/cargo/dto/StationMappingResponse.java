package com.smbility.railcargo.cargo.dto;

public record StationMappingResponse(
        String inputLocation,
        String railStation,
        boolean mapped,
        String mappedStationId,
        boolean outOfCoverage,
        String userMessage
) {
}
