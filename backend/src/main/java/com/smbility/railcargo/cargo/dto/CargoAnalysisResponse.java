package com.smbility.railcargo.cargo.dto;

import java.util.List;

/**
 * AI 분석 직후에만 반환하는 응답. {@link CargoAiAnalysisResult#lowConfidenceFields}는
 * {@link com.smbility.railcargo.cargo.domain.CargoOrder}에 영속화되는 값이 아니라 분석 시점에만 존재하므로,
 * 등록/조회/수정 응답({@link CargoResponse})과 분리해 이 시점에만 함께 내려준다.
 */
public record CargoAnalysisResponse(
        CargoResponse cargo,
        List<String> lowConfidenceFields
) {
    public static CargoAnalysisResponse of(CargoResponse cargo, List<String> lowConfidenceFields) {
        return new CargoAnalysisResponse(cargo, lowConfidenceFields);
    }
}
