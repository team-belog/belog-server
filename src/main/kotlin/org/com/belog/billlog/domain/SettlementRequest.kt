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
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.com.belog.global.domain.BaseEntity
import org.com.belog.meeting.domain.Meeting
import org.com.belog.meeting.domain.MeetingParticipant
import java.math.BigDecimal
import java.time.Duration
import java.time.Instant

@Entity
@Table(
    name = "bill_log_settlement_requests",
    indexes = [
        Index(
            name = "idx_bill_log_settlement_requests_meeting_status",
            columnList = "meeting_id, status",
        ),
    ],
    check = [
        CheckConstraint(
            name = "chk_bill_log_settlement_requests_amount",
            constraint = "(status = 'PENDING' AND amount > 0) OR (status = 'COMPLETED' AND amount >= 0)",
        ),
        CheckConstraint(
            name = "chk_bill_log_settlement_requests_participants",
            constraint = "sender_meeting_participant_id <> receiver_meeting_participant_id",
        ),
        CheckConstraint(
            name = "chk_bill_log_settlement_requests_completion",
            constraint =
                "(status = 'PENDING' AND completed_at IS NULL) OR " +
                    "(status = 'COMPLETED' AND completed_at IS NOT NULL)",
        ),
    ],
)
class SettlementRequest protected constructor(
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "meeting_id",
        nullable = false,
        foreignKey = ForeignKey(name = "fk_bill_log_settlement_requests_meeting_id"),
    )
    val meeting: Meeting,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = BillCurrency.COLUMN_LENGTH)
    val currency: BillCurrency,
    sender: MeetingParticipant,
    receiver: MeetingParticipant,
    amount: BigDecimal,
) : BaseEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "sender_meeting_participant_id",
        nullable = false,
        foreignKey = ForeignKey(name = "fk_bill_log_settlement_requests_sender_id"),
    )
    var sender: MeetingParticipant = sender
        protected set

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "receiver_meeting_participant_id",
        nullable = false,
        foreignKey = ForeignKey(name = "fk_bill_log_settlement_requests_receiver_id"),
    )
    var receiver: MeetingParticipant = receiver
        protected set

    @Column(nullable = false, precision = BillAmount.PRECISION, scale = BillAmount.SCALE)
    var amount: BigDecimal = amount
        protected set

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: SettlementRequestStatus = SettlementRequestStatus.PENDING
        protected set

    @Column(name = "completed_at")
    var completedAt: Instant? = null
        protected set

    @Column(name = "last_reminded_at")
    var lastRemindedAt: Instant? = null
        protected set

    fun isReminderCooldownElapsed(now: Instant): Boolean {
        val lastRemindedAt = lastRemindedAt ?: return true
        return !now.isBefore(lastRemindedAt.plus(REMINDER_COOLDOWN))
    }

    fun remind(remindedAt: Instant) {
        check(status == SettlementRequestStatus.PENDING) { "완료된 정산 요청은 리마인드할 수 없습니다." }
        check(isReminderCooldownElapsed(remindedAt)) { "정산 리마인드 대기 시간이 지나지 않았습니다." }

        lastRemindedAt = remindedAt
    }

    fun complete(completedAt: Instant) {
        if (status == SettlementRequestStatus.COMPLETED) {
            return
        }

        status = SettlementRequestStatus.COMPLETED
        this.completedAt = completedAt
    }

    fun offset(
        debtor: MeetingParticipant,
        creditor: MeetingParticipant,
        debtAmount: BigDecimal,
        offsetAt: Instant,
    ) {
        check(status == SettlementRequestStatus.PENDING) { "완료된 정산 요청에는 결제 내역을 상계할 수 없습니다." }
        validateDebtAmount(currency, debtAmount)

        val netAmount = calculateNetAmount(debtor, creditor, debtAmount)
        require(BillAmount.isStorable(netAmount)) { "상계된 정산 금액이 저장 가능한 범위를 벗어났습니다." }

        if (netAmount.signum() < 0) {
            val previousSender = sender
            sender = receiver
            receiver = previousSender
        }
        amount = currency.normalize(netAmount.abs())
        lastRemindedAt = null

        if (amount.signum() == 0) {
            complete(offsetAt)
        }
    }

    fun canStoreOffset(
        debtor: MeetingParticipant,
        creditor: MeetingParticipant,
        debtAmount: BigDecimal,
    ): Boolean = BillAmount.isStorable(calculateNetAmount(debtor, creditor, debtAmount))

    private fun calculateNetAmount(
        debtor: MeetingParticipant,
        creditor: MeetingParticipant,
        debtAmount: BigDecimal,
    ): BigDecimal =
        when {
            debtor.isSameParticipantAs(sender) && creditor.isSameParticipantAs(receiver) -> amount.add(debtAmount)
            debtor.isSameParticipantAs(receiver) && creditor.isSameParticipantAs(sender) -> amount.subtract(debtAmount)
            else -> throw IllegalArgumentException("정산 요청의 두 참여자 사이 결제 내역만 상계할 수 있습니다.")
        }

    fun covers(
        bill: Bill,
        debtor: MeetingParticipant,
    ): Boolean {
        val creditor = bill.payer
        val isSamePair =
            (debtor.isSameParticipantAs(sender) && creditor.isSameParticipantAs(receiver)) ||
                (debtor.isSameParticipantAs(receiver) && creditor.isSameParticipantAs(sender))
        return currency == bill.currency && isSamePair
    }

    companion object {
        private val REMINDER_COOLDOWN: Duration = Duration.ofMinutes(1)

        fun create(
            meeting: Meeting,
            currency: BillCurrency,
            debtor: MeetingParticipant,
            creditor: MeetingParticipant,
            debtAmount: BigDecimal,
        ): SettlementRequest {
            require(!debtor.isSameParticipantAs(creditor)) { "결제자에게는 정산 요청을 생성할 수 없습니다." }
            require(debtor.belongsTo(meeting) && creditor.belongsTo(meeting)) {
                "정산 요청의 송금자와 수취인은 같은 만남의 참여자여야 합니다."
            }
            validateDebtAmount(currency, debtAmount)

            return SettlementRequest(
                meeting = meeting,
                currency = currency,
                sender = debtor,
                receiver = creditor,
                amount = currency.normalize(debtAmount),
            )
        }

        private fun validateDebtAmount(
            currency: BillCurrency,
            debtAmount: BigDecimal,
        ) {
            require(debtAmount.signum() > 0) { "상계할 부담 금액은 0보다 커야 합니다." }
            require(currency.supports(debtAmount) && BillAmount.isStorable(debtAmount)) {
                "상계할 부담 금액이 화폐 단위의 허용 범위를 벗어났습니다."
            }
        }
    }
}

internal fun MeetingParticipant.isSameParticipantAs(other: MeetingParticipant): Boolean {
    if (this === other) {
        return true
    }

    val participantId = id
    val otherParticipantId = other.id
    return participantId != null && otherParticipantId != null && participantId == otherParticipantId
}
