package com.smbility.railcargo.tracking.domain;

/** 예약된 운송 건의 진행 단계. Train의 departureAt/arrivalAt과 현재 시각을 비교해 판단한다. */
public enum TransportPhase {
    /** 아직 출발 전. 화차는 출발역에 대기 중이라고 가정한다. */
    BEFORE_DEPARTURE,
    /** 출발~도착 사이. 경과 시간 비율만큼 출발역-도착역 사이를 이동 중이라고 시뮬레이션한다. */
    IN_TRANSIT,
    /** 도착 예정 시각이 지났음. */
    ARRIVED
}
