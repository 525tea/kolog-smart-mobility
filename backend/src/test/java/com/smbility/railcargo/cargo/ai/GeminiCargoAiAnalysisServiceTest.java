package com.smbility.railcargo.cargo.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.smbility.railcargo.auth.domain.Member;
import com.smbility.railcargo.auth.domain.MemberRole;
import com.smbility.railcargo.cargo.domain.CargoOrder;
import com.smbility.railcargo.cargo.domain.TemperatureCondition;
import com.smbility.railcargo.cargo.dto.CargoAiAnalysisResult;
import com.smbility.railcargo.cargo.service.RuleBasedCargoAiAnalysisService;
import com.smbility.railcargo.shipper.domain.Shipper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GeminiCargoAiAnalysisServiceTest {

    @Mock
    private GeminiClient geminiClient;

    @Mock
    private GeminiResponseParser responseParser;

    private final RuleBasedCargoAiAnalysisService fallback = new RuleBasedCargoAiAnalysisService();

    private CargoOrder cargoOrder() {
        Member member = Member.of("shipper@example.com", "encoded", MemberRole.SHIPPER);
        Shipper shipper = Shipper.of(member, "123-45-67890", "테스트상사", "홍길동", "010-0000-0000");
        return CargoOrder.register(shipper, "냉동식품", "냉동 300kg", "천안", "서울", LocalDate.now().plusDays(1));
    }

    @Test
    void 정상_호출시_파싱된_결과를_그대로_반환한다() {
        GeminiCargoAiAnalysisService service = new GeminiCargoAiAnalysisService(geminiClient, responseParser, fallback);
        CargoAiAnalysisResult expected = new CargoAiAnalysisResult(BigDecimal.valueOf(300), BigDecimal.valueOf(1),
                TemperatureCondition.FROZEN, false, null, "박스", null, List.of());

        when(geminiClient.generateJson(anyString())).thenReturn("{...}");
        when(responseParser.parse("{...}")).thenReturn(expected);

        CargoAiAnalysisResult result = service.analyze(cargoOrder());

        assertThat(result).isEqualTo(expected);
    }

    @Test
    void API_호출이_실패하면_규칙기반으로_폴백한다() {
        GeminiCargoAiAnalysisService service = new GeminiCargoAiAnalysisService(geminiClient, responseParser, fallback);

        when(geminiClient.generateJson(anyString())).thenThrow(new RuntimeException("network error"));

        CargoAiAnalysisResult result = service.analyze(cargoOrder());

        // fallback(RuleBasedCargoAiAnalysisService)이 "냉동" 키워드를 인식해 FROZEN으로 분류하는지로 폴백 동작 확인
        assertThat(result.temperatureCondition()).isEqualTo(TemperatureCondition.FROZEN);
        verify(geminiClient).generateJson(anyString());
    }
}
