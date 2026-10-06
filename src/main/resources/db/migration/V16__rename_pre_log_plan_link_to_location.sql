ALTER TABLE pre_log_plans
    DROP CHECK chk_pre_log_plans_type_fields;

UPDATE pre_log_plans
SET type = 'LOCATION'
WHERE type = 'LINK';

ALTER TABLE pre_log_plans
    ADD CONSTRAINT chk_pre_log_plans_type_fields CHECK (
        (type = 'LOCATION' AND url IS NOT NULL AND content IS NULL)
        OR (type = 'MEMO' AND url IS NULL AND content IS NOT NULL)
    );
