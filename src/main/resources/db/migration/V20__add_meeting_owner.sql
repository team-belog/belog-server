ALTER TABLE meetings
    ADD COLUMN owner_group_member_id BIGINT NULL AFTER created_by_group_member_id;

UPDATE meetings
SET owner_group_member_id = created_by_group_member_id;

ALTER TABLE meetings
    MODIFY COLUMN owner_group_member_id BIGINT NOT NULL,
    ADD CONSTRAINT fk_meetings_owner_group_member_id
        FOREIGN KEY (owner_group_member_id) REFERENCES group_members (id);
