-- 위험물 등급(A~D). hazardous=true인 화물에만 의미가 있다.
ALTER TABLE cargo_order
    ADD COLUMN hazard_grade VARCHAR(10) NULL;
