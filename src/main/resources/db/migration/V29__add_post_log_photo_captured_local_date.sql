ALTER TABLE post_log_photos
    ADD COLUMN captured_local_date DATE NULL AFTER captured_offset_minutes;

UPDATE post_log_photos
SET captured_local_date = DATE(DATE_ADD(captured_at_utc, INTERVAL captured_offset_minutes MINUTE));

ALTER TABLE post_log_photos
    MODIFY COLUMN captured_local_date DATE NOT NULL;

ALTER TABLE post_log_photos
    ADD INDEX idx_post_log_photos_meeting_local_date_captured_id (meeting_id, captured_local_date, captured_at_utc, id);

ALTER TABLE post_log_photos
    DROP INDEX idx_post_log_photos_meeting_captured_id;
