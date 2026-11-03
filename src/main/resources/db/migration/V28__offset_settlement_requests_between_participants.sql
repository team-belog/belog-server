ALTER TABLE bill_log_settlement_requests
    ADD COLUMN meeting_id                      BIGINT     NULL AFTER id,
    ADD COLUMN currency                        VARCHAR(3) NULL AFTER meeting_id,
    ADD COLUMN receiver_meeting_participant_id BIGINT     NULL AFTER meeting_participant_id;

UPDATE bill_log_settlement_requests settlement_request
    JOIN bill_log_bills bill ON bill.id = settlement_request.bill_id
SET settlement_request.meeting_id                      = bill.meeting_id,
    settlement_request.currency                        = bill.currency,
    settlement_request.receiver_meeting_participant_id = bill.payer_meeting_participant_id;

ALTER TABLE bill_log_bill_shares
    ADD COLUMN settlement_request_id BIGINT NULL AFTER allocation_order;

UPDATE bill_log_bill_shares share
    JOIN bill_log_settlement_requests settlement_request
    ON settlement_request.bill_id = share.bill_id
        AND settlement_request.meeting_participant_id = share.meeting_participant_id
SET share.settlement_request_id = settlement_request.id;

CREATE TEMPORARY TABLE pending_settlement_offsets AS
SELECT settlement_request.meeting_id,
       settlement_request.currency,
       LEAST(settlement_request.meeting_participant_id,
             settlement_request.receiver_meeting_participant_id)    AS first_participant_id,
       GREATEST(settlement_request.meeting_participant_id,
                settlement_request.receiver_meeting_participant_id) AS second_participant_id,
       MIN(settlement_request.id)                                   AS kept_settlement_request_id,
       COUNT(*)                                                     AS merged_count,
       SUM(
               CASE
                   WHEN settlement_request.meeting_participant_id <
                        settlement_request.receiver_meeting_participant_id
                       THEN settlement_request.amount
                   ELSE -settlement_request.amount
                   END
       )                                                            AS net_amount
FROM bill_log_settlement_requests settlement_request
WHERE settlement_request.status = 'PENDING'
GROUP BY settlement_request.meeting_id,
         settlement_request.currency,
         LEAST(settlement_request.meeting_participant_id, settlement_request.receiver_meeting_participant_id),
         GREATEST(settlement_request.meeting_participant_id, settlement_request.receiver_meeting_participant_id)
HAVING COUNT(*) > 1;

UPDATE bill_log_bill_shares share
    JOIN bill_log_settlement_requests settlement_request ON settlement_request.id = share.settlement_request_id
    JOIN pending_settlement_offsets pending_offset
    ON pending_offset.meeting_id = settlement_request.meeting_id
        AND pending_offset.currency = settlement_request.currency
        AND pending_offset.first_participant_id = LEAST(settlement_request.meeting_participant_id,
                                                        settlement_request.receiver_meeting_participant_id)
        AND pending_offset.second_participant_id = GREATEST(settlement_request.meeting_participant_id,
                                                            settlement_request.receiver_meeting_participant_id)
SET share.settlement_request_id = pending_offset.kept_settlement_request_id
WHERE settlement_request.status = 'PENDING';

DELETE settlement_request
FROM bill_log_settlement_requests settlement_request
         JOIN pending_settlement_offsets pending_offset
              ON pending_offset.meeting_id = settlement_request.meeting_id
                  AND pending_offset.currency = settlement_request.currency
                  AND pending_offset.first_participant_id = LEAST(settlement_request.meeting_participant_id,
                                                                  settlement_request.receiver_meeting_participant_id)
                  AND pending_offset.second_participant_id = GREATEST(settlement_request.meeting_participant_id,
                                                                      settlement_request.receiver_meeting_participant_id)
WHERE settlement_request.status = 'PENDING'
  AND settlement_request.id <> pending_offset.kept_settlement_request_id;

ALTER TABLE bill_log_settlement_requests
    DROP CHECK chk_bill_log_settlement_requests_amount;

UPDATE bill_log_settlement_requests settlement_request
    JOIN pending_settlement_offsets pending_offset
    ON pending_offset.kept_settlement_request_id = settlement_request.id
SET settlement_request.meeting_participant_id          =
        IF(pending_offset.net_amount >= 0, pending_offset.first_participant_id,
           pending_offset.second_participant_id),
    settlement_request.receiver_meeting_participant_id =
        IF(pending_offset.net_amount >= 0, pending_offset.second_participant_id,
           pending_offset.first_participant_id),
    settlement_request.amount                          = ABS(pending_offset.net_amount),
    settlement_request.status                          = IF(pending_offset.net_amount = 0, 'COMPLETED', 'PENDING'),
    settlement_request.completed_at                    = IF(pending_offset.net_amount = 0, UTC_TIMESTAMP(6), NULL),
    settlement_request.last_reminded_at                = NULL;

DROP TEMPORARY TABLE pending_settlement_offsets;

ALTER TABLE bill_log_settlement_requests
    DROP FOREIGN KEY fk_bill_log_settlement_requests_bill_id,
    DROP FOREIGN KEY fk_bill_log_settlement_requests_participant_id;

ALTER TABLE bill_log_settlement_requests
    DROP INDEX uk_bill_log_settlement_requests_bill_participant,
    DROP INDEX fk_bill_log_settlement_requests_participant_id,
    DROP COLUMN bill_id,
    RENAME COLUMN meeting_participant_id TO sender_meeting_participant_id,
    MODIFY COLUMN meeting_id BIGINT NOT NULL,
    MODIFY COLUMN currency VARCHAR(3) NOT NULL,
    MODIFY COLUMN receiver_meeting_participant_id BIGINT NOT NULL AFTER sender_meeting_participant_id,
    ADD COLUMN pending_pair_key VARCHAR(100) GENERATED ALWAYS AS (
        IF(status = 'PENDING',
           CONCAT_WS(':', meeting_id, currency,
                     LEAST(sender_meeting_participant_id, receiver_meeting_participant_id),
                     GREATEST(sender_meeting_participant_id, receiver_meeting_participant_id)),
           NULL)
        ) STORED,
    ADD CONSTRAINT uk_bill_log_settlement_requests_pending_pair UNIQUE (pending_pair_key),
    ADD INDEX idx_bill_log_settlement_requests_meeting_status (meeting_id, status),
    ADD CONSTRAINT fk_bill_log_settlement_requests_meeting_id
        FOREIGN KEY (meeting_id) REFERENCES meetings (id),
    ADD CONSTRAINT fk_bill_log_settlement_requests_sender_id
        FOREIGN KEY (sender_meeting_participant_id) REFERENCES meeting_participants (id),
    ADD CONSTRAINT fk_bill_log_settlement_requests_receiver_id
        FOREIGN KEY (receiver_meeting_participant_id) REFERENCES meeting_participants (id),
    ADD CONSTRAINT chk_bill_log_settlement_requests_amount
        CHECK ((status = 'PENDING' AND amount > 0) OR (status = 'COMPLETED' AND amount >= 0)),
    ADD CONSTRAINT chk_bill_log_settlement_requests_participants
        CHECK (sender_meeting_participant_id <> receiver_meeting_participant_id),
    ADD CONSTRAINT chk_bill_log_settlement_requests_currency
        CHECK (currency IN ('KRW', 'USD', 'JPY', 'EUR'));

ALTER TABLE bill_log_bill_shares
    ADD CONSTRAINT fk_bill_log_bill_shares_settlement_request_id
        FOREIGN KEY (settlement_request_id) REFERENCES bill_log_settlement_requests (id);
