package com.smbility.railcargo.cargo.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.smbility.railcargo.cargo.domain.TemperatureCondition;
import com.smbility.railcargo.cargo.dto.CargoAiAnalysisResult;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

/**
 * 실제 Gemini API를 호출하지 않고, 모델이 반환했을 법한 JSON 문자열을 파싱하는 로직만 검증한다.
 */
class GeminiResponseParserTest {

    private final GeminiResponseParser parser = new GeminiResponseParser();

    @Test
    void 정상_JSON을_CargoAiAnalysisResult로_변환한다() {
        String json = """
                {
                  "weightKg": 820,
                  "volumeCbm": 1.2,
                  "temperatureCondition": "REFRIGERATED",
                  "hazardous": false,
                  "packagingType": "박스",
                  "handlingNote": "파손 주의",
                  "lowConfidenceFields": []
                }
                """;

        CargoAiAnalysisResult result = parser.parse(json);

        assertThat(result.weightKg()).isEqualByComparingTo(BigDecimal.valueOf(820));
        assertThat(result.volumeCbm()).isEqualByComparingTo(BigDecimal.valueOf(1.2));
        assertThat(result.temperatureCondition()).isEqualTo(TemperatureCondition.REFRIGERATED);
        assertThat(result.hazardous()).isFalse();
        assertThat(result.packagingType()).isEqualTo("박스");
        assertThat(result.handlingNote()).isEqualTo("파손 주의");
        assertThat(result.estimated()).isFalse();
    }

    @Test
    void lowConfidenceFields가_있으면_estimated가_true다() {
        String json = """
                {
                  "weightKg": 50,
                  "volumeCbm": 0.3,
                  "temperatureCondition": "ROOM",
                  "hazardous": false,
                  "packagingType": "박스",
                  "handlingNote": null,
                  "lowConfidenceFields": ["weightKg", "volumeCbm"]
                }
                """;

        CargoAiAnalysisResult result = parser.parse(json);

        assertThat(result.estimated()).isTrue();
        assertThat(result.lowConfidenceFields()).containsExactly("weightKg", "volumeCbm");
    }

    @Test
    void 알수없는_온도조건_문자열은_ROOM으로_대체한다() {
        String json = """
                {"weightKg": 100, "temperatureCondition": "UNKNOWN_VALUE", "hazardous": false, "lowConfidenceFields": []}
                """;

        CargoAiAnalysisResult result = parser.parse(json);

        assertThat(result.temperatureCondition()).isEqualTo(TemperatureCondition.ROOM);
    }

    @Test
    void volumeCbm이_없으면_null을_유지한다() {
        String json = """
                {"weightKg": 100, "temperatureCondition": "ROOM", "hazardous": false,
                 "lowConfidenceFields": ["volumeCbm"]}
                """;

        CargoAiAnalysisResult result = parser.parse(json);

        assertThat(result.volumeCbm()).isNull();
        assertThat(result.lowConfidenceFields()).contains("volumeCbm");
    }

    @Test
    void weightKg가_없으면_예외를_던진다() {
        String json = """
                {"temperatureCondition": "ROOM", "hazardous": false}
                """;

        assertThatThrownBy(() -> parser.parse(json)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void JSON이_아니면_예외를_던진다() {
        assertThatThrownBy(() -> parser.parse("이건 JSON이 아닙니다")).isInstanceOf(IllegalArgumentException.class);
    }
}
