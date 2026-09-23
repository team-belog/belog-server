package org.com.belog.billlog.domain

import jakarta.persistence.CheckConstraint
import jakarta.persistence.Column
import jakarta.persistence.Entity
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
    name = "bill_log_bill_shares",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_bill_log_bill_shares_bill_participant",
            columnNames = ["bill_id", "meeting_participant_id"],
        ),
        UniqueConstraint(
            name = "uk_bill_log_bill_shares_bill_order",
            columnNames = ["bill_id", "allocation_order"],
        ),
    ],
    check = [
        CheckConstraint(
            name = "chk_bill_log_bill_shares_amount",
            constraint = "amount > 0",
        ),
        CheckConstraint(
            name = "chk_bill_log_bill_shares_order",
            constraint = "allocation_order >= 0",
        ),
    ],
)
class BillShare protected constructor(
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "bill_id",
        nullable = false,
        foreignKey = ForeignKey(name = "fk_bill_log_bill_shares_bill_id"),
    )
    val bill: Bill,
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "meeting_participant_id",
        nullable = false,
        foreignKey = ForeignKey(name = "fk_bill_log_bill_shares_participant_id"),
    )
    val participant: MeetingParticipant,
    @Column(nullable = false)
    val amount: Long,
    @Column(name = "allocation_order", nullable = false)
    val allocationOrder: Int,
) : BaseEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set

    companion object {
        fun create(
            bill: Bill,
            participant: MeetingParticipant,
            amount: Long,
            allocationOrder: Int,
        ): BillShare {
            require(participant.belongsTo(bill.meeting)) {
                "부담자는 결제 내역이 속한 만남의 참여자여야 합니다."
            }
            require(amount > 0) {
                "개인별 부담 금액은 0원보다 커야 합니다."
            }
            require(allocationOrder >= 0) {
                "개인별 부담 금액 배분 순서는 0 이상이어야 합니다."
            }

            return BillShare(
                bill = bill,
                participant = participant,
                amount = amount,
                allocationOrder = allocationOrder,
            )
        }
    }
}
