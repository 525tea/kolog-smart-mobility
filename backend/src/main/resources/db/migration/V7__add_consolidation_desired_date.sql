ALTER TABLE consolidated_cargo
    ADD COLUMN desired_date DATE NULL AFTER recruitment_deadline;

UPDATE consolidated_cargo
SET desired_date = DATE(recruitment_deadline)
WHERE desired_date IS NULL;

ALTER TABLE consolidated_cargo
    MODIFY COLUMN desired_date DATE NOT NULL;

CREATE INDEX idx_consolidated_cargo_desired_date
    ON consolidated_cargo (origin_station, destination_station, desired_date, status);
