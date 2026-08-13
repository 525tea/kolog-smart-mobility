package com.smbility.railcargo.shipper.dto;

import jakarta.validation.constraints.NotBlank;

public record ShipperRegisterRequest(

        @NotBlank(message = "사업자등록번호는 필수입니다.")
        String businessNumber,

        @NotBlank(message = "상호명은 필수입니다.")
        String companyName,

        @NotBlank(message = "담당자명은 필수입니다.")
        String managerName,

        @NotBlank(message = "연락처는 필수입니다.")
        String phone
) {
}
