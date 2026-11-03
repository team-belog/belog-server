ALTER TABLE bill_log_settlement_requests
    ADD COLUMN last_reminded_at DATETIME(6) NULL AFTER completed_at;
