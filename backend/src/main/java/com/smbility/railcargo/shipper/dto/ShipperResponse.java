package com.smbility.railcargo.shipper.dto;

import com.smbility.railcargo.shipper.domain.Shipper;

public record ShipperResponse(
        Long id,
        Long memberId,
        String businessNumber,
        String companyName,
        String managerName,
        String phone
) {
    public static ShipperResponse from(Shipper shipper) {
        return new ShipperResponse(
                shipper.getId(),
                shipper.getMember().getId(),
                shipper.getBusinessNumber(),
                shipper.getCompanyName(),
                shipper.getManagerName(),
                shipper.getPhone());
    }
}
