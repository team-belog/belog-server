ALTER TABLE bill_log_settlement_requests
    ADD COLUMN completed_at DATETIME(6) NULL AFTER status,
    ADD CONSTRAINT chk_bill_log_settlement_requests_completion
        CHECK (
            (status = 'PENDING' AND completed_at IS NULL)
            OR (status = 'COMPLETED' AND completed_at IS NOT NULL)
        );
