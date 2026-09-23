package org.com.belog.billlog.domain

import jakarta.persistence.CheckConstraint
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.ForeignKey
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import org.com.belog.global.domain.BaseEntity
import org.com.belog.meeting.domain.MeetingParticipant

@Entity
@Table(
    name = "bill_log_settlement_requests",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_bill_log_settlement_requests_bill_participant",
            columnNames = ["bill_id", "meeting_participant_id"],
        ),
    ],
    check = [
        CheckConstraint(
            name = "chk_bill_log_settlement_requests_amount",
            constraint = "amount > 0",
        ),
    ],
)
class SettlementRequest protected constructor(
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "bill_id",
        nullable = false,
        foreignKey = ForeignKey(name = "fk_bill_log_settlement_requests_bill_id"),
    )
    val bill: Bill,
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "meeting_participant_id",
        nullable = false,
        foreignKey = ForeignKey(name = "fk_bill_log_settlement_requests_participant_id"),
    )
    val participant: MeetingParticipant,
    @Column(nullable = false)
    val amount: Long,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val status: SettlementRequestStatus,
) : BaseEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set

    companion object {
        fun create(share: BillShare): SettlementRequest {
            require(!share.participant.isSameParticipantAs(share.bill.payer)) {
                "결제자에게는 정산 요청을 생성할 수 없습니다."
            }

            return SettlementRequest(
                bill = share.bill,
                participant = share.participant,
                amount = share.amount,
                status = SettlementRequestStatus.PENDING,
            )
        }
    }
}

private fun MeetingParticipant.isSameParticipantAs(other: MeetingParticipant): Boolean {
    if (this === other) {
        return true
    }

    val participantId = id
    val otherParticipantId = other.id
    return participantId != null && otherParticipantId != null && participantId == otherParticipantId
}
