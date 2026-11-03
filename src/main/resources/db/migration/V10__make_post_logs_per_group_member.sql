ALTER TABLE post_logs
    ADD COLUMN created_by_group_member_id BIGINT NULL AFTER meeting_id;

UPDATE post_logs post_log
    INNER JOIN meetings meeting ON meeting.id = post_log.meeting_id
SET post_log.created_by_group_member_id = meeting.created_by_group_member_id;

ALTER TABLE post_logs
    MODIFY COLUMN created_by_group_member_id BIGINT NOT NULL,
    ADD CONSTRAINT uk_post_logs_meeting_member UNIQUE (meeting_id, created_by_group_member_id),
    ADD CONSTRAINT fk_post_logs_created_by_group_member_id
        FOREIGN KEY (created_by_group_member_id) REFERENCES group_members (id);

ALTER TABLE post_logs
    DROP INDEX uk_post_logs_meeting_id;
