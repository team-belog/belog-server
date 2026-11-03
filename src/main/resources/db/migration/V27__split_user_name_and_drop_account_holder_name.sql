ALTER TABLE users
    DROP CHECK chk_users_onboarding_state;

ALTER TABLE users
    ADD COLUMN last_name  VARCHAR(20) NULL AFTER nickname,
    ADD COLUMN first_name VARCHAR(50) NULL AFTER last_name;

UPDATE users
SET last_name  = LEFT(TRIM(name), 1),
    first_name = CASE
                     WHEN CHAR_LENGTH(TRIM(name)) > 1 THEN SUBSTRING(TRIM(name), 2)
                     ELSE TRIM(name)
        END
WHERE name IS NOT NULL;

ALTER TABLE users
    DROP COLUMN name,
    DROP COLUMN account_holder_name;

ALTER TABLE users
    ADD CONSTRAINT chk_users_onboarding_state CHECK (
        (
            onboarding_completed_at IS NULL
            AND nickname IS NULL
            AND last_name IS NULL
            AND first_name IS NULL
            AND profile_image_object_key IS NULL
            AND bank IS NULL
            AND encrypted_account_number IS NULL
        )
        OR
        (
            onboarding_completed_at IS NOT NULL
            AND nickname IS NOT NULL
            AND last_name IS NOT NULL
            AND first_name IS NOT NULL
            AND bank IS NOT NULL
            AND encrypted_account_number IS NOT NULL
        )
    );
