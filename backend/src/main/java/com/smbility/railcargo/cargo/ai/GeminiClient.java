package com.smbility.railcargo.cargo.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Google Gemini API(generateContent) 얇은 래퍼.
 * 구조화된 JSON 응답(responseMimeType=application/json)을 요청하고 모델이 생성한 JSON 문자열을 그대로 반환한다.
 */
@Slf4j
@Component
public class GeminiClient {

    private final RestClient restClient;
    private final GeminiProperties properties;
    // Boot 4 웹 스택은 MVC 메시지 컨버터 내부에 자체 ObjectMapper를 구성해두지만 별도 빈으로 노출하지 않는다.
    // 이 클래스는 단순 JSON 조립/파싱만 하므로 전역 설정과 무관한 전용 인스턴스를 직접 만들어 쓴다.
    private final ObjectMapper objectMapper = new ObjectMapper();

    public GeminiClient(GeminiProperties properties) {
        this.properties = properties;
        this.restClient = RestClient.create();
    }

    /**
     * @param prompt 모델에게 전달할 프롬프트
     * @return 모델이 생성한 원시 JSON 문자열 (파싱은 호출자의 몫)
     */
    public String generateJson(String prompt) {
        String url = "%s/%s:generateContent?key=%s".formatted(properties.endpoint(), properties.model(), properties.apiKey());

        ObjectNode requestBody = objectMapper.createObjectNode();
        ObjectNode content = requestBody.putArray("contents").addObject();
        content.putArray("parts").addObject().put("text", prompt);
        ObjectNode generationConfig = requestBody.putObject("generationConfig");
        generationConfig.put("responseMimeType", "application/json");
        generationConfig.put("temperature", 0.1);

        JsonNode response = restClient.post()
                .uri(url)
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .body(JsonNode.class);

        return extractText(response);
    }

    private String extractText(JsonNode response) {
        if (response == null) {
            throw new IllegalStateException("Gemini API 응답이 비어 있습니다.");
        }
        JsonNode textNode = response.path("candidates").path(0)
                .path("content").path("parts").path(0).path("text");
        if (textNode.isMissingNode() || textNode.isNull()) {
            throw new IllegalStateException("Gemini API 응답에서 텍스트를 찾을 수 없습니다: " + response);
        }
        return textNode.asText();
    }
}
