package com.smbility.railcargo.train.service;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** 해커톤 데모 열차가 배포 몇 시간 후 모두 과거가 되지 않도록 시간만 새로 맞춘다. */
@Component
@Order(10)
@ConditionalOnProperty(prefix = "app.demo", name = "schedule-refresh-enabled", havingValue = "true")
public class DemoScheduleRefreshService implements ApplicationRunner {

    private static final Map<String, DeparturePlan> DEPARTURE_PLANS = Map.of(
            "KLG-404", new DeparturePlan(1, LocalTime.of(21, 30), 8),
            "KLG-512", new DeparturePlan(1, LocalTime.of(23, 10), 8),
            "KLG-338", new DeparturePlan(2, LocalTime.of(1, 0), 8),
            "KLG-711", new DeparturePlan(2, LocalTime.of(3, 40), 2),
            "KLG-219", new DeparturePlan(2, LocalTime.of(5, 20), 5));

    private final JdbcTemplate jdbcTemplate;

    public DemoScheduleRefreshService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        refresh();
    }

    @Scheduled(cron = "0 5 0 * * *")
    @Transactional
    public void refresh() {
        DEPARTURE_PLANS.forEach((trainNumber, plan) -> {
            LocalDateTime departure = LocalDate.now().plusDays(plan.dayOffset()).atTime(plan.time());
            LocalDateTime arrival = departure.plusHours(plan.durationHours());
            LocalDateTime deadline = departure.minusHours(2);
            jdbcTemplate.update("""
                UPDATE train
                   SET departure_at = ?, arrival_at = ?, reservation_deadline = ?,
                       status = 'SCHEDULED', updated_at = NOW()
                 WHERE train_number = ?
                """, Timestamp.valueOf(departure), Timestamp.valueOf(arrival), Timestamp.valueOf(deadline), trainNumber);
        });
    }

    private record DeparturePlan(int dayOffset, LocalTime time, int durationHours) {
    }
}
