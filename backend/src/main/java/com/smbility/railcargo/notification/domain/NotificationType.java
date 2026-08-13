package com.smbility.railcargo.notification.domain;

/** 알림 종류. 프론트엔드 알림 화면의 분류(analysis/match/payment/approval/reject/info)와 1:1로 대응한다. */
public enum NotificationType {
    ANALYSIS,
    MATCH,
    PAYMENT,
    APPROVAL,
    REJECT,
    INFO
}
