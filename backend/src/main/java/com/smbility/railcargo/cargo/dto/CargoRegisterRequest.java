package com.smbility.railcargo.cargo.dto;

import com.smbility.railcargo.cargo.domain.ServiceMode;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 화주가 화물을 등록할 때의 요청.
 * {@code rawInput}에는 자연어 상품 설명, 상품 URL, 발주서·송장 내용 등을 그대로 담아
 * AI 분석({@code CargoAiAnalysisService})의 입력으로 사용한다.
 */
public record CargoRegisterRequest(

        @NotBlank
        String cargoName,

        String rawInput,

        @NotBlank
        String originStation,

        @NotBlank
        String destinationStation,

        @NotNull @FutureOrPresent
        LocalDate desiredDate,

        @NotNull
        ServiceMode serviceMode,

        /** 화물가액(원). 적재보험료와 배상한도 산정을 위한 필수 입력값. */
        @NotNull @Positive
        BigDecimal declaredValueKrw
) {
}
