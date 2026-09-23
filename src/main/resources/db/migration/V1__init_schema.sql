CREATE TABLE users
(
    id                       BIGINT       NOT NULL AUTO_INCREMENT,
    email                    VARCHAR(320) NOT NULL,
    provider                 VARCHAR(20)  NOT NULL,
    provider_user_id         VARCHAR(255) NOT NULL,
    nickname                 VARCHAR(8)   NULL,
    name                     VARCHAR(50)  NULL,
    profile_image_object_key VARCHAR(1024) NULL,
    social_profile_image_url VARCHAR(2048) NULL,
    bank                     VARCHAR(30)  NULL,
    encrypted_account_number VARCHAR(128) NULL,
    account_holder_name      VARCHAR(50)  NULL,
    onboarding_completed_at  DATETIME(6)  NULL,
    created_at               DATETIME(6)  NOT NULL,
    updated_at               DATETIME(6)  NOT NULL,
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uk_users_provider_provider_user_id UNIQUE (provider, provider_user_id),
    CONSTRAINT uk_users_nickname UNIQUE (nickname),
    CONSTRAINT chk_users_onboarding_state CHECK (
        (
            onboarding_completed_at IS NULL
            AND nickname IS NULL
            AND name IS NULL
            AND profile_image_object_key IS NULL
            AND bank IS NULL
            AND encrypted_account_number IS NULL
            AND account_holder_name IS NULL
        )
        OR
        (
            onboarding_completed_at IS NOT NULL
            AND nickname IS NOT NULL
            AND name IS NOT NULL
            AND bank IS NOT NULL
            AND encrypted_account_number IS NOT NULL
            AND account_holder_name IS NOT NULL
        )
    )
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE refresh_tokens
(
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    user_id    BIGINT      NOT NULL,
    token_hash VARCHAR(64) NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_refresh_tokens PRIMARY KEY (id),
    CONSTRAINT uk_refresh_tokens_user_id UNIQUE (user_id),
    CONSTRAINT fk_refresh_tokens_user_id
        FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE belog_groups
(
    id                     BIGINT        NOT NULL AUTO_INCREMENT,
    name                   VARCHAR(20)   NOT NULL,
    cover_image_object_key VARCHAR(1024) NULL,
    invite_code            VARCHAR(6)    NOT NULL,
    created_at             DATETIME(6)   NOT NULL,
    updated_at             DATETIME(6)   NOT NULL,
    CONSTRAINT pk_belog_groups PRIMARY KEY (id),
    CONSTRAINT uk_groups_invite_code UNIQUE (invite_code),
    CONSTRAINT chk_groups_name CHECK (CHAR_LENGTH(TRIM(name)) BETWEEN 1 AND 20),
    CONSTRAINT chk_groups_invite_code_length CHECK (CHAR_LENGTH(invite_code) = 6)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE group_members
(
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    group_id   BIGINT      NOT NULL,
    user_id    BIGINT      NOT NULL,
    role       VARCHAR(20) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_group_members PRIMARY KEY (id),
    CONSTRAINT uk_group_members_group_user UNIQUE (group_id, user_id),
    CONSTRAINT fk_group_members_group_id
        FOREIGN KEY (group_id) REFERENCES belog_groups (id),
    CONSTRAINT fk_group_members_user_id
        FOREIGN KEY (user_id) REFERENCES users (id),
    INDEX idx_group_members_user_id (user_id)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE meetings
(
    id                         BIGINT      NOT NULL AUTO_INCREMENT,
    group_id                   BIGINT      NOT NULL,
    created_by_group_member_id BIGINT      NOT NULL,
    name                       VARCHAR(15) NOT NULL,
    location                   VARCHAR(20) NULL,
    schedule_type              VARCHAR(20) NOT NULL,
    status                     VARCHAR(20) NOT NULL,
    start_date                 DATE        NULL,
    end_date                   DATE        NULL,
    confirmed_at               DATETIME(6) NULL,
    created_at                 DATETIME(6) NOT NULL,
    updated_at                 DATETIME(6) NOT NULL,
    CONSTRAINT pk_meetings PRIMARY KEY (id),
    CONSTRAINT fk_meetings_group_id
        FOREIGN KEY (group_id) REFERENCES belog_groups (id),
    CONSTRAINT fk_meetings_created_by_group_member_id
        FOREIGN KEY (created_by_group_member_id) REFERENCES group_members (id),
    CONSTRAINT chk_meetings_name CHECK (CHAR_LENGTH(TRIM(name)) BETWEEN 1 AND 15),
    CONSTRAINT chk_meetings_location CHECK (
        location IS NULL OR CHAR_LENGTH(TRIM(location)) BETWEEN 1 AND 20
    ),
    CONSTRAINT chk_meetings_schedule_state CHECK (
        (
            schedule_type = 'FIXED'
            AND status = 'CONFIRMED'
            AND start_date IS NOT NULL
            AND end_date IS NOT NULL
            AND confirmed_at IS NOT NULL
        )
        OR
        (
            schedule_type = 'POLL'
            AND
            (
                (
                    status = 'SCHEDULING'
                    AND start_date IS NULL
                    AND end_date IS NULL
                    AND confirmed_at IS NULL
                )
                OR
                (
                    status = 'CONFIRMED'
                    AND start_date IS NOT NULL
                    AND end_date IS NOT NULL
                    AND confirmed_at IS NOT NULL
                )
            )
        )
    ),
    CONSTRAINT chk_meetings_date_range CHECK (start_date IS NULL OR end_date >= start_date)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE meeting_participants
(
    id              BIGINT      NOT NULL AUTO_INCREMENT,
    meeting_id      BIGINT      NOT NULL,
    group_member_id BIGINT      NOT NULL,
    created_at      DATETIME(6) NOT NULL,
    updated_at      DATETIME(6) NOT NULL,
    CONSTRAINT pk_meeting_participants PRIMARY KEY (id),
    CONSTRAINT uk_meeting_participants_meeting_member UNIQUE (meeting_id, group_member_id),
    CONSTRAINT fk_meeting_participants_meeting_id
        FOREIGN KEY (meeting_id) REFERENCES meetings (id),
    CONSTRAINT fk_meeting_participants_group_member_id
        FOREIGN KEY (group_member_id) REFERENCES group_members (id),
    INDEX idx_meeting_participants_group_member_id_meeting_id (group_member_id, meeting_id)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE meeting_candidate_date_ranges
(
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    meeting_id BIGINT      NOT NULL,
    start_date DATE        NOT NULL,
    end_date   DATE        NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_meeting_candidate_date_ranges PRIMARY KEY (id),
    CONSTRAINT uk_meeting_candidate_date_ranges_meeting_dates UNIQUE (meeting_id, start_date, end_date),
    CONSTRAINT fk_meeting_candidate_date_ranges_meeting_id
        FOREIGN KEY (meeting_id) REFERENCES meetings (id),
    CONSTRAINT chk_meeting_candidate_date_ranges_date_range CHECK (end_date >= start_date)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE meeting_schedule_responses
(
    id                     BIGINT      NOT NULL AUTO_INCREMENT,
    meeting_id             BIGINT      NOT NULL,
    meeting_participant_id BIGINT      NOT NULL,
    responded_at           DATETIME(6) NOT NULL,
    created_at             DATETIME(6) NOT NULL,
    updated_at             DATETIME(6) NOT NULL,
    CONSTRAINT pk_meeting_schedule_responses PRIMARY KEY (id),
    CONSTRAINT uk_meeting_schedule_responses_meeting_participant UNIQUE (meeting_id, meeting_participant_id),
    CONSTRAINT fk_meeting_schedule_responses_meeting_id
        FOREIGN KEY (meeting_id) REFERENCES meetings (id),
    CONSTRAINT fk_meeting_schedule_responses_participant_id
        FOREIGN KEY (meeting_participant_id) REFERENCES meeting_participants (id)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE meeting_available_dates
(
    id                              BIGINT      NOT NULL AUTO_INCREMENT,
    meeting_schedule_response_id    BIGINT      NOT NULL,
    meeting_candidate_date_range_id BIGINT      NOT NULL,
    created_at                      DATETIME(6) NOT NULL,
    updated_at                      DATETIME(6) NOT NULL,
    CONSTRAINT pk_meeting_available_dates PRIMARY KEY (id),
    CONSTRAINT uk_meeting_available_dates_response_candidate UNIQUE (
        meeting_schedule_response_id,
        meeting_candidate_date_range_id
    ),
    CONSTRAINT fk_meeting_available_dates_response_id
        FOREIGN KEY (meeting_schedule_response_id) REFERENCES meeting_schedule_responses (id),
    CONSTRAINT fk_meeting_available_dates_candidate_id
        FOREIGN KEY (meeting_candidate_date_range_id) REFERENCES meeting_candidate_date_ranges (id)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE pre_log_plans
(
    id                         BIGINT        NOT NULL AUTO_INCREMENT,
    meeting_id                 BIGINT        NOT NULL,
    created_by_group_member_id BIGINT        NOT NULL,
    type                       VARCHAR(20)   NOT NULL,
    category                   VARCHAR(20)   NOT NULL,
    title                      VARCHAR(100)  NOT NULL,
    url                        VARCHAR(2048) NULL,
    content                    VARCHAR(2000) NULL,
    created_at                 DATETIME(6)   NOT NULL,
    updated_at                 DATETIME(6)   NOT NULL,
    CONSTRAINT pk_pre_log_plans PRIMARY KEY (id),
    CONSTRAINT fk_pre_log_plans_meeting_id
        FOREIGN KEY (meeting_id) REFERENCES meetings (id),
    CONSTRAINT fk_pre_log_plans_created_by_group_member_id
        FOREIGN KEY (created_by_group_member_id) REFERENCES group_members (id),
    CONSTRAINT chk_pre_log_plans_title CHECK (CHAR_LENGTH(TRIM(title)) BETWEEN 1 AND 100),
    CONSTRAINT chk_pre_log_plans_type_fields CHECK (
        (type = 'LINK' AND url IS NOT NULL AND content IS NULL)
        OR (type = 'MEMO' AND url IS NULL AND content IS NOT NULL)
    ),
    CONSTRAINT chk_pre_log_plans_url CHECK (
        url IS NULL OR CHAR_LENGTH(TRIM(url)) BETWEEN 1 AND 2048
    ),
    CONSTRAINT chk_pre_log_plans_content CHECK (
        content IS NULL OR CHAR_LENGTH(TRIM(content)) BETWEEN 1 AND 2000
    )
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;
