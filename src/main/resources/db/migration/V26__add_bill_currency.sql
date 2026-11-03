ALTER TABLE bill_log_bills
    ADD COLUMN currency VARCHAR(3) NOT NULL DEFAULT 'KRW' AFTER title,
    MODIFY COLUMN total_amount DECIMAL(19, 2) NOT NULL;

ALTER TABLE bill_log_bills
    ALTER COLUMN currency DROP DEFAULT,
    ADD CONSTRAINT chk_bill_log_bills_currency CHECK (currency IN ('KRW', 'USD', 'JPY', 'EUR'));

ALTER TABLE bill_log_bill_items
    MODIFY COLUMN amount DECIMAL(19, 2) NOT NULL;

ALTER TABLE bill_log_bill_shares
    MODIFY COLUMN amount DECIMAL(19, 2) NOT NULL;

ALTER TABLE bill_log_settlement_requests
    MODIFY COLUMN amount DECIMAL(19, 2) NOT NULL;
