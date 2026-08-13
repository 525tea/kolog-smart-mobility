package com.smbility.railcargo.cargo.dto;

import com.smbility.railcargo.cargo.domain.CargoOrder;
import com.smbility.railcargo.cargo.domain.CargoOrderStatus;
import com.smbility.railcargo.cargo.domain.HazardGrade;
import com.smbility.railcargo.cargo.domain.ServiceMode;
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
        ServiceMode serviceMode,
        BigDecimal weightKg,
        BigDecimal volumeCbm,
        TemperatureCondition temperatureCondition,
        boolean hazardous,
        HazardGrade hazardGrade,
        String hazardClassCode,
        String hazardClassName,
        boolean transportRejected,
        boolean requiresMsds,
        boolean msdsAttached,
        String msdsFileName,
        String hazardReason,
        BigDecimal surchargeRate,
        BigDecimal fixedPowerFeeKrw,
        BigDecimal detectedTemperatureC,
        String assignedContainer,
        String packagingType,
        String handlingNote,
        BigDecimal declaredValueKrw,
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
                order.getServiceMode(),
                order.getWeightKg(),
                order.getVolumeCbm(),
                order.getTemperatureCondition(),
                order.isHazardous(),
                order.getHazardGrade(),
                order.getHazardClassCode(),
                order.getHazardClassName(),
                order.isTransportRejected(),
                order.isRequiresMsds(),
                order.isMsdsAttached(),
                order.getMsdsFileName(),
                hazardReason(order),
                order.getSurchargeRate(),
                order.getFixedPowerFeeKrw(),
                order.getDetectedTemperatureC(),
                assignedContainer(order.getTemperatureCondition()),
                order.getPackagingType(),
                order.getHandlingNote(),
                order.getDeclaredValueKrw(),
                order.getStatus());
    }

    private static String assignedContainer(TemperatureCondition condition) {
        if (condition == null) return null;
        return switch (condition) {
            case FROZEN -> "초저온 Reefer";
            case REFRIGERATED -> "신선 Reefer";
            case CONSTANT -> "스마트 항온항습";
            case ROOM -> null;
        };
    }

    private static String hazardReason(CargoOrder order) {
        if (!order.isHazardous()) return null;
        if (order.isTransportRejected()) return "제1급 폭발물 가능성이 감지되어 철도 운송 접수가 차단되었습니다.";
        String classification = java.util.stream.Stream.of(order.getHazardClassCode(), order.getHazardClassName())
                .filter(value -> value != null && !value.isBlank())
                .collect(java.util.stream.Collectors.joining(" "));
        if (classification.isBlank()) {
            return "위험 가능성이 감지되었지만 등급은 확정하지 않았습니다. MSDS의 운송 정보를 확인해주세요.";
        }
        return classification + " 가능성이 감지되어 20% 안전 할증과 MSDS 확인이 적용됩니다.";
    }
}
