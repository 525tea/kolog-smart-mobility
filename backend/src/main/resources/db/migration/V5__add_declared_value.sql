-- 화주가 등록 시 신고하는 화물가액. 적재보험료 산정 기준(PricingPolicy.insurancePremium)으로 사용한다.
ALTER TABLE cargo_order
    ADD COLUMN declared_value_krw DECIMAL(12, 2) NULL;
