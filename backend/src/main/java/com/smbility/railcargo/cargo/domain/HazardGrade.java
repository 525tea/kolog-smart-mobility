package com.smbility.railcargo.cargo.domain;

/**
 * 위험물 등급. {@code hazardous=true}인 화물에만 의미가 있다.
 * 국토교통부 위험물철도운송규칙의 유엔번호(UN No.)/위험등급 체계를 그대로 가져오지 않고,
 * 화면 설계에 맞춰 A(최고위험)~D(최저위험) 4단계로 단순화한 자체 분류다.
 * 실제 규정과 매핑이 필요해지면 이 enum과 {@link com.smbility.railcargo.pricing.PricingPolicy}의
 * 할증률을 함께 교체한다.
 */
public enum HazardGrade {
    /** 폭발성, 고압가스 등 최고위험군. */
    A,
    /** 인화성 액체/고체 등 고위험군. */
    B,
    /** 부식성 물질 등 중위험군. */
    C,
    /** 그 외 위험물(저위험군, 일반 위험물 신고 대상). */
    D
}
