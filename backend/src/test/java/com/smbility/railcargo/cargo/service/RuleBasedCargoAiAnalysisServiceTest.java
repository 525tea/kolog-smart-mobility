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
    void 쉼표가_포함된_중량과_CBM을_정확히_추출한다() {
        CargoAiAnalysisResult result = service.analyze(cargoOrder(
                "냉장 딸기 생과",
                "4,200kg, 18.6CBM, 플라스틱 컨테이너 12개, 상단 적재 금지"));

        assertEquals(0, BigDecimal.valueOf(4200).setScale(2).compareTo(result.weightKg()));
        assertEquals(0, BigDecimal.valueOf(18.6).setScale(3).compareTo(result.volumeCbm()));
        assertTrue(result.handlingNote().contains("상단 적재 금지"));
    }

    @Test
    void 영하_1도는_냉동이_아니라_냉장으로_분류한다() {
        CargoAiAnalysisResult result = service.analyze(cargoOrder("딸기 생과", "냉장 -1도~5도 유지, 4,200kg"));

        assertEquals(TemperatureCondition.REFRIGERATED, result.temperatureCondition());
        assertEquals(0, BigDecimal.valueOf(0.20).compareTo(result.surchargeRate()));
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

    @Test
    void 정온_키워드는_상온과_구분하고_정액_전력비를_적용한다() {
        CargoAiAnalysisResult result = service.analyze(cargoOrder("의약품", "정온 20도 유지, 100kg, 1cbm"));

        assertEquals(TemperatureCondition.CONSTANT, result.temperatureCondition());
        assertEquals(0, BigDecimal.valueOf(0.15).compareTo(result.surchargeRate()));
        assertEquals(0, BigDecimal.valueOf(30_000).compareTo(result.fixedPowerFeeKrw()));
    }

    @Test
    void 전동_캠핑_소독_키워드의_내장_위험을_추론한다() {
        assertTrue(service.analyze(cargoOrder("전동 공구", "100kg, 1cbm")).hazardous());
        assertEquals("2급", service.analyze(cargoOrder("캠핑 용품", "100kg, 1cbm")).hazardClassCode());
        assertEquals("5급", service.analyze(cargoOrder("소독 용품", "100kg, 1cbm")).hazardClassCode());
    }

    @Test
    void 여러_특수조건은_가장_높은_할증률_하나만_적용한다() {
        CargoAiAnalysisResult result = service.analyze(cargoOrder("유리 대리석", "파손 주의 무거운 화물 100kg, 1cbm"));
        assertEquals(0, BigDecimal.valueOf(0.20).compareTo(result.surchargeRate()));
        assertTrue(result.handlingNote().contains("초과 중량"));
        assertTrue(result.handlingNote().contains("파손 주의"));
    }
}
