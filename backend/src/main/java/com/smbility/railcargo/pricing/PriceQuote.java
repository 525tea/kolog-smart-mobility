package com.smbility.railcargo.pricing;

import java.math.BigDecimal;

/**
 * 특정 시점에 계산된 kg당 판매가.
 *
 * @param ratePerKg    실제 적용할 kg당 가격(원)
 * @param discountRate 기준운임 대비 할인율 (0~1)
 * @param feasible     false면 가격 하한 미만이라 상품 자체를 판매할 수 없다는 뜻
 * @param reason       어떤 규칙이 적용됐는지 설명 (로그/디버깅/화면 노출용)
 */
public record PriceQuote(BigDecimal ratePerKg, BigDecimal discountRate, boolean feasible, String reason) {
}
