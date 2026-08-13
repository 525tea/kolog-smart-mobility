ALTER TABLE cargo_order
    ADD COLUMN service_mode VARCHAR(20) NOT NULL DEFAULT 'CO_LOAD' AFTER desired_date;
