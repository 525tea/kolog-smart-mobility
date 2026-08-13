-- 공개 해커톤 데모 데이터를 매일 같은 행에 안전하게 갱신하기 위한 내부 식별자.
-- 일반 사용자가 만든 화물/공동화물에는 NULL이며 서비스 기능에는 노출하지 않는다.
ALTER TABLE cargo_order
    ADD COLUMN demo_seed_key VARCHAR(60) NULL,
    ADD CONSTRAINT uk_cargo_order_demo_seed_key UNIQUE (demo_seed_key);

ALTER TABLE consolidated_cargo
    ADD COLUMN demo_seed_key VARCHAR(60) NULL,
    ADD CONSTRAINT uk_consolidated_cargo_demo_seed_key UNIQUE (demo_seed_key);
