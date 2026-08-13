package com.smbility.railcargo.auth.dto;

import com.smbility.railcargo.auth.domain.Member;
import com.smbility.railcargo.auth.domain.MemberRole;

public record MemberResponse(
        Long id,
        String email,
        MemberRole role
) {
    public static MemberResponse from(Member member) {
        return new MemberResponse(member.getId(), member.getEmail(), member.getRole());
    }
}
