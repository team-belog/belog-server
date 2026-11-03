ALTER TABLE users
    ADD COLUMN profile_image_source VARCHAR(20) NOT NULL;

ALTER TABLE users
    ADD CONSTRAINT chk_users_profile_image_state CHECK (
        (profile_image_source = 'CUSTOM' AND profile_image_object_key IS NOT NULL)
        OR
        (profile_image_source IN ('SOCIAL', 'DEFAULT') AND profile_image_object_key IS NULL)
    );
