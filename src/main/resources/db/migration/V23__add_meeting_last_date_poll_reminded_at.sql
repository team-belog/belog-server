ALTER TABLE meetings
    ADD COLUMN last_date_poll_reminded_at DATETIME(6) NULL AFTER confirmed_at;
