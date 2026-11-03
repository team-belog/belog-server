CREATE TABLE pre_log_plan_likes
(
    id              BIGINT      NOT NULL AUTO_INCREMENT,
    plan_id         BIGINT      NOT NULL,
    group_member_id BIGINT      NOT NULL,
    created_at      DATETIME(6) NOT NULL,
    updated_at      DATETIME(6) NOT NULL,
    CONSTRAINT pk_pre_log_plan_likes PRIMARY KEY (id),
    CONSTRAINT uk_pre_log_plan_likes_plan_member UNIQUE (plan_id, group_member_id),
    CONSTRAINT fk_pre_log_plan_likes_plan_id
        FOREIGN KEY (plan_id) REFERENCES pre_log_plans (id),
    CONSTRAINT fk_pre_log_plan_likes_group_member_id
        FOREIGN KEY (group_member_id) REFERENCES group_members (id),
    INDEX idx_pre_log_plan_likes_group_member_id_plan_id (group_member_id, plan_id)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;
