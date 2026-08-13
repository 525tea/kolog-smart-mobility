package com.smbility.railcargo.auth.dto;

import com.smbility.railcargo.auth.domain.MemberRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SignupRequest(

        @NotBlank @Email
        String email,

        @NotBlank @Size(min = 8, max = 100, message = "비밀번호는 8자 이상이어야 합니다.")
        String password,

        @NotNull
        MemberRole role
) {
}
