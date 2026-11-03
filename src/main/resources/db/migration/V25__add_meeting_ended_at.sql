ALTER TABLE meetings
    ADD COLUMN ended_at DATETIME(6) NULL AFTER last_date_poll_reminded_at;

ALTER TABLE meetings
    ADD INDEX idx_meetings_group_ended_at (group_id, ended_at);

ALTER TABLE meetings
    DROP INDEX idx_meetings_group_end_date;
