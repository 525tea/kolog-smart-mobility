package com.smbility.railcargo.reservation.domain;

public enum PaymentStatus {
    PENDING,
    /** 실제 PG 연동 없이 가상 결제가 완료된 상태 (기획안 3번 화면 참고). */
    VIRTUAL_PAID
}
