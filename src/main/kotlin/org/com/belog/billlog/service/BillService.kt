package org.com.belog.billlog.service

import org.com.belog.billlog.code.BillLogErrorCode
import org.com.belog.billlog.domain.Bill
import org.com.belog.billlog.domain.BillItem
import org.com.belog.billlog.domain.BillShare
import org.com.belog.billlog.domain.SettlementRequest
import org.com.belog.billlog.domain.calculateBillDayNumber
import org.com.belog.billlog.repository.BillItemRepository
import org.com.belog.billlog.repository.BillRepository
import org.com.belog.billlog.repository.BillShareRepository
import org.com.belog.billlog.repository.SettlementRequestRepository
import org.com.belog.billlog.service.command.RegisterBillCommand
import org.com.belog.billlog.service.result.BillDetailResult
import org.com.belog.billlog.service.result.BillItemResult
import org.com.belog.billlog.service.result.BillShareResult
import org.com.belog.billlog.service.result.RegisteredBill
import org.com.belog.global.error.BusinessException
import org.com.belog.global.time.toBusinessDate
import org.com.belog.group.code.GroupErrorCode
import org.com.belog.group.domain.GroupMember
import org.com.belog.group.repository.GroupMemberRepository
import org.com.belog.meeting.code.MeetingErrorCode
import org.com.belog.meeting.domain.Meeting
import org.com.belog.meeting.domain.MeetingParticipant
import org.com.belog.meeting.repository.MeetingParticipantRepository
import org.com.belog.meeting.repository.MeetingRepository
import org.com.belog.user.service.UserService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class BillService(
    private val meetingRepository: MeetingRepository,
    private val groupMemberRepository: GroupMemberRepository,
    private val meetingParticipantRepository: MeetingParticipantRepository,
    private val billRepository: BillRepository,
    private val billItemRepository: BillItemRepository,
    private val billShareRepository: BillShareRepository,
    private val settlementRequestRepository: SettlementRequestRepository,
    private val userService: UserService,
) {
    @Transactional
    fun registerBill(command: RegisterBillCommand): RegisteredBill {
        validateCommand(command)

        val meeting = findMeeting(command.meetingId)
        val creator = findGroupMember(meeting, command.creatorUserId)
        val participantsByMemberId = findParticipantsByMemberId(meeting, command)
        val payer =
            participantsByMemberId[command.payerMemberId]
                ?: throw BusinessException(BillLogErrorCode.PAYER_NOT_MEETING_PARTICIPANT)
        val shareParticipants =
            command.shares.map { share ->
                participantsByMemberId[share.participantMemberId]
                    ?: throw BusinessException(BillLogErrorCode.INVALID_SHARE_PARTICIPANT)
            }

        val bill = saveBill(command, meeting, creator, payer)
        saveItems(command, bill)
        val shares = saveShares(command, bill, shareParticipants)
        saveSettlementRequests(shares, payer)

        return RegisteredBill(
            billId = checkNotNull(bill.id) { "저장된 결제 내역의 ID가 없습니다." },
        )
    }

    @Transactional(readOnly = true)
    fun getBillDetail(
        billId: Long,
        userId: Long,
    ): BillDetailResult {
        val bill =
            billRepository.findByIdWithMeetingAndPayer(billId)
                ?: throw BusinessException(BillLogErrorCode.BILL_NOT_FOUND)
        val meeting = bill.meeting
        findGroupMember(meeting, userId)
        val items = billItemRepository.findAllByBillIdOrderByItemOrderAsc(billId)
        val shares = billShareRepository.findAllWithParticipantAndUserByBillId(billId)
        val paymentDate =
            checkNotNull(bill.createdAt) { "조회된 결제 내역의 생성 시각이 없습니다." }
                .toBusinessDate()
        val meetingStartDate =
            meeting.startDate
                ?: throw BusinessException(BillLogErrorCode.MEETING_DATE_NOT_CONFIRMED)
        val payerParticipantId = checkNotNull(bill.payer.id) { "조회된 결제자의 만남 참여자 ID가 없습니다." }
        val payerNickname = checkNotNull(bill.payer.groupMember.user.nickname) { "조회된 결제자의 닉네임이 없습니다." }

        return BillDetailResult(
            billId = checkNotNull(bill.id) { "조회된 결제 내역의 ID가 없습니다." },
            dayNumber = calculateBillDayNumber(meetingStartDate, paymentDate),
            paymentDate = paymentDate,
            title = bill.title,
            payerNickname = payerNickname,
            settlementMethod = bill.splitType,
            items =
                items.map { item ->
                    BillItemResult(
                        name = item.name,
                        amount = item.amount,
                    )
                },
            totalAmount = bill.totalAmount,
            shares =
                shares.map { share ->
                    val participant = share.participant
                    val participantId =
                        checkNotNull(participant.id) { "조회된 부담자의 만남 참여자 ID가 없습니다." }
                    val participantUser = participant.groupMember.user

                    BillShareResult(
                        meetingParticipantId = participantId,
                        nickname = checkNotNull(participantUser.nickname) { "조회된 부담자의 닉네임이 없습니다." },
                        profileImageUrl = userService.resolveProfileImageUrl(participantUser),
                        amount = share.amount,
                        payer = participantId == payerParticipantId,
                    )
                },
        )
    }

    private fun validateCommand(command: RegisterBillCommand) {
        if (command.items.isEmpty()) {
            throw BusinessException(BillLogErrorCode.EMPTY_BILL_ITEM)
        }
        if (command.shares.isEmpty()) {
            throw BusinessException(BillLogErrorCode.EMPTY_BILL_SHARE)
        }
        if (
            command.totalAmount <= 0 ||
            command.items.any { item -> item.amount <= 0 } ||
            command.shares.any { share -> share.amount <= 0 }
        ) {
            throw BusinessException(BillLogErrorCode.INVALID_AMOUNT)
        }

        val shareMemberIds = command.shares.map { share -> share.participantMemberId }
        if (shareMemberIds.distinct().size != shareMemberIds.size) {
            throw BusinessException(BillLogErrorCode.DUPLICATE_SHARE_PARTICIPANT)
        }

        val itemTotal = sumAmounts(command.items.map { item -> item.amount })
        if (itemTotal != command.totalAmount) {
            throw BusinessException(BillLogErrorCode.ITEM_TOTAL_MISMATCH)
        }

        val shareTotal = sumAmounts(command.shares.map { share -> share.amount })
        if (shareTotal != command.totalAmount) {
            throw BusinessException(BillLogErrorCode.SHARE_TOTAL_MISMATCH)
        }
    }

    private fun sumAmounts(amounts: List<Long>): Long =
        try {
            amounts.fold(0L) { total, amount -> Math.addExact(total, amount) }
        } catch (_: ArithmeticException) {
            throw BusinessException(BillLogErrorCode.AMOUNT_OVERFLOW)
        }

    private fun findMeeting(meetingId: Long): Meeting =
        meetingRepository.findByIdWithGroupAndCreator(meetingId)
            ?: throw BusinessException(MeetingErrorCode.MEETING_NOT_FOUND)

    private fun findGroupMember(
        meeting: Meeting,
        userId: Long,
    ): GroupMember {
        val groupId = checkNotNull(meeting.group.id) { "결제 내역 대상 만남의 그룹 ID가 없습니다." }
        return groupMemberRepository.findByGroupIdAndUserIdAndWithdrawnAtIsNull(groupId, userId)
            ?: throw BusinessException(GroupErrorCode.NOT_GROUP_MEMBER)
    }

    private fun findParticipantsByMemberId(
        meeting: Meeting,
        command: RegisterBillCommand,
    ): Map<Long, MeetingParticipant> {
        val memberIds =
            (listOf(command.payerMemberId) + command.shares.map { share -> share.participantMemberId }).distinct()

        return meetingParticipantRepository
            .findAllByMeetingIdAndGroupMemberIdIn(
                meetingId = checkNotNull(meeting.id) { "결제 내역 대상 만남의 ID가 없습니다." },
                groupMemberIds = memberIds,
            ).associateBy { participant ->
                checkNotNull(participant.groupMember.id) { "만남 참여자의 그룹 멤버 ID가 없습니다." }
            }
    }

    private fun saveBill(
        command: RegisterBillCommand,
        meeting: Meeting,
        creator: GroupMember,
        payer: MeetingParticipant,
    ): Bill =
        saveDomainEntity {
            billRepository.save(
                Bill.create(
                    meeting = meeting,
                    creator = creator,
                    payer = payer,
                    title = command.title,
                    totalAmount = command.totalAmount,
                    splitType = command.splitType,
                ),
            )
        }

    private fun saveItems(
        command: RegisterBillCommand,
        bill: Bill,
    ) {
        saveDomainEntity {
            billItemRepository.saveAll(
                command.items.mapIndexed { index, item ->
                    BillItem.create(
                        bill = bill,
                        name = item.name,
                        amount = item.amount,
                        itemOrder = index,
                    )
                },
            )
        }
    }

    private fun saveShares(
        command: RegisterBillCommand,
        bill: Bill,
        participants: List<MeetingParticipant>,
    ): List<BillShare> =
        saveDomainEntity {
            billShareRepository.saveAll(
                command.shares.zip(participants).mapIndexed { index, (share, participant) ->
                    BillShare.create(
                        bill = bill,
                        participant = participant,
                        amount = share.amount,
                        allocationOrder = index,
                    )
                },
            )
        }

    private fun saveSettlementRequests(
        shares: List<BillShare>,
        payer: MeetingParticipant,
    ) {
        val payerId = checkNotNull(payer.id) { "결제자의 만남 참여자 ID가 없습니다." }
        val settlementRequests =
            shares
                .filter { share -> share.participant.id != payerId }
                .map(SettlementRequest::create)

        settlementRequestRepository.saveAll(settlementRequests)
    }

    private fun <T> saveDomainEntity(block: () -> T): T =
        try {
            block()
        } catch (exception: IllegalArgumentException) {
            throw BusinessException(BillLogErrorCode.INVALID_BILL, exception)
        }
}
