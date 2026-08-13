package com.smbility.railcargo.consolidation.domain;

/**
 * 공동화가 성립하지 못했을 때(마감까지 목표중량 미달, 또는 코레일 반려) 화주가 미리 선택해두는 처리 방식.
 * 기획안 보충 "공동화 실패시 대응/보상"에 대응한다.
 *
 * <p>기획안에는 "인접 거점 상품으로 재매칭", "제휴 육상운송으로 전환", "추가요금 보장운송" 옵션도 제시되어 있지만,
 * 인접 거점 마스터 데이터·육상운송 제휴사 연동이 아직 없어 이번 단계에서는 구현하지 않는다
 * (docs/planning/functional-spec.md 참고).
 */
public enum FailurePreference {
    /** 같은 노선의 다음 공동화물 모집 그룹으로 자동 이월한다. */
    NEXT_TRAIN,
    /** 참여를 취소하고 결제 전 상태로 되돌린다(가상 결제 단계라 실제 환불 처리는 없음). */
    AUTO_REFUND
}
