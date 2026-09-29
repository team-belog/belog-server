CREATE TABLE post_log_photo_likes
(
    id              BIGINT      NOT NULL AUTO_INCREMENT,
    photo_id        BIGINT      NOT NULL,
    group_member_id BIGINT      NOT NULL,
    created_at      DATETIME(6) NOT NULL,
    updated_at      DATETIME(6) NOT NULL,
    CONSTRAINT pk_post_log_photo_likes PRIMARY KEY (id),
    CONSTRAINT uk_post_log_photo_likes_photo_member UNIQUE (photo_id, group_member_id),
    CONSTRAINT fk_post_log_photo_likes_photo_id
        FOREIGN KEY (photo_id) REFERENCES post_log_photos (id),
    CONSTRAINT fk_post_log_photo_likes_group_member_id
        FOREIGN KEY (group_member_id) REFERENCES group_members (id),
    INDEX idx_post_log_photo_likes_group_member_photo (group_member_id, photo_id)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;
