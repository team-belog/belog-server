ALTER TABLE post_log_photos
    ADD COLUMN meeting_id BIGINT NULL AFTER id;

UPDATE post_log_photos photo
    INNER JOIN post_logs post_log ON post_log.id = photo.post_log_id
SET photo.meeting_id = post_log.meeting_id;

ALTER TABLE post_log_photos
    MODIFY COLUMN meeting_id BIGINT NOT NULL,
    ADD CONSTRAINT fk_post_log_photos_meeting_id
        FOREIGN KEY (meeting_id) REFERENCES meetings (id),
    ADD INDEX idx_post_log_photos_meeting_captured_id (meeting_id, captured_at_utc, id),
    DROP INDEX idx_post_log_photos_post_log_captured_id,
    DROP FOREIGN KEY fk_post_log_photos_post_log_id,
    DROP COLUMN post_log_id;

DROP TABLE post_logs;
