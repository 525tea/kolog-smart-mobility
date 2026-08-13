-- Figma 시연 노선을 실제 열차/화차 데이터로 제공한다. 시간은 DemoScheduleRefreshService가 매일 갱신한다.
INSERT INTO train (train_number, origin_station, destination_station, departure_at, arrival_at,
                   reservation_deadline, status, created_at, updated_at)
VALUES ('KLG-404', '의왕ICD', '부산진역', DATE_ADD(NOW(), INTERVAL 8 HOUR), DATE_ADD(NOW(), INTERVAL 16 HOUR), DATE_ADD(NOW(), INTERVAL 6 HOUR), 'SCHEDULED', NOW(), NOW()),
       ('KLG-512', '의왕ICD', '부산진역', DATE_ADD(NOW(), INTERVAL 10 HOUR), DATE_ADD(NOW(), INTERVAL 18 HOUR), DATE_ADD(NOW(), INTERVAL 8 HOUR), 'SCHEDULED', NOW(), NOW()),
       ('KLG-338', '오봉역', '부산진역', DATE_ADD(NOW(), INTERVAL 11 HOUR), DATE_ADD(NOW(), INTERVAL 19 HOUR), DATE_ADD(NOW(), INTERVAL 9 HOUR), 'SCHEDULED', NOW(), NOW()),
       ('KLG-711', '의왕ICD', '동인천역', DATE_ADD(NOW(), INTERVAL 13 HOUR), DATE_ADD(NOW(), INTERVAL 15 HOUR), DATE_ADD(NOW(), INTERVAL 11 HOUR), 'SCHEDULED', NOW(), NOW()),
       ('KLG-219', '오봉역', '동대구역', DATE_ADD(NOW(), INTERVAL 15 HOUR), DATE_ADD(NOW(), INTERVAL 20 HOUR), DATE_ADD(NOW(), INTERVAL 13 HOUR), 'SCHEDULED', NOW(), NOW());

INSERT INTO wagon (train_id, wagon_number, wagon_type, max_weight_kg, remaining_weight_kg, hazardous_allowed,
                   created_at, updated_at)
SELECT id, CONCAT('W-', train_number, '-C'), 'CONTAINER', 820.00, 820.00, TRUE, NOW(), NOW()
FROM train WHERE train_number IN ('KLG-404', 'KLG-512', 'KLG-338', 'KLG-711', 'KLG-219');

INSERT INTO wagon (train_id, wagon_number, wagon_type, max_weight_kg, remaining_weight_kg, hazardous_allowed,
                   created_at, updated_at)
SELECT id, CONCAT('W-', train_number, '-R'), 'REFRIGERATED', 820.00, 820.00, FALSE, NOW(), NOW()
FROM train WHERE train_number IN ('KLG-404', 'KLG-512');
