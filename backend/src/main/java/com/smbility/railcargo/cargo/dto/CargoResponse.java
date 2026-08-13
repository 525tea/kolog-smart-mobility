package com.smbility.railcargo.cargo.dto;

import com.smbility.railcargo.cargo.domain.CargoOrder;
import com.smbility.railcargo.cargo.domain.CargoOrderStatus;
import com.smbility.railcargo.cargo.domain.TemperatureCondition;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CargoResponse(
        Long id,
        Long shipperId,
        String cargoName,
        String originStation,
        String destinationStation,
        LocalDate desiredDate,
        BigDecimal weightKg,
        BigDecimal volumeCbm,
        TemperatureCondition temperatureCondition,
        boolean hazardous,
        String packagingType,
        String handlingNote,
        CargoOrderStatus status
) {
    public static CargoResponse from(CargoOrder order) {
        return new CargoResponse(
                order.getId(),
                order.getShipper().getId(),
                order.getCargoName(),
                order.getOriginStation(),
                order.getDestinationStation(),
                order.getDesiredDate(),
                order.getWeightKg(),
                order.getVolumeCbm(),
                order.getTemperatureCondition(),
                order.isHazardous(),
                order.getPackagingType(),
                order.getHandlingNote(),
                order.getStatus());
    }
}
