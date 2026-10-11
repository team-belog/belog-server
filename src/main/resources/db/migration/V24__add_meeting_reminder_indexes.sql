ALTER TABLE meetings
    ADD INDEX idx_meetings_start_date_id (start_date, id),
    ADD INDEX idx_meetings_end_date_id (end_date, id),
    ADD INDEX idx_meetings_group_end_date (group_id, end_date);
