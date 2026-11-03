ALTER TABLE users
    ADD COLUMN push_notification_enabled BOOLEAN NOT NULL DEFAULT TRUE AFTER onboarding_completed_at;

CREATE TABLE notifications
(
    id                BIGINT       NOT NULL AUTO_INCREMENT,
    recipient_user_id BIGINT       NOT NULL,
    actor_user_id     BIGINT       NULL,
    type              VARCHAR(50)  NOT NULL,
    message           VARCHAR(200) NOT NULL,
    target_id         BIGINT       NULL,
    deduplication_key VARCHAR(150) NOT NULL,
    read_at           DATETIME(6)  NULL,
    created_at        DATETIME(6)  NOT NULL,
    updated_at        DATETIME(6)  NOT NULL,
    CONSTRAINT pk_notifications PRIMARY KEY (id),
    CONSTRAINT uk_notifications_deduplication_key UNIQUE (deduplication_key),
    CONSTRAINT fk_notifications_recipient_user_id
        FOREIGN KEY (recipient_user_id) REFERENCES users (id),
    CONSTRAINT fk_notifications_actor_user_id
        FOREIGN KEY (actor_user_id) REFERENCES users (id),
    CONSTRAINT chk_notifications_message
        CHECK (CHAR_LENGTH(TRIM(message)) BETWEEN 1 AND 200),
    CONSTRAINT chk_notifications_deduplication_key
        CHECK (CHAR_LENGTH(TRIM(deduplication_key)) BETWEEN 1 AND 150),
    CONSTRAINT chk_notifications_target
        CHECK (
            (type = 'GROUP_DELETED' AND target_id IS NULL)
            OR (type <> 'GROUP_DELETED' AND target_id IS NOT NULL AND target_id > 0)
        ),
    INDEX idx_notifications_recipient_id (recipient_user_id, id),
    INDEX idx_notifications_recipient_read_at (recipient_user_id, read_at)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;
