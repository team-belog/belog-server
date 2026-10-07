ALTER TABLE belog_groups
    ADD COLUMN deleted_at DATETIME(6) NULL AFTER updated_at;
