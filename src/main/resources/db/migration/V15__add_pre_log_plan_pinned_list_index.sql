ALTER TABLE pre_log_plans
    ADD INDEX idx_pre_log_plans_meeting_pinned_id (meeting_id, pinned, id);
