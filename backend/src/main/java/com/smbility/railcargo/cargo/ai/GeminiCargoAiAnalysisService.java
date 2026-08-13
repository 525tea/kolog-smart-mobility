package com.smbility.railcargo.cargo.ai;

import com.smbility.railcargo.cargo.domain.CargoOrder;
import com.smbility.railcargo.cargo.dto.CargoAiAnalysisResult;
import com.smbility.railcargo.cargo.service.CargoAiAnalysisService;
import com.smbility.railcargo.cargo.service.RuleBasedCargoAiAnalysisService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

/**
 * Google Gemini API(무료 등급)로 화물 운송조건을 실제로 추출하는 구현체.
 * {@code app.ai.provider=gemini} 로 설정했을 때만 활성화되며(기획안 보충 5번 "화물 정보 구조화" 대응),
 * API 호출/파싱이 실패하면 서비스가 죽지 않도록 {@link RuleBasedCargoAiAnalysisService}로 안전하게 폴백한다.
 */
@Slf4j
@Service
@Primary
@ConditionalOnProperty(prefix = "app.ai", name = "provider", havingValue = "gemini")
public class GeminiCargoAiAnalysisService implements CargoAiAnalysisService {

    private static final String PROMPT_TEMPLATE = """
            너는 철도 화물 운송 플랫폼의 화물 정보 구조화 어시스턴트다.
            아래 화주가 입력한 화물명과 원본 텍스트(자연어 설명, 상품 URL, 발주서/송장 내용 등)를 분석해서
            운송에 필요한 조건을 JSON으로만 응답해라. 다른 설명은 절대 붙이지 마라.

            화물명: %s
            원본 입력: %s

            다음 JSON 스키마를 정확히 따라라:
            {
              "weightKg": number,               // 총 중량(kg). 명시가 없으면 화물 종류로 보수적으로 추정
              "volumeCbm": number | null,        // 부피(m3). 명시가 없으면 null
              "temperatureCondition": "ROOM" | "CONSTANT" | "REFRIGERATED" | "FROZEN",
              "hazardous": boolean,
              "hazardGrade": "A" | "B" | "C" | "D" | null,
              "hazardClassCode": "1급" | "2급" | "3급" | "4급" | "5급" | "6급" | "7급" | "8급" | "9급" | null,
              "hazardClassName": string | null,
              "rejected": boolean,               // 1급 폭발물만 true
              "requiresMsds": boolean,
              "surchargeRate": number,            // 0.20처럼 소수. 위험물/콜드체인/특수화물 중 최고 요율 하나
              "fixedPowerFeeKrw": number,         // 냉동 50000, 냉장 40000, 정온 30000, 일반 0
              "detectedTemperatureC": number | null,
              "detectedSpecialCargoCodes": string[], // OVERWEIGHT, FRAGILE, ODOROUS, LIQUID_BULK
              "packagingType": string,           // 예: 박스, 팔레트, 컨테이너
              "handlingNote": string | null,     // 파손주의 등 취급 유의사항, 없으면 null
              "lowConfidenceFields": string[]    // 텍스트에 명시되지 않아 추정한 필드명 목록 (예: ["weightKg"])
            }

            위험물은 1~9급으로 분류하고 1급은 rejected=true로 운송을 차단한다. 2~9급 위험물은 20%% 할증이다.
            화물명에 전동, 스마트, 캠핑, 소독 같은 단어가 있으면 배터리·가스·산화성 물질 등 내장 위험을 추론하라.
            콜드체인은 냉동 30%%+50000원, 냉장 20%%+40000원, 정온 15%%+30000원이다.
            특수화물은 OVERWEIGHT 20%%, FRAGILE 10%%, ODOROUS 20%%, LIQUID_BULK 15%%다.
            여러 할증 조건이 겹치면 surchargeRate에는 가장 높은 비율 하나만 넣고 전력비는 별도로 넣어라.
            """;

    private final GeminiClient geminiClient;
    private final GeminiResponseParser responseParser;
    private final RuleBasedCargoAiAnalysisService fallback;

    public GeminiCargoAiAnalysisService(GeminiClient geminiClient, GeminiResponseParser responseParser,
                                         RuleBasedCargoAiAnalysisService fallback) {
        this.geminiClient = geminiClient;
        this.responseParser = responseParser;
        this.fallback = fallback;
    }

    @Override
    public CargoAiAnalysisResult analyze(CargoOrder cargoOrder) {
        try {
            String prompt = PROMPT_TEMPLATE.formatted(
                    cargoOrder.getCargoName(),
                    cargoOrder.getRawInput() == null ? "" : cargoOrder.getRawInput());
            String rawJson = geminiClient.generateJson(prompt);
            return responseParser.parse(rawJson);
        } catch (Exception e) {
            log.warn("Gemini 화물 분석 실패, 규칙 기반으로 폴백합니다. cargoOrderId={}, cause={}",
                    cargoOrder.getId(), e.toString());
            return fallback.analyze(cargoOrder);
        }
    }
}
