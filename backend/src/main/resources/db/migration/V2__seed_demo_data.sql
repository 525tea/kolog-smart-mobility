-- 데모 시연 시나리오(기획안 9번) 재현을 위한 샘플 열차/화차 데이터.
-- 기존 적재율 40%, 잔여용량 1,000kg, 전체 적재용량 약 1,666.7kg, 기존 적재량 약 666.7kg, 출발까지 8시간.

INSERT INTO train (train_number, origin_station, destination_station, departure_at, arrival_at,
                    reservation_deadline, status, created_at, updated_at)
VALUES ('KTX-C101', '천안', '서울',
        DATE_ADD(NOW(), INTERVAL 8 HOUR),
        DATE_ADD(NOW(), INTERVAL 9 HOUR),
        DATE_ADD(NOW(), INTERVAL 6 HOUR),
        'SCHEDULED', NOW(), NOW());

INSERT INTO wagon (train_id, wagon_number, wagon_type, max_weight_kg, remaining_weight_kg, hazardous_allowed,
                    created_at, updated_at)
VALUES (LAST_INSERT_ID(), 'W-C101-1', 'CONTAINER', 1666.70, 1000.00, FALSE, NOW(), NOW());

-- 냉장 화물용 화차가 필요한 후속 시나리오를 위해 하나 더 등록해둔다.
INSERT INTO wagon (train_id, wagon_number, wagon_type, max_weight_kg, remaining_weight_kg, hazardous_allowed,
                    created_at, updated_at)
SELECT id, 'W-C101-2', 'REFRIGERATED', 1200.00, 900.00, FALSE, NOW(), NOW()
FROM train
WHERE train_number = 'KTX-C101';
