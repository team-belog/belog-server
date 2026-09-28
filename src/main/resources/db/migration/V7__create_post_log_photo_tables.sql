CREATE TABLE post_logs
(
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    meeting_id BIGINT      NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_post_logs PRIMARY KEY (id),
    CONSTRAINT uk_post_logs_meeting_id UNIQUE (meeting_id),
    CONSTRAINT fk_post_logs_meeting_id
        FOREIGN KEY (meeting_id) REFERENCES meetings (id)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE post_log_photos
(
    id                          BIGINT       NOT NULL AUTO_INCREMENT,
    post_log_id                 BIGINT       NOT NULL,
    uploaded_by_group_member_id BIGINT       NOT NULL,
    object_key                  VARCHAR(512) NOT NULL,
    captured_at_utc             DATETIME(6)  NOT NULL,
    captured_offset_minutes     INTEGER      NOT NULL,
    created_at                  DATETIME(6)  NOT NULL,
    updated_at                  DATETIME(6)  NOT NULL,
    CONSTRAINT pk_post_log_photos PRIMARY KEY (id),
    CONSTRAINT uk_post_log_photos_object_key UNIQUE (object_key),
    CONSTRAINT fk_post_log_photos_post_log_id
        FOREIGN KEY (post_log_id) REFERENCES post_logs (id),
    CONSTRAINT fk_post_log_photos_uploaded_by_group_member_id
        FOREIGN KEY (uploaded_by_group_member_id) REFERENCES group_members (id),
    CONSTRAINT chk_post_log_photos_captured_offset
        CHECK (captured_offset_minutes BETWEEN -1080 AND 1080),
    INDEX idx_post_log_photos_post_log_captured_id (post_log_id, captured_at_utc, id)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;
