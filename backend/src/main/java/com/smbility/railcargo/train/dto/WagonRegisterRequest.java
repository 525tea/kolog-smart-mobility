package com.smbility.railcargo.train.dto;

import com.smbility.railcargo.train.domain.WagonType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record WagonRegisterRequest(

        @NotBlank
        String wagonNumber,

        @NotNull
        WagonType wagonType,

        @NotNull @DecimalMin(value = "0.01")
        BigDecimal maxWeightKg,

        boolean hazardousAllowed
) {
}
