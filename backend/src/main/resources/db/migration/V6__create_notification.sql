-- 서버 알림. 프론트엔드의 클라이언트 로컬 활동 로그를 대체하는 실제 저장 알림.
CREATE TABLE notification (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id  BIGINT       NOT NULL,
    type       VARCHAR(20)  NOT NULL,
    title      VARCHAR(100) NOT NULL,
    message    VARCHAR(500) NOT NULL,
    is_read    BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at DATETIME(6)  NOT NULL,
    updated_at DATETIME(6)  NOT NULL,
    CONSTRAINT fk_notification_member FOREIGN KEY (member_id) REFERENCES member (id)
);

CREATE INDEX idx_notification_member_id ON notification (member_id);
