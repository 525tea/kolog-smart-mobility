-- AI 철도 공동화물 거래소 - 초기 스키마
-- docs/architecture/overview.md 의 ERD 참고

CREATE TABLE member (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    email         VARCHAR(100) NOT NULL,
    password      VARCHAR(255) NOT NULL,
    role          VARCHAR(20)  NOT NULL,
    created_at    DATETIME(6)  NOT NULL,
    updated_at    DATETIME(6)  NOT NULL,
    CONSTRAINT uk_member_email UNIQUE (email)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE shipper (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id       BIGINT       NOT NULL,
    business_number VARCHAR(20)  NOT NULL,
    company_name    VARCHAR(100) NOT NULL,
    manager_name    VARCHAR(50)  NOT NULL,
    phone           VARCHAR(30)  NOT NULL,
    created_at      DATETIME(6)  NOT NULL,
    updated_at      DATETIME(6)  NOT NULL,
    CONSTRAINT uk_shipper_member_id UNIQUE (member_id),
    CONSTRAINT fk_shipper_member FOREIGN KEY (member_id) REFERENCES member (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE train (
    id                    BIGINT AUTO_INCREMENT PRIMARY KEY,
    train_number          VARCHAR(20)  NOT NULL,
    origin_station        VARCHAR(50)  NOT NULL,
    destination_station   VARCHAR(50)  NOT NULL,
    departure_at          DATETIME(6)  NOT NULL,
    arrival_at            DATETIME(6)  NOT NULL,
    reservation_deadline  DATETIME(6)  NOT NULL,
    status                VARCHAR(20)  NOT NULL,
    created_at            DATETIME(6)  NOT NULL,
    updated_at            DATETIME(6)  NOT NULL,
    INDEX idx_train_route (origin_station, destination_station, status)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE wagon (
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    train_id             BIGINT         NOT NULL,
    wagon_number         VARCHAR(20)    NOT NULL,
    wagon_type           VARCHAR(20)    NOT NULL,
    max_weight_kg        DECIMAL(10, 2) NOT NULL,
    remaining_weight_kg  DECIMAL(10, 2) NOT NULL,
    hazardous_allowed    BOOLEAN        NOT NULL DEFAULT FALSE,
    created_at           DATETIME(6)    NOT NULL,
    updated_at           DATETIME(6)    NOT NULL,
    CONSTRAINT fk_wagon_train FOREIGN KEY (train_id) REFERENCES train (id),
    INDEX idx_wagon_remaining (remaining_weight_kg)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE cargo_order (
    id                     BIGINT AUTO_INCREMENT PRIMARY KEY,
    shipper_id             BIGINT         NOT NULL,
    cargo_name             VARCHAR(100)   NOT NULL,
    raw_input              TEXT,
    origin_station         VARCHAR(50)    NOT NULL,
    destination_station    VARCHAR(50)    NOT NULL,
    desired_date           DATE           NOT NULL,
    weight_kg              DECIMAL(10, 2),
    volume_cbm             DECIMAL(10, 3),
    temperature_condition  VARCHAR(20),
    hazardous              BOOLEAN        NOT NULL DEFAULT FALSE,
    packaging_type         VARCHAR(30),
    handling_note          VARCHAR(200),
    status                 VARCHAR(20)    NOT NULL,
    created_at             DATETIME(6)    NOT NULL,
    updated_at             DATETIME(6)    NOT NULL,
    CONSTRAINT fk_cargo_order_shipper FOREIGN KEY (shipper_id) REFERENCES shipper (id),
    INDEX idx_cargo_order_shipper_status (shipper_id, status)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE consolidated_cargo (
    id                     BIGINT AUTO_INCREMENT PRIMARY KEY,
    origin_station         VARCHAR(50)    NOT NULL,
    destination_station    VARCHAR(50)    NOT NULL,
    temperature_condition  VARCHAR(20)    NOT NULL,
    hazardous              BOOLEAN        NOT NULL DEFAULT FALSE,
    target_weight_kg       DECIMAL(10, 2) NOT NULL,
    recruited_weight_kg    DECIMAL(10, 2) NOT NULL DEFAULT 0,
    recruitment_deadline   DATETIME(6)    NOT NULL,
    matched_wagon_id       BIGINT,
    status                 VARCHAR(20)    NOT NULL,
    created_at             DATETIME(6)    NOT NULL,
    updated_at             DATETIME(6)    NOT NULL,
    CONSTRAINT fk_consolidated_cargo_wagon FOREIGN KEY (matched_wagon_id) REFERENCES wagon (id),
    INDEX idx_consolidated_cargo_match (origin_station, destination_station, temperature_condition, hazardous, status)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE cargo_participation (
    id                      BIGINT AUTO_INCREMENT PRIMARY KEY,
    cargo_order_id          BIGINT         NOT NULL,
    consolidated_cargo_id   BIGINT         NOT NULL,
    allocated_cost          DECIMAL(12, 2) NOT NULL,
    created_at              DATETIME(6)    NOT NULL,
    updated_at              DATETIME(6)    NOT NULL,
    CONSTRAINT fk_participation_cargo_order FOREIGN KEY (cargo_order_id) REFERENCES cargo_order (id),
    CONSTRAINT fk_participation_consolidated_cargo FOREIGN KEY (consolidated_cargo_id) REFERENCES consolidated_cargo (id),
    INDEX idx_participation_consolidated_cargo (consolidated_cargo_id),
    INDEX idx_participation_cargo_order (cargo_order_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE match_prediction (
    id                     BIGINT AUTO_INCREMENT PRIMARY KEY,
    consolidated_cargo_id  BIGINT         NOT NULL,
    wagon_id               BIGINT         NOT NULL,
    success_probability    DECIMAL(5, 2)  NOT NULL,
    expected_revenue       DECIMAL(12, 2) NOT NULL,
    expected_load_factor   DECIMAL(5, 2)  NOT NULL,
    contribution_margin    DECIMAL(12, 2) NOT NULL,
    created_at             DATETIME(6)    NOT NULL,
    updated_at             DATETIME(6)    NOT NULL,
    CONSTRAINT fk_match_prediction_consolidated_cargo FOREIGN KEY (consolidated_cargo_id) REFERENCES consolidated_cargo (id),
    CONSTRAINT fk_match_prediction_wagon FOREIGN KEY (wagon_id) REFERENCES wagon (id),
    INDEX idx_match_prediction_consolidated_cargo (consolidated_cargo_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE approval_decision (
    id                     BIGINT AUTO_INCREMENT PRIMARY KEY,
    consolidated_cargo_id  BIGINT       NOT NULL,
    operator_id            BIGINT       NOT NULL,
    status                 VARCHAR(20)  NOT NULL,
    memo                   VARCHAR(500),
    created_at             DATETIME(6)  NOT NULL,
    updated_at             DATETIME(6)  NOT NULL,
    CONSTRAINT fk_approval_consolidated_cargo FOREIGN KEY (consolidated_cargo_id) REFERENCES consolidated_cargo (id),
    CONSTRAINT fk_approval_operator FOREIGN KEY (operator_id) REFERENCES member (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE reservation (
    id                     BIGINT AUTO_INCREMENT PRIMARY KEY,
    consolidated_cargo_id  BIGINT         NOT NULL,
    wagon_id               BIGINT         NOT NULL,
    total_cost             DECIMAL(12, 2) NOT NULL,
    payment_status         VARCHAR(20)    NOT NULL,
    created_at             DATETIME(6)    NOT NULL,
    updated_at             DATETIME(6)    NOT NULL,
    CONSTRAINT uk_reservation_consolidated_cargo UNIQUE (consolidated_cargo_id),
    CONSTRAINT fk_reservation_consolidated_cargo FOREIGN KEY (consolidated_cargo_id) REFERENCES consolidated_cargo (id),
    CONSTRAINT fk_reservation_wagon FOREIGN KEY (wagon_id) REFERENCES wagon (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE performance_report (
    id                    BIGINT AUTO_INCREMENT PRIMARY KEY,
    train_id              BIGINT         NOT NULL,
    wagon_id              BIGINT         NOT NULL,
    before_load_factor    DECIMAL(5, 2)  NOT NULL,
    after_load_factor     DECIMAL(5, 2)  NOT NULL,
    new_revenue           DECIMAL(12, 2) NOT NULL,
    cost_savings          DECIMAL(12, 2) NOT NULL,
    carbon_reduction_kg   DECIMAL(10, 2) NOT NULL,
    created_at            DATETIME(6)    NOT NULL,
    updated_at            DATETIME(6)    NOT NULL,
    CONSTRAINT fk_performance_report_train FOREIGN KEY (train_id) REFERENCES train (id),
    CONSTRAINT fk_performance_report_wagon FOREIGN KEY (wagon_id) REFERENCES wagon (id),
    INDEX idx_performance_report_train (train_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;
