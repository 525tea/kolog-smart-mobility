package com.smbility.railcargo.demo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.smbility.railcargo.common.exception.BusinessException;
import com.smbility.railcargo.cargo.service.CargoService;
import com.smbility.railcargo.consolidation.service.ConsolidationService;
import com.smbility.railcargo.support.MySqlTestContainerSupport;
import java.time.LocalDate;
import java.time.MonthDay;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest(properties = {
        "app.demo.seed-enabled=true",
        "app.demo.schedule-refresh-enabled=false"
})
class DemoDataBootstrapServiceIntegrationTest extends MySqlTestContainerSupport {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ConsolidationService consolidationService;

    @Autowired
    private CargoService cargoService;

    @Test
    void notionCasesHaveEligibleRoutesAndRepresentativeCaseCompletesPaymentFlow() {
        assertThat(count("SELECT COUNT(*) FROM train WHERE train_number IN ('KLG-N01','KLG-N03','KLG-N08','KLG-N09')"))
                .isEqualTo(4);
        assertThat(count("SELECT COUNT(*) FROM consolidated_cargo WHERE demo_seed_key LIKE 'demo-group-notion-%'"))
                .isEqualTo(5);

        assertThat(value("SELECT volume_cbm FROM cargo_order WHERE demo_seed_key = 'demo-cargo-notion-01-complete'"))
                .isNull();
        assertThat(count("""
                SELECT COUNT(*) FROM reservation r
                JOIN consolidated_cargo c ON c.id = r.consolidated_cargo_id
                WHERE c.demo_seed_key = 'demo-group-notion-01-complete'
                  AND r.payment_status = 'VIRTUAL_PAID'
                  AND r.total_cost = 258000
                """))
                .isEqualTo(1);
        assertThat(count("""
                SELECT COUNT(*) FROM cargo_participation p
                JOIN cargo_order o ON o.id = p.cargo_order_id
                JOIN consolidated_cargo c ON c.id = p.consolidated_cargo_id
                WHERE o.demo_seed_key = 'demo-cargo-notion-01-complete'
                  AND c.demo_seed_key = 'demo-group-notion-01-complete'
                """))
                .isEqualTo(1);

        Long memberId = jdbcTemplate.queryForObject("SELECT id FROM member WHERE email = 'demo@kolog.kr'", Long.class);
        Long case01Id = cargoId("demo-cargo-notion-01-complete");
        Long case03Id = cargoId("demo-cargo-notion-03");
        Long case08Id = cargoId("demo-cargo-notion-08");
        Long case09Id = cargoId("demo-cargo-notion-09");

        jdbcTemplate.update("UPDATE cargo_order SET status = 'ANALYZED' WHERE id = ?", case01Id);
        assertThat(consolidationService.getCandidates(memberId, case01Id)).isNotEmpty();
        assertThat(consolidationService.getCandidates(memberId, case03Id)).isNotEmpty();
        assertThatThrownBy(() -> consolidationService.getCandidates(memberId, case08Id))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("MSDS");
        assertThat(consolidationService.getCandidates(memberId, case09Id)).isNotEmpty();
        assertThat(count("""
                SELECT COUNT(*) FROM cargo_participation p
                JOIN cargo_order o ON o.id = p.cargo_order_id
                WHERE o.demo_seed_key IN ('demo-cargo-notion-03', 'demo-cargo-notion-08', 'demo-cargo-notion-09')
                """))
                .isZero();

        assertThat(value("SELECT msds_data FROM cargo_order WHERE demo_seed_key = 'demo-cargo-notion-08'"))
                .isNull();
        assertThat(value("SELECT msds_data FROM cargo_order WHERE demo_seed_key = 'demo-cargo-notion-09'"))
                .isNotNull();
        var msds = cargoService.getMsds(memberId, case09Id);
        assertThat(msds.fileName()).isEqualTo("demo-msds-case-09.pdf");
        assertThat(msds.contentType()).isEqualTo("application/pdf");
        assertThat(msds.data()).isNotEmpty();
        assertThatThrownBy(() -> cargoService.getMsds(memberId, case08Id))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("제출된 MSDS");
        assertThat(value("SELECT desired_date FROM cargo_order WHERE demo_seed_key = 'demo-cargo-notion-01-complete'"))
                .isEqualTo(java.sql.Date.valueOf(nextOccurrence(MonthDay.of(8, 20))));
    }

    private int count(String sql) {
        return jdbcTemplate.queryForObject(sql, Integer.class);
    }

    private Object value(String sql) {
        return jdbcTemplate.queryForObject(sql, Object.class);
    }

    private Long cargoId(String seedKey) {
        return jdbcTemplate.queryForObject("SELECT id FROM cargo_order WHERE demo_seed_key = ?", Long.class, seedKey);
    }

    private LocalDate nextOccurrence(MonthDay monthDay) {
        LocalDate today = LocalDate.now();
        LocalDate occurrence = monthDay.atYear(today.getYear());
        return occurrence.isBefore(today) ? occurrence.plusYears(1) : occurrence;
    }
}
