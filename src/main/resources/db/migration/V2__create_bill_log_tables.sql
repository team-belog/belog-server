CREATE TABLE bill_log_bills
(
    id                           BIGINT       NOT NULL AUTO_INCREMENT,
    meeting_id                   BIGINT       NOT NULL,
    created_by_group_member_id   BIGINT       NOT NULL,
    payer_meeting_participant_id BIGINT       NOT NULL,
    title                        VARCHAR(100) NOT NULL,
    total_amount                 BIGINT       NOT NULL,
    split_type                   VARCHAR(20)  NOT NULL,
    created_at                   DATETIME(6)  NOT NULL,
    updated_at                   DATETIME(6)  NOT NULL,
    CONSTRAINT pk_bill_log_bills PRIMARY KEY (id),
    CONSTRAINT fk_bill_log_bills_meeting_id
        FOREIGN KEY (meeting_id) REFERENCES meetings (id),
    CONSTRAINT fk_bill_log_bills_created_by_member_id
        FOREIGN KEY (created_by_group_member_id) REFERENCES group_members (id),
    CONSTRAINT fk_bill_log_bills_payer_participant_id
        FOREIGN KEY (payer_meeting_participant_id) REFERENCES meeting_participants (id),
    CONSTRAINT chk_bill_log_bills_title CHECK (CHAR_LENGTH(TRIM(title)) BETWEEN 1 AND 100),
    CONSTRAINT chk_bill_log_bills_total_amount CHECK (total_amount > 0)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE bill_log_bill_items
(
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    bill_id    BIGINT       NOT NULL,
    name       VARCHAR(100) NOT NULL,
    amount     BIGINT       NOT NULL,
    item_order INT          NOT NULL,
    created_at DATETIME(6)  NOT NULL,
    updated_at DATETIME(6)  NOT NULL,
    CONSTRAINT pk_bill_log_bill_items PRIMARY KEY (id),
    CONSTRAINT uk_bill_log_bill_items_bill_order UNIQUE (bill_id, item_order),
    CONSTRAINT fk_bill_log_bill_items_bill_id
        FOREIGN KEY (bill_id) REFERENCES bill_log_bills (id),
    CONSTRAINT chk_bill_log_bill_items_name CHECK (CHAR_LENGTH(TRIM(name)) BETWEEN 1 AND 100),
    CONSTRAINT chk_bill_log_bill_items_amount CHECK (amount > 0),
    CONSTRAINT chk_bill_log_bill_items_order CHECK (item_order >= 0)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE bill_log_bill_shares
(
    id                     BIGINT      NOT NULL AUTO_INCREMENT,
    bill_id                BIGINT      NOT NULL,
    meeting_participant_id BIGINT      NOT NULL,
    amount                 BIGINT      NOT NULL,
    allocation_order       INT         NOT NULL,
    created_at             DATETIME(6) NOT NULL,
    updated_at             DATETIME(6) NOT NULL,
    CONSTRAINT pk_bill_log_bill_shares PRIMARY KEY (id),
    CONSTRAINT uk_bill_log_bill_shares_bill_participant UNIQUE (bill_id, meeting_participant_id),
    CONSTRAINT uk_bill_log_bill_shares_bill_order UNIQUE (bill_id, allocation_order),
    CONSTRAINT fk_bill_log_bill_shares_bill_id
        FOREIGN KEY (bill_id) REFERENCES bill_log_bills (id),
    CONSTRAINT fk_bill_log_bill_shares_participant_id
        FOREIGN KEY (meeting_participant_id) REFERENCES meeting_participants (id),
    CONSTRAINT chk_bill_log_bill_shares_amount CHECK (amount > 0),
    CONSTRAINT chk_bill_log_bill_shares_order CHECK (allocation_order >= 0)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE bill_log_settlement_requests
(
    id                     BIGINT      NOT NULL AUTO_INCREMENT,
    bill_id                BIGINT      NOT NULL,
    meeting_participant_id BIGINT      NOT NULL,
    amount                 BIGINT      NOT NULL,
    status                 VARCHAR(20) NOT NULL,
    created_at             DATETIME(6) NOT NULL,
    updated_at             DATETIME(6) NOT NULL,
    CONSTRAINT pk_bill_log_settlement_requests PRIMARY KEY (id),
    CONSTRAINT uk_bill_log_settlement_requests_bill_participant UNIQUE (bill_id, meeting_participant_id),
    CONSTRAINT fk_bill_log_settlement_requests_bill_id
        FOREIGN KEY (bill_id) REFERENCES bill_log_bills (id),
    CONSTRAINT fk_bill_log_settlement_requests_participant_id
        FOREIGN KEY (meeting_participant_id) REFERENCES meeting_participants (id),
    CONSTRAINT chk_bill_log_settlement_requests_amount CHECK (amount > 0)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;
