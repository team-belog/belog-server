CREATE TABLE post_logs
(
    id                BIGINT       NOT NULL AUTO_INCREMENT,
    meeting_id        BIGINT       NOT NULL,
    memory            VARCHAR(80)  NULL,
    ticket_created_at DATETIME(6)  NULL,
    created_at        DATETIME(6)  NOT NULL,
    updated_at        DATETIME(6)  NOT NULL,
    CONSTRAINT pk_post_logs PRIMARY KEY (id),
    CONSTRAINT uk_post_logs_meeting_id UNIQUE (meeting_id),
    CONSTRAINT fk_post_logs_meeting_id
        FOREIGN KEY (meeting_id) REFERENCES meetings (id),
    CONSTRAINT chk_post_logs_memory
        CHECK (memory IS NULL OR CHAR_LENGTH(TRIM(memory)) BETWEEN 1 AND 80),
    CONSTRAINT chk_post_logs_ticket
        CHECK (ticket_created_at IS NULL OR memory IS NOT NULL)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;
