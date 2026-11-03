ALTER TABLE users
    ADD COLUMN deleted_at DATETIME(6) NULL,
    DROP INDEX uk_users_nickname,
    DROP INDEX uk_users_provider_provider_user_id,
    ADD COLUMN active_nickname VARCHAR(8)
        GENERATED ALWAYS AS (CASE WHEN deleted_at IS NULL THEN nickname ELSE NULL END) STORED,
    ADD COLUMN active_provider VARCHAR(20)
        GENERATED ALWAYS AS (CASE WHEN deleted_at IS NULL THEN provider ELSE NULL END) STORED,
    ADD COLUMN active_provider_user_id VARCHAR(255)
        GENERATED ALWAYS AS (CASE WHEN deleted_at IS NULL THEN provider_user_id ELSE NULL END) STORED,
    ADD CONSTRAINT uk_users_nickname UNIQUE (active_nickname),
    ADD CONSTRAINT uk_users_provider_provider_user_id UNIQUE (active_provider, active_provider_user_id);

ALTER TABLE group_members
    ADD COLUMN withdrawn_at DATETIME(6) NULL AFTER updated_at;
