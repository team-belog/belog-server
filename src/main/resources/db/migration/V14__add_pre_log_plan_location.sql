ALTER TABLE pre_log_plans
    ADD COLUMN map_provider VARCHAR(20) NULL,
    ADD COLUMN external_place_id VARCHAR(512) NULL,
    ADD COLUMN place_name VARCHAR(100) NULL,
    ADD COLUMN address VARCHAR(500) NULL,
    ADD COLUMN latitude DECIMAL(10, 7) NULL,
    ADD COLUMN longitude DECIMAL(11, 7) NULL,
    ADD COLUMN location_status VARCHAR(20) NOT NULL DEFAULT 'NOT_APPLICABLE',
    ADD CONSTRAINT chk_pre_log_plans_location_status CHECK (
        (
            location_status = 'RESOLVED'
            AND map_provider IS NOT NULL
            AND latitude IS NOT NULL
            AND longitude IS NOT NULL
        )
        OR
        (
            location_status IN ('NOT_APPLICABLE', 'FAILED')
            AND map_provider IS NULL
            AND external_place_id IS NULL
            AND place_name IS NULL
            AND address IS NULL
            AND latitude IS NULL
            AND longitude IS NULL
        )
    ),
    ADD CONSTRAINT chk_pre_log_plans_latitude CHECK (
        latitude IS NULL OR latitude BETWEEN -90 AND 90
    ),
    ADD CONSTRAINT chk_pre_log_plans_longitude CHECK (
        longitude IS NULL OR longitude BETWEEN -180 AND 180
    ),
    ADD CONSTRAINT chk_pre_log_plans_map_provider CHECK (
        map_provider IS NULL OR map_provider IN ('GOOGLE', 'KAKAO')
    ),
    ADD CONSTRAINT chk_pre_log_plans_external_place_id CHECK (
        external_place_id IS NULL OR CHAR_LENGTH(TRIM(external_place_id)) BETWEEN 1 AND 512
    ),
    ADD CONSTRAINT chk_pre_log_plans_place_name CHECK (
        place_name IS NULL OR CHAR_LENGTH(TRIM(place_name)) BETWEEN 1 AND 100
    ),
    ADD CONSTRAINT chk_pre_log_plans_address CHECK (
        address IS NULL OR CHAR_LENGTH(TRIM(address)) BETWEEN 1 AND 500
    ),
    ADD INDEX idx_pre_log_plans_meeting_location (meeting_id, location_status, id);
