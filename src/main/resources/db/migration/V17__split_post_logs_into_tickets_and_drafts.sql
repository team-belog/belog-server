CREATE TABLE post_log_tickets
(
    id                      BIGINT       NOT NULL AUTO_INCREMENT,
    owner_user_id           BIGINT       NOT NULL,
    source_meeting_id       BIGINT       NOT NULL,
    creator_group_member_id BIGINT       NOT NULL,
    creator_nickname        VARCHAR(8)   NOT NULL,
    meeting_name            VARCHAR(15)  NOT NULL,
    meeting_location        VARCHAR(20)  NULL,
    meeting_start_date      DATE         NULL,
    meeting_end_date        DATE         NULL,
    memory                  VARCHAR(80)  NOT NULL,
    cover_image_object_key  VARCHAR(512) NULL,
    issued_at               DATETIME(6)  NOT NULL,
    created_at              DATETIME(6)  NOT NULL,
    updated_at              DATETIME(6)  NOT NULL,
    CONSTRAINT pk_post_log_tickets PRIMARY KEY (id),
    CONSTRAINT uk_post_log_tickets_meeting_creator UNIQUE (source_meeting_id, creator_group_member_id),
    CONSTRAINT fk_post_log_tickets_owner_user_id
        FOREIGN KEY (owner_user_id) REFERENCES users (id),
    CONSTRAINT fk_post_log_tickets_source_meeting_id
        FOREIGN KEY (source_meeting_id) REFERENCES meetings (id),
    CONSTRAINT fk_post_log_tickets_creator_group_member_id
        FOREIGN KEY (creator_group_member_id) REFERENCES group_members (id),
    CONSTRAINT chk_post_log_tickets_memory
        CHECK (CHAR_LENGTH(TRIM(memory)) BETWEEN 1 AND 80),
    CONSTRAINT chk_post_log_tickets_meeting_name
        CHECK (CHAR_LENGTH(TRIM(meeting_name)) BETWEEN 1 AND 15),
    INDEX idx_post_log_tickets_owner_end_date (owner_user_id, meeting_end_date, id)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

INSERT INTO post_log_tickets (id,
                              owner_user_id,
                              source_meeting_id,
                              creator_group_member_id,
                              creator_nickname,
                              meeting_name,
                              meeting_location,
                              meeting_start_date,
                              meeting_end_date,
                              memory,
                              cover_image_object_key,
                              issued_at,
                              created_at,
                              updated_at)
SELECT post_log.id,
       creator.user_id,
       post_log.meeting_id,
       post_log.created_by_group_member_id,
       creator_user.nickname,
       meeting.name,
       meeting.location,
       meeting.start_date,
       meeting.end_date,
       post_log.memory,
       (SELECT photo.object_key
        FROM post_log_photos photo
                 LEFT JOIN post_log_photo_likes photo_like ON photo_like.photo_id = photo.id
        WHERE photo.meeting_id = post_log.meeting_id
        GROUP BY photo.id, photo.object_key
        ORDER BY COUNT(photo_like.id) DESC, photo.id ASC
        LIMIT 1),
       post_log.ticket_created_at,
       post_log.ticket_created_at,
       post_log.ticket_created_at
FROM post_logs post_log
         INNER JOIN meetings meeting ON meeting.id = post_log.meeting_id
         INNER JOIN group_members creator ON creator.id = post_log.created_by_group_member_id
         INNER JOIN users creator_user ON creator_user.id = creator.user_id
WHERE post_log.ticket_created_at IS NOT NULL;

DELETE
FROM post_logs
WHERE ticket_created_at IS NOT NULL
   OR memory IS NULL;

ALTER TABLE post_logs
    DROP CHECK chk_post_logs_ticket,
    DROP CHECK chk_post_logs_memory;

ALTER TABLE post_logs
    DROP FOREIGN KEY fk_post_logs_meeting_id,
    DROP FOREIGN KEY fk_post_logs_created_by_group_member_id;

ALTER TABLE post_logs
    DROP COLUMN ticket_created_at,
    MODIFY COLUMN memory VARCHAR(80) NOT NULL;

RENAME TABLE post_logs TO post_log_drafts;

ALTER TABLE post_log_drafts
    RENAME INDEX uk_post_logs_meeting_member TO uk_post_log_drafts_meeting_member,
    RENAME INDEX fk_post_logs_created_by_group_member_id TO fk_post_log_drafts_created_by_group_member_id;

ALTER TABLE post_log_drafts
    ADD CONSTRAINT chk_post_log_drafts_memory
        CHECK (CHAR_LENGTH(TRIM(memory)) BETWEEN 1 AND 80),
    ADD CONSTRAINT fk_post_log_drafts_meeting_id
        FOREIGN KEY (meeting_id) REFERENCES meetings (id),
    ADD CONSTRAINT fk_post_log_drafts_created_by_group_member_id
        FOREIGN KEY (created_by_group_member_id) REFERENCES group_members (id);
