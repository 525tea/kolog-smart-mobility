package com.smbility.railcargo.demo;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 심사위원이 공유 계정으로 로그인하자마자 모든 주요 화면을 둘러볼 수 있도록 시연 데이터를 보장한다.
 * demo_seed_key가 붙은 행만 갱신하므로 일반 사용자 데이터는 건드리지 않는다.
 */
@Component
@Order(20)
@ConditionalOnProperty(prefix = "app.demo", name = "seed-enabled", havingValue = "true")
public class DemoDataBootstrapService implements ApplicationRunner {

    private static final String DEMO_EMAIL = "demo@kolog.kr";
    private static final String DEMO_PASSWORD = "demo1234";

    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;

    public DemoDataBootstrapService(JdbcTemplate jdbcTemplate, PasswordEncoder passwordEncoder) {
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Long demoMemberId = ensureMember(DEMO_EMAIL, DEMO_PASSWORD);
        Long demoShipperId = ensureShipper(demoMemberId, "525-08-26080", "(주)한국물류", "김물류", "010-5250-2608");

        Long freshMemberId = ensureMember("demo.partner.fresh@kolog.invalid", "disabled-demo-account");
        Long freshShipperId = ensureShipper(freshMemberId, "525-08-26081", "(주)신선마트", "박신선", "010-0000-0001");
        Long greenMemberId = ensureMember("demo.partner.green@kolog.invalid", "disabled-demo-account");
        Long greenShipperId = ensureShipper(greenMemberId, "525-08-26082", "(주)그린팜", "이그린", "010-0000-0002");

        resetDemoWagonCapacities();
        LocalDate departureDate = LocalDate.now().plusDays(1);
        Long demoCargoId = upsertCargo("demo-cargo-main", demoShipperId, "냉동 만두 · 가공식품", 200, 1.8,
                "FROZEN", "파렛트 8개", departureDate, "RESERVED");
        Long freshCargoId = upsertCargo("demo-cargo-fresh", freshShipperId, "냉동 수산물", 180, 1.6,
                "FROZEN", "파렛트 6개", departureDate, "RESERVED");
        Long greenCargoId = upsertCargo("demo-cargo-green", greenShipperId, "냉장 채소류", 240, 2.1,
                "FROZEN", "파렛트 9개", departureDate, "RESERVED");
        Long reviewFreshCargoId = upsertCargo("demo-cargo-review-fresh", freshShipperId, "신선식품 공동화 검토", 400, 2.8,
                "ROOM", "파렛트 10개", departureDate, "PARTICIPATING");
        Long reviewGreenCargoId = upsertCargo("demo-cargo-review-green", greenShipperId, "농산물 공동화 검토", 400, 3.0,
                "ROOM", "파렛트 11개", departureDate, "PARTICIPATING");

        Long activeWagonId = wagonId("KLG-404", "REFRIGERATED");
        Long activeGroupId = upsertGroup("demo-group-active", "의왕ICD", "부산진역", "FROZEN",
                820, 620, departureDate, activeWagonId, "CONFIRMED", LocalDateTime.now().plusDays(1).withHour(19).withMinute(30));

        upsertGroup("demo-group-uiwang-room", "의왕ICD", "부산진역", "ROOM",
                820, 420, departureDate, null, "RECRUITING", LocalDateTime.now().plusDays(1).withHour(19).withMinute(30));
        upsertGroup("demo-group-uiwang-frozen", "의왕ICD", "부산진역", "FROZEN",
                820, 300, departureDate, null, "RECRUITING", LocalDateTime.now().plusDays(1).withHour(21).withMinute(10));
        upsertGroup("demo-group-obong-busan", "오봉역", "부산진역", "ROOM",
                820, 540, departureDate.plusDays(1), null, "RECRUITING", LocalDateTime.now().plusDays(2).withHour(0).withMinute(0));
        upsertGroup("demo-group-uiwang-incheon", "의왕ICD", "동인천역", "ROOM",
                820, 160, departureDate.plusDays(1), null, "RECRUITING", LocalDateTime.now().plusDays(2).withHour(1).withMinute(40));
        upsertGroup("demo-group-obong-daegu", "오봉역", "동대구역", "ROOM",
                820, 320, departureDate.plusDays(1), null, "RECRUITING", LocalDateTime.now().plusDays(2).withHour(3).withMinute(20));

        Long reviewWagonId = wagonId("KLG-512", "CONTAINER");
        Long reviewGroupId = upsertGroup("demo-group-review", "의왕ICD", "부산진역", "ROOM",
                800, 800, departureDate, reviewWagonId, "PENDING_APPROVAL",
                LocalDateTime.now().plusDays(1).withHour(21).withMinute(10));

        resetParticipations(activeGroupId, List.of(demoCargoId, freshCargoId, greenCargoId));
        resetReviewParticipations(reviewGroupId, reviewFreshCargoId, reviewGreenCargoId);
        resetMatchPrediction(reviewGroupId, reviewWagonId);
        upsertReservation(activeGroupId, activeWagonId);
        resetNotifications(demoMemberId);
    }

    private Long ensureMember(String email, String rawPassword) {
        String encodedPassword = passwordEncoder.encode(rawPassword);
        jdbcTemplate.update("""
                INSERT INTO member (email, password, role, created_at, updated_at)
                VALUES (?, ?, 'SHIPPER', NOW(), NOW())
                ON DUPLICATE KEY UPDATE password = VALUES(password), role = 'SHIPPER', updated_at = NOW()
                """, email, encodedPassword);
        return jdbcTemplate.queryForObject("SELECT id FROM member WHERE email = ?", Long.class, email);
    }

    private Long ensureShipper(Long memberId, String businessNumber, String companyName, String managerName, String phone) {
        jdbcTemplate.update("""
                INSERT INTO shipper (member_id, business_number, company_name, manager_name, phone, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, NOW(), NOW())
                ON DUPLICATE KEY UPDATE business_number = VALUES(business_number), company_name = VALUES(company_name),
                    manager_name = VALUES(manager_name), phone = VALUES(phone), updated_at = NOW()
                """, memberId, businessNumber, companyName, managerName, phone);
        return jdbcTemplate.queryForObject("SELECT id FROM shipper WHERE member_id = ?", Long.class, memberId);
    }

    private Long upsertCargo(String seedKey, Long shipperId, String cargoName, int weightKg, double volumeCbm,
                             String temperature, String packaging, LocalDate desiredDate, String status) {
        jdbcTemplate.update("""
                INSERT INTO cargo_order (
                    shipper_id, cargo_name, raw_input, origin_station, destination_station, desired_date,
                    weight_kg, volume_cbm, temperature_condition, hazardous, packaging_type, handling_note,
                    status, hazard_grade, declared_value_krw, hazard_class_code, hazard_class_name,
                    transport_rejected, requires_msds, surcharge_rate, fixed_power_fee_krw,
                    detected_temperature_c, demo_seed_key, created_at, updated_at)
                VALUES (?, ?, '[DEMO] 심사위원용 시연 화물', '의왕ICD', '부산진역', ?, ?, ?, ?, FALSE, ?,
                    '이중 단열 포장·콜드체인 유지', ?, NULL, 10000000, NULL, NULL, FALSE, FALSE, 0.20,
                    15000, -18, ?, NOW(), NOW())
                ON DUPLICATE KEY UPDATE shipper_id = VALUES(shipper_id), cargo_name = VALUES(cargo_name),
                    desired_date = VALUES(desired_date), weight_kg = VALUES(weight_kg), volume_cbm = VALUES(volume_cbm),
                    temperature_condition = VALUES(temperature_condition), packaging_type = VALUES(packaging_type),
                    status = VALUES(status), updated_at = NOW()
                """, shipperId, cargoName, desiredDate, BigDecimal.valueOf(weightKg), BigDecimal.valueOf(volumeCbm),
                temperature, packaging, status, seedKey);
        return jdbcTemplate.queryForObject("SELECT id FROM cargo_order WHERE demo_seed_key = ?", Long.class, seedKey);
    }

    private Long upsertGroup(String seedKey, String origin, String destination, String temperature,
                             int targetWeight, int recruitedWeight, LocalDate desiredDate, Long wagonId,
                             String status, LocalDateTime deadline) {
        jdbcTemplate.update("""
                INSERT INTO consolidated_cargo (
                    origin_station, destination_station, temperature_condition, hazardous, target_weight_kg,
                    recruited_weight_kg, recruitment_deadline, desired_date, matched_wagon_id, status,
                    demo_seed_key, created_at, updated_at)
                VALUES (?, ?, ?, FALSE, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())
                ON DUPLICATE KEY UPDATE origin_station = VALUES(origin_station), destination_station = VALUES(destination_station),
                    temperature_condition = VALUES(temperature_condition), target_weight_kg = VALUES(target_weight_kg),
                    recruited_weight_kg = VALUES(recruited_weight_kg), recruitment_deadline = VALUES(recruitment_deadline),
                    desired_date = VALUES(desired_date), matched_wagon_id = VALUES(matched_wagon_id),
                    status = VALUES(status), updated_at = NOW()
                """, origin, destination, temperature, BigDecimal.valueOf(targetWeight), BigDecimal.valueOf(recruitedWeight),
                Timestamp.valueOf(deadline), desiredDate, wagonId, status, seedKey);
        return jdbcTemplate.queryForObject("SELECT id FROM consolidated_cargo WHERE demo_seed_key = ?", Long.class, seedKey);
    }

    private Long wagonId(String trainNumber, String wagonType) {
        return jdbcTemplate.queryForObject("""
                SELECT w.id FROM wagon w JOIN train t ON t.id = w.train_id
                WHERE t.train_number = ? AND w.wagon_type = ? ORDER BY w.id LIMIT 1
                """, Long.class, trainNumber, wagonType);
    }

    private void resetDemoWagonCapacities() {
        jdbcTemplate.update("""
                UPDATE wagon w JOIN train t ON t.id = w.train_id
                SET w.remaining_weight_kg = CASE
                    WHEN t.train_number = 'KLG-404' AND w.wagon_type = 'CONTAINER' THEN 460
                    WHEN t.train_number = 'KLG-404' THEN 200
                    WHEN t.train_number = 'KLG-512' AND w.wagon_type = 'CONTAINER' THEN 620
                    WHEN t.train_number = 'KLG-512' THEN 820
                    WHEN t.train_number = 'KLG-338' THEN 280
                    WHEN t.train_number = 'KLG-711' THEN 740
                    WHEN t.train_number = 'KLG-219' THEN 580
                    ELSE w.remaining_weight_kg END,
                    w.updated_at = NOW()
                WHERE t.train_number IN ('KLG-404', 'KLG-512', 'KLG-338', 'KLG-711', 'KLG-219')
                """);
    }

    private void resetParticipations(Long groupId, List<Long> cargoIds) {
        jdbcTemplate.update("DELETE FROM cargo_participation WHERE cargo_order_id IN (?, ?, ?)",
                cargoIds.get(0), cargoIds.get(1), cargoIds.get(2));
        int[] costs = {103200, 91800, 116400};
        for (int i = 0; i < cargoIds.size(); i++) {
            jdbcTemplate.update("""
                    INSERT INTO cargo_participation (cargo_order_id, consolidated_cargo_id, allocated_cost,
                        failure_preference, superseded, created_at, updated_at)
                    VALUES (?, ?, ?, 'NEXT_TRAIN', FALSE, NOW(), NOW())
                    """, cargoIds.get(i), groupId, BigDecimal.valueOf(costs[i]));
        }
    }

    private void upsertReservation(Long groupId, Long wagonId) {
        jdbcTemplate.update("""
                INSERT INTO reservation (consolidated_cargo_id, wagon_id, total_cost, payment_status, created_at, updated_at)
                VALUES (?, ?, 103200, 'VIRTUAL_PAID', NOW(), NOW())
                ON DUPLICATE KEY UPDATE wagon_id = VALUES(wagon_id), total_cost = VALUES(total_cost),
                    payment_status = 'VIRTUAL_PAID', updated_at = NOW()
                """, groupId, wagonId);
    }

    private void resetReviewParticipations(Long groupId, Long firstCargoId, Long secondCargoId) {
        jdbcTemplate.update("DELETE FROM cargo_participation WHERE cargo_order_id IN (?, ?)", firstCargoId, secondCargoId);
        jdbcTemplate.update("""
                INSERT INTO cargo_participation (cargo_order_id, consolidated_cargo_id, allocated_cost,
                    failure_preference, superseded, created_at, updated_at)
                VALUES (?, ?, 210000, 'NEXT_TRAIN', FALSE, NOW(), NOW()),
                       (?, ?, 210000, 'AUTO_REFUND', FALSE, NOW(), NOW())
                """, firstCargoId, groupId, secondCargoId, groupId);
    }

    private void resetMatchPrediction(Long groupId, Long wagonId) {
        jdbcTemplate.update("DELETE FROM match_prediction WHERE consolidated_cargo_id = ?", groupId);
        jdbcTemplate.update("""
                INSERT INTO match_prediction (consolidated_cargo_id, wagon_id, success_probability,
                    expected_revenue, expected_load_factor, contribution_margin, created_at, updated_at)
                VALUES (?, ?, 94.0, 420000, 97.6, 168000, NOW(), NOW())
                """, groupId, wagonId);
    }

    private void resetNotifications(Long memberId) {
        jdbcTemplate.update("DELETE FROM notification WHERE member_id = ? AND title LIKE '데모:%'", memberId);
        insertNotification(memberId, "APPROVAL", "데모: 운송 확정", "KLG-2608-0071 운송이 확정되었습니다.", false);
        insertNotification(memberId, "MATCH", "데모: 공동화물 매칭", "620kg 모집 완료 · 3개 화주와 함께 운송됩니다.", false);
        insertNotification(memberId, "ANALYSIS", "데모: AI 분석 완료", "8개 항목 추출 완료 · 신뢰도 94%", true);
        insertNotification(memberId, "PAYMENT", "데모: 결제 완료", "103,200원 법인카드 가상 결제가 완료되었습니다.", true);
        insertNotification(memberId, "INFO", "데모: 열차 출발 안내", "KLG-404가 내일 21:30 의왕ICD에서 출발합니다.", true);
    }

    private void insertNotification(Long memberId, String type, String title, String message, boolean read) {
        jdbcTemplate.update("""
                INSERT INTO notification (member_id, type, title, message, is_read, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, NOW(), NOW())
                """, memberId, type, title, message, read);
    }
}
