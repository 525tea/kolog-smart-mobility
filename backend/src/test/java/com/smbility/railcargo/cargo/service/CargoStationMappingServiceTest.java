package com.smbility.railcargo.cargo.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CargoStationMappingServiceTest {
    private final CargoStationMappingService service = new CargoStationMappingService();

    @Test
    void 기획안의_권역별_철도_거점으로_변환한다() {
        assertThat(service.map("서울시 강남구 역삼동").railStation()).isEqualTo("오봉역(의왕)");
        assertThat(service.map("경기도 화성시 동탄").railStation()).isEqualTo("서화성역");
        assertThat(service.map("강원도 강릉시").railStation()).isEqualTo("안인역(강릉)");
        assertThat(service.map("광양시").mappedStationId()).isEqualTo("ST_GWANGYANG");
        assertThat(service.map("경기").railStation()).isEqualTo("오봉역(의왕)");
    }

    @Test
    void 기존_서비스_역명은_그대로_사용한다() {
        assertThat(service.map("부산진역").railStation()).isEqualTo("부산진역");
        assertThat(service.map("부산진역").mapped()).isFalse();
        assertThat(service.map("오봉역").railStation()).isEqualTo("오봉역(의왕)");
    }

    @Test
    void 지원_권역을_찾지_못하면_200_응답용_트럭_대안을_반환한다() {
        var response = service.map("제주도 제주시");
        assertThat(response.outOfCoverage()).isTrue();
        assertThat(response.railStation()).isNull();
        assertThat(response.userMessage()).contains("트럭 직배송");
    }

    @Test
    void 여러_지역명이_겹치면_더_구체적인_키워드를_우선한다() {
        assertThat(service.map("부산광역시 강서구 명지동").railStation()).isEqualTo("북철송장(부산신항)");
    }
}
