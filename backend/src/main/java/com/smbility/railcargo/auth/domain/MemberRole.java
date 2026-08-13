package com.smbility.railcargo.auth.domain;

public enum MemberRole {
    /** 화주: 화물을 등록하고 공동화물에 참여하는 판매자/화주 */
    SHIPPER,
    /** 코레일 운영자: 잔여용량 관리 및 공동화물 승인 */
    OPERATOR
}
