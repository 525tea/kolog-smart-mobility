package com.smbility.railcargo.cargo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.smbility.railcargo.auth.domain.Member;
import com.smbility.railcargo.auth.domain.MemberRole;
import com.smbility.railcargo.cargo.domain.CargoOrder;
import com.smbility.railcargo.cargo.domain.TemperatureCondition;
import com.smbility.railcargo.cargo.document.CargoDocumentExtractionService;
import com.smbility.railcargo.cargo.document.DocumentAiProperties;
import com.smbility.railcargo.cargo.dto.CargoAiAnalysisResult;
import com.smbility.railcargo.shipper.domain.Shipper;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

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

    @Test
    void notion_01_부피는_만들지_않고_박스수를_추출한다() {
        CargoAiAnalysisResult result = service.analyze(cargoOrder("즉석밥",
                "즉석밥 500kg을 보내려고 합니다. 20kg씩 박스에 포장되어 있고 총 25박스입니다. 별도의 온도관리는 필요하지 않습니다."));

        assertEquals(0, BigDecimal.valueOf(500).setScale(2).compareTo(result.weightKg()));
        assertNull(result.volumeCbm());
        assertEquals("25BOX", result.packagingType());
        assertEquals(TemperatureCondition.ROOM, result.temperatureCondition());
    }

    @Test
    void notion_02_온도와_종이박스를_추출한다() {
        CargoAiAnalysisResult result = service.analyze(cargoOrder("사과",
                "사과 300킬로를 보내야 합니다. 신선도 유지를 위해 2도 정도로 운송해주세요. 종이박스에 담겨 있습니다."));

        assertEquals(0, BigDecimal.valueOf(300).setScale(2).compareTo(result.weightKg()));
        assertEquals(0, BigDecimal.valueOf(2).compareTo(result.detectedTemperatureC()));
        assertEquals(TemperatureCondition.REFRIGERATED, result.temperatureCondition());
        assertEquals("종이박스", result.packagingType());
    }

    @Test
    void notion_03_영하18도이하를_냉동으로_해석한다() {
        CargoAiAnalysisResult result = service.analyze(cargoOrder("냉동만두",
                "냉동만두 800킬로 정도 운송하고 싶습니다. 계속 영하 18도 이하로 유지되어야 합니다. 20kg 박스 40개입니다."));

        assertEquals(0, BigDecimal.valueOf(800).setScale(2).compareTo(result.weightKg()));
        assertEquals(0, BigDecimal.valueOf(-18).compareTo(result.detectedTemperatureC()));
        assertEquals(TemperatureCondition.FROZEN, result.temperatureCondition());
        assertEquals("40BOX", result.packagingType());
    }

    @Test
    void notion_04_박스규격과_수량으로_부피를_계산한다() {
        CargoAiAnalysisResult result = service.analyze(cargoOrder("생활용품",
                "생활용품 100박스입니다. 박스 하나당 60cm × 40cm × 40cm이고 총중량은 1,200kg입니다."));

        assertEquals(0, BigDecimal.valueOf(1200).setScale(2).compareTo(result.weightKg()));
        assertEquals(0, BigDecimal.valueOf(9.6).setScale(3).compareTo(result.volumeCbm()));
        assertEquals("100BOX", result.packagingType());
        assertFalse(result.lowConfidenceFields().contains("volumeCbm"));
    }

    @Test
    void notion_05_06_애매한_온도를_임의수치화하지_않는다() {
        CargoAiAnalysisResult salmon = service.analyze(cargoOrder("연어", "냉장 연어 400kg입니다. 신선하게 운송해주세요."));
        CargoAiAnalysisResult tangerine = service.analyze(cargoOrder("감귤", "감귤 250kg입니다. 너무 덥지 않게 신선하게 운송해주세요."));

        assertNull(salmon.detectedTemperatureC());
        assertNull(tangerine.detectedTemperatureC());
        assertTrue(salmon.lowConfidenceFields().contains("temperatureCondition"));
        assertTrue(tangerine.lowConfidenceFields().contains("temperatureCondition"));
        assertTrue(tangerine.analysisWarnings().stream().anyMatch(message -> message.contains("임의의 온도")));
    }

    @Test
    void notion_07_복수품목을_분리하고_중량을_합산한다() {
        CargoAiAnalysisResult result = service.analyze(cargoOrder("냉동식품",
                "냉동만두 300kg과 냉동볶음밥 200kg을 부산에서 서울로 보내려고 합니다. 두 제품 모두 영하 18도 이하로 유지해주세요."));

        assertEquals(0, BigDecimal.valueOf(500).setScale(2).compareTo(result.weightKg()));
        assertEquals(java.util.List.of("냉동만두 300kg", "냉동볶음밥 200kg"), result.detectedItems());
    }

    @Test
    void notion_08_09_산업용화학제품은_msds를_요구하되_등급을_확정하지_않는다() {
        for (String input : new String[]{"산업용 세정제 200kg", "산업용 화학제품 200kg, MSDS 보유"}) {
            CargoAiAnalysisResult result = service.analyze(cargoOrder("산업용 제품", input));
            assertTrue(result.hazardous());
            assertTrue(result.requiresMsds());
            assertNull(result.hazardClassCode());
            assertTrue(result.lowConfidenceFields().contains("hazardGrade"));
            assertTrue(result.analysisWarnings().stream().anyMatch(message -> message.contains("등급은 확정하지")));
        }
    }

    @Test
    void notion_10_사용자선택과_분석결과_불일치를_경고한다() {
        CargoAiAnalysisResult result = service.analyze(cargoOrder("냉동만두",
                "냉동만두 300kg, 영하 18도 이하. 화물유형: 일반화물"));

        assertEquals(TemperatureCondition.FROZEN, result.temperatureCondition());
        assertTrue(result.analysisWarnings().stream().anyMatch(message -> message.contains("다릅니다")));
    }

    @Test
    void notion_11_대형기계는_규격없이_운송가능여부를_확정하지_않는다() {
        CargoAiAnalysisResult result = service.analyze(cargoOrder("산업용 기계",
                "공장용 대형 기계 1대, 무게는 15톤이고 크기가 상당히 큰 편입니다."));

        assertEquals(0, BigDecimal.valueOf(15000).setScale(2).compareTo(result.weightKg()));
        assertNull(result.volumeCbm());
        assertTrue(result.analysisWarnings().stream().anyMatch(message -> message.contains("컨테이너 적합성")));
    }

    @Test
    void notion_12_서로다른_박스규격의_부피는_임의생성하지_않는다() {
        CargoAiAnalysisResult result = service.analyze(cargoOrder("전자제품",
                "전자제품 약 50박스 정도를 서울로 보내려고 합니다. 박스는 크기가 조금씩 다르고 총 무게는 700kg 정도입니다."));

        assertEquals(0, BigDecimal.valueOf(700).setScale(2).compareTo(result.weightKg()));
        assertEquals("50BOX", result.packagingType());
        assertNull(result.volumeCbm());
        assertTrue(result.lowConfidenceFields().contains("volumeCbm"));
    }

    @Test
    void notion_13_pdf송장_추출결과를_화물조건으로_분석한다() {
        CargoAiAnalysisResult result = analyzeDemoDocument(
                "INV_001_냉동닭가슴살.pdf", "application/pdf", "%PDF-1.7".getBytes());

        assertEquals(0, BigDecimal.valueOf(500).setScale(2).compareTo(result.weightKg()));
        assertEquals(TemperatureCondition.FROZEN, result.temperatureCondition());
        assertEquals(0, BigDecimal.valueOf(-18).compareTo(result.detectedTemperatureC()));
        assertEquals("100BOX", result.packagingType());
    }

    @Test
    void notion_14_png발주서_추출결과를_화물조건으로_분석한다() {
        CargoAiAnalysisResult result = analyzeDemoDocument(
                "PO_002_생수.png", "image/png", new byte[]{1, 2, 3});

        assertEquals(0, BigDecimal.valueOf(1000).setScale(2).compareTo(result.weightKg()));
        assertEquals(TemperatureCondition.ROOM, result.temperatureCondition());
        assertEquals("1000BOX", result.packagingType());
    }

    @Test
    void notion_15_엑셀의_여러행을_품목별로_분리하고_합산한다() {
        CargoAiAnalysisResult result = service.analyze(cargoOrder("화물목록",
                "[시트: 화물목록]\n품목\t중량\t온도\n사과\t300kg\t2℃\n생수\t500kg\t상온\n생활용품\t800kg\t상온"));

        assertEquals(0, BigDecimal.valueOf(1600).setScale(2).compareTo(result.weightKg()));
        assertTrue(result.detectedItems().contains("사과 300kg"));
        assertTrue(result.detectedItems().contains("생수 500kg"));
        assertTrue(result.detectedItems().contains("생활용품 800kg"));
    }

    private CargoAiAnalysisResult analyzeDemoDocument(String fileName, String contentType, byte[] bytes) {
        CargoDocumentExtractionService documentService = new CargoDocumentExtractionService(
                new DocumentAiProperties(false, "", "us", ""));
        var extracted = documentService.extract(new MockMultipartFile("file", fileName, contentType, bytes));
        return service.analyze(cargoOrder(fileName, extracted.extractedText()));
    }
}
