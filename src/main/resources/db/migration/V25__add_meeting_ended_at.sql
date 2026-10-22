ALTER TABLE meetings
    ADD COLUMN ended_at DATETIME(6) NULL AFTER last_date_poll_reminded_at;
