CREATE TABLE notification_devices
(
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    user_id          BIGINT       NOT NULL,
    device_id        VARCHAR(255) NOT NULL,
    fcm_token        VARCHAR(255) NOT NULL,
    deactivated_at   DATETIME(6)  NULL,
    active_fcm_token VARCHAR(255)
        GENERATED ALWAYS AS (CASE WHEN deactivated_at IS NULL THEN fcm_token ELSE NULL END) STORED,
    created_at       DATETIME(6)  NOT NULL,
    updated_at       DATETIME(6)  NOT NULL,
    CONSTRAINT pk_notification_devices PRIMARY KEY (id),
    CONSTRAINT uk_notification_devices_user_device UNIQUE (user_id, device_id),
    CONSTRAINT uk_notification_devices_active_fcm_token UNIQUE (active_fcm_token),
    CONSTRAINT fk_notification_devices_user_id
        FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT chk_notification_devices_device_id
        CHECK (CHAR_LENGTH(TRIM(device_id)) BETWEEN 1 AND 255),
    CONSTRAINT chk_notification_devices_fcm_token
        CHECK (CHAR_LENGTH(TRIM(fcm_token)) BETWEEN 1 AND 255),
    INDEX idx_notification_devices_user_deactivated (user_id, deactivated_at)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE notification_outbox
(
    id                    BIGINT       NOT NULL AUTO_INCREMENT,
    notification_id       BIGINT       NOT NULL,
    event_key             VARCHAR(150) NOT NULL,
    status                VARCHAR(20)  NOT NULL,
    attempt_count         INT          NOT NULL DEFAULT 0,
    next_attempt_at       DATETIME(6)  NULL,
    processing_started_at DATETIME(6)  NULL,
    completed_at          DATETIME(6)  NULL,
    last_failure_reason   VARCHAR(500) NULL,
    created_at            DATETIME(6)  NOT NULL,
    updated_at            DATETIME(6)  NOT NULL,
    CONSTRAINT pk_notification_outbox PRIMARY KEY (id),
    CONSTRAINT uk_notification_outbox_notification_id UNIQUE (notification_id),
    CONSTRAINT uk_notification_outbox_event_key UNIQUE (event_key),
    CONSTRAINT fk_notification_outbox_notification_id
        FOREIGN KEY (notification_id) REFERENCES notifications (id),
    CONSTRAINT chk_notification_outbox_event_key
        CHECK (CHAR_LENGTH(TRIM(event_key)) BETWEEN 1 AND 150),
    CONSTRAINT chk_notification_outbox_attempt_count
        CHECK (attempt_count >= 0),
    INDEX idx_notification_outbox_status_next_attempt (status, next_attempt_at, id)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;
