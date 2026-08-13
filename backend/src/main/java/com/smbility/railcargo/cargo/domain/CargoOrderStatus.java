package com.smbility.railcargo.cargo.domain;

public enum CargoOrderStatus {
    /** 화주가 화물 정보를 등록한 직후 */
    REGISTERED,
    /** AI 운송조건 추출이 완료된 상태 */
    ANALYZED,
    /** 공동화물에 참여 신청한 상태 */
    PARTICIPATING,
    /** 코레일 승인 및 예약이 확정된 상태 */
    RESERVED,
    /** 취소된 상태 */
    CANCELLED
}
