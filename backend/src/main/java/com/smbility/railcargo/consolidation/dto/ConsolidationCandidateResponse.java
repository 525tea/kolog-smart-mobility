package com.smbility.railcargo.consolidation.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 화면 "공동화물 추천" 응답: 화주가 등록한 화물 1건 기준으로 참여 가능한 후보를 보여준다. */
public record ConsolidationCandidateResponse(
        Long consolidatedCargoId,
        String originStation,
        String destinationStation,
        BigDecimal targetWeightKg,
        BigDecimal recruitedWeightKg,
        BigDecimal recruitmentRatePercent,
        LocalDateTime recruitmentDeadline,
        Long trainId,
        String trainNumber,
        LocalDateTime departureAt,
        LocalDateTime arrivalAt,
        BigDecimal availableWeightKg,
        BigDecimal suitabilityScore,
        BigDecimal estimatedSuccessProbability,
        BigDecimal estimatedFreightForOrder,
        BigDecimal estimatedSavingsForOrder,
        BigDecimal appliedRatePerKg,
        BigDecimal appliedDiscountRate,
        String pricingReason,
        /** 위험물 등급에 따른 할증률(0.12 = 12%). 위험물이 아니면 0. estimatedFreightForOrder에 이미 반영돼 있다. */
        BigDecimal hazardSurchargeRate,
        BigDecimal fixedPowerFeeKrw,
        /** 적재보험료(원). 화물가액을 신고하지 않았으면 0. */
        BigDecimal insuranceFeeKrw,
        /** 플랫폼 이용 수수료(원). */
        BigDecimal platformFeeKrw,
        /** 화주가 실제로 결제하는 최종 금액(원) = estimatedFreightForOrder + insuranceFeeKrw + platformFeeKrw. */
        BigDecimal totalPayableKrw
) {
}
