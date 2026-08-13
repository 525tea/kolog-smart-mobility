package com.smbility.railcargo.consolidation.domain;

public enum ConsolidationStatus {
    /** 모집 중 (목표중량 미달) */
    RECRUITING,
    /** 목표중량 달성, 최적 적재 조합(화차 배정) 계산 대기 중 */
    READY_FOR_MATCHING,
    /** 화차 배정 및 성립확률/수익성 산정 완료 */
    MATCHED,
    /** 코레일 담당자 검토 대기 */
    PENDING_APPROVAL,
    /** 승인됨 (예약 생성됨) */
    APPROVED,
    /** 반려됨 */
    REJECTED,
    /** 운송 확정 */
    CONFIRMED,
    /** 모집 실패/취소 */
    CANCELLED
}
