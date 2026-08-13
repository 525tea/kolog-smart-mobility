-- 기획안 보충 "공동화 실패시 대응/보상": 화주가 참여 시점에 미리 선택해두는 처리 방식을 저장한다.
ALTER TABLE cargo_participation
    ADD COLUMN failure_preference VARCHAR(20) NOT NULL DEFAULT 'AUTO_REFUND',
    ADD COLUMN superseded BOOLEAN NOT NULL DEFAULT FALSE;
