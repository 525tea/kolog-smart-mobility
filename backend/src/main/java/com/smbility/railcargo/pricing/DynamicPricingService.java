package com.smbility.railcargo.pricing;

import com.smbility.railcargo.consolidation.domain.ConsolidatedCargo;

/**
 * 출발(모집 마감) 임박도에 따라 공동화물의 kg당 판매가를 계산한다.
 * 기획안 보충 "4) 동적 가격 구현 - 1단계: 규칙 기반 가격"에 대응한다.
 */
public interface DynamicPricingService {

    PriceQuote quote(ConsolidatedCargo consolidatedCargo);
}
