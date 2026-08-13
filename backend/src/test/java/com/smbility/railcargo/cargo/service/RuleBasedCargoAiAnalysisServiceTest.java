package com.smbility.railcargo.cargo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.smbility.railcargo.auth.domain.Member;
import com.smbility.railcargo.auth.domain.MemberRole;
import com.smbility.railcargo.cargo.domain.CargoOrder;
import com.smbility.railcargo.cargo.domain.TemperatureCondition;
import com.smbility.railcargo.cargo.dto.CargoAiAnalysisResult;
import com.smbility.railcargo.shipper.domain.Shipper;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class RuleBasedCargoAiAnalysisServiceTest {

    private final RuleBasedCargoAiAnalysisService service = new RuleBasedCargoAiAnalysisService();

    private CargoOrder cargoOrder(String cargoName, String rawInput) {
        Member member = Member.of("shipper@example.com", "encoded", MemberRole.SHIPPER);
        Shipper shipper = Shipper.of(member, "123-45-67890", "테스트상사", "홍길동", "010-0000-0000");
        return CargoOrder.register(shipper, cargoName, rawInput, "천안", "서울", LocalDate.now().plusDays(1));
    }

    @Test
    void 냉동_키워드가_있으면_FROZEN으로_분류한다() {
        CargoOrder order = cargoOrder("아이스크림", "냉동 상태로 배송 필요, 100kg");

        CargoAiAnalysisResult result = service.analyze(order);

        assertEquals(TemperatureCondition.FROZEN, result.temperatureCondition());
        assertEquals(0, BigDecimal.valueOf(100).setScale(2).compareTo(result.weightKg()));
    }

    @Test
    void 위험물_키워드가_있으면_hazardous가_true다() {
        CargoOrder order = cargoOrder("스프레이 캔", "위험물 인화성 물질 주의");

        CargoAiAnalysisResult result = service.analyze(order);

        assertTrue(result.hazardous());
    }

    @Test
    void 톤_단위는_kg으로_환산된다() {
        CargoOrder order = cargoOrder("농산물", "2톤 분량의 감자");

        CargoAiAnalysisResult result = service.analyze(order);

        assertEquals(0, BigDecimal.valueOf(2000).setScale(2).compareTo(result.weightKg()));
    }

    @Test
    void 부피를_입력하지_않으면_임의의_값을_생성하지_않는다() {
        CargoOrder order = cargoOrder("일반 상품", "특별한 설명 없음");

        CargoAiAnalysisResult result = service.analyze(order);

        assertNull(result.volumeCbm());
        assertEquals(TemperatureCondition.ROOM, result.temperatureCondition());
        assertFalse(result.hazardous());
        assertTrue(result.estimated());
    }
}
