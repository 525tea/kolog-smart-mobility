package com.smbility.railcargo.auth.jwt;

import com.smbility.railcargo.auth.domain.MemberRole;

/**
 * 인증된 요청의 Spring Security principal로 사용되는 값 객체.
 * 컨트롤러에서 {@code @AuthenticationPrincipal LoginMember loginMember} 로 주입받는다.
 */
public record LoginMember(Long memberId, MemberRole role) {
}
