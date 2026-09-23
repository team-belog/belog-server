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
import org.com.belog.global.domain.BaseEntity
import org.com.belog.group.domain.Group
import org.com.belog.group.domain.GroupMember
import org.com.belog.meeting.domain.Meeting
import org.com.belog.meeting.domain.MeetingParticipant

private const val BILL_TITLE_MIN_LENGTH = 1
private const val BILL_TITLE_MAX_LENGTH = 100

@Entity
@Table(
    name = "bill_log_bills",
    check = [
        CheckConstraint(
            name = "chk_bill_log_bills_title",
            constraint =
                "CHAR_LENGTH(TRIM(title)) BETWEEN $BILL_TITLE_MIN_LENGTH AND $BILL_TITLE_MAX_LENGTH",
        ),
        CheckConstraint(
            name = "chk_bill_log_bills_total_amount",
            constraint = "total_amount > 0",
        ),
    ],
)
class Bill protected constructor(
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "meeting_id",
        nullable = false,
        foreignKey = ForeignKey(name = "fk_bill_log_bills_meeting_id"),
    )
    val meeting: Meeting,
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "created_by_group_member_id",
        nullable = false,
        foreignKey = ForeignKey(name = "fk_bill_log_bills_created_by_member_id"),
    )
    val createdBy: GroupMember,
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "payer_meeting_participant_id",
        nullable = false,
        foreignKey = ForeignKey(name = "fk_bill_log_bills_payer_participant_id"),
    )
    val payer: MeetingParticipant,
    @Column(nullable = false, length = BILL_TITLE_MAX_LENGTH)
    val title: String,
    @Column(name = "total_amount", nullable = false)
    val totalAmount: Long,
    @Enumerated(EnumType.STRING)
    @Column(name = "split_type", nullable = false, length = 20)
    val splitType: BillSplitType,
) : BaseEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set

    companion object {
        const val TITLE_MIN_LENGTH = BILL_TITLE_MIN_LENGTH
        const val TITLE_MAX_LENGTH = BILL_TITLE_MAX_LENGTH

        fun create(
            meeting: Meeting,
            creator: GroupMember,
            payer: MeetingParticipant,
            title: String,
            totalAmount: Long,
            splitType: BillSplitType,
        ): Bill {
            require(creator.belongsTo(meeting.group)) {
                "결제 내역 작성자는 해당 만남이 속한 그룹의 멤버여야 합니다."
            }
            require(payer.belongsTo(meeting)) {
                "결제자는 해당 만남의 참여자여야 합니다."
            }
            require(totalAmount > 0) {
                "결제 총액은 0원보다 커야 합니다."
            }

            val normalizedTitle = title.trim()
            require(normalizedTitle.length in TITLE_MIN_LENGTH..TITLE_MAX_LENGTH) {
                "결제 내역 제목은 ${TITLE_MIN_LENGTH}자 이상 ${TITLE_MAX_LENGTH}자 이하여야 합니다."
            }

            return Bill(
                meeting = meeting,
                createdBy = creator,
                payer = payer,
                title = normalizedTitle,
                totalAmount = totalAmount,
                splitType = splitType,
            )
        }
    }
}

internal fun GroupMember.belongsTo(group: Group): Boolean {
    if (this.group === group) {
        return true
    }

    val memberGroupId = this.group.id
    val groupId = group.id
    return memberGroupId != null && groupId != null && memberGroupId == groupId
}

internal fun MeetingParticipant.belongsTo(meeting: Meeting): Boolean {
    if (this.meeting === meeting) {
        return true
    }

    val participantMeetingId = this.meeting.id
    val meetingId = meeting.id
    return participantMeetingId != null && meetingId != null && participantMeetingId == meetingId
}
