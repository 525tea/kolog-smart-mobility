package com.smbility.railcargo.train.dto;

import com.smbility.railcargo.train.domain.Wagon;
import com.smbility.railcargo.train.domain.WagonType;
import java.math.BigDecimal;

public record WagonResponse(
        Long id,
        Long trainId,
        String wagonNumber,
        WagonType wagonType,
        BigDecimal maxWeightKg,
        BigDecimal remainingWeightKg,
        BigDecimal loadFactorPercent,
        boolean hazardousAllowed
) {
    public static WagonResponse from(Wagon wagon) {
        return new WagonResponse(
                wagon.getId(),
                wagon.getTrain().getId(),
                wagon.getWagonNumber(),
                wagon.getWagonType(),
                wagon.getMaxWeightKg(),
                wagon.getRemainingWeightKg(),
                wagon.getLoadFactorPercent(),
                wagon.isHazardousAllowed());
    }
}
