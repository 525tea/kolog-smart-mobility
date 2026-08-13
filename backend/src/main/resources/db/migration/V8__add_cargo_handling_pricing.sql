ALTER TABLE cargo_order
    ADD COLUMN hazard_class_code VARCHAR(10) NULL AFTER hazard_grade,
    ADD COLUMN hazard_class_name VARCHAR(100) NULL AFTER hazard_class_code,
    ADD COLUMN transport_rejected BOOLEAN NOT NULL DEFAULT FALSE AFTER hazard_class_name,
    ADD COLUMN requires_msds BOOLEAN NOT NULL DEFAULT FALSE AFTER transport_rejected,
    ADD COLUMN surcharge_rate DECIMAL(5, 4) NOT NULL DEFAULT 0 AFTER requires_msds,
    ADD COLUMN fixed_power_fee_krw DECIMAL(12, 2) NOT NULL DEFAULT 0 AFTER surcharge_rate,
    ADD COLUMN detected_temperature_c DECIMAL(6, 2) NULL AFTER fixed_power_fee_krw;
