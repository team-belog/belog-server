ALTER TABLE group_members
    ADD COLUMN pinned BOOLEAN NOT NULL DEFAULT FALSE,
    ADD INDEX idx_group_members_user_pinned_id (user_id, pinned, id);
