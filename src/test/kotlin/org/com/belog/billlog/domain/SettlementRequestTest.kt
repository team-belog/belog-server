package org.com.belog.billlog.domain

import org.com.belog.group.domain.Group
import org.com.belog.group.domain.GroupMember
import org.com.belog.group.domain.InviteCode
import org.com.belog.meeting.domain.Meeting
import org.com.belog.meeting.domain.MeetingDateRange
import org.com.belog.meeting.domain.MeetingParticipant
import org.com.belog.user.domain.Bank
import org.com.belog.user.domain.BankAccount
import org.com.belog.user.domain.SocialProvider
import org.com.belog.user.domain.User
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertSame

class SettlementRequestTest {
    private val group = Group.create(name = "주말 여행 모임", coverImageObjectKey = null, inviteCode = InviteCode.create("AB12CD"))
    private val firstMember = GroupMember.createOwner(group, completedUser("first"))
    private val secondMember = GroupMember.createMember(group, completedUser("second"))
    private val thirdMember = GroupMember.createMember(group, completedUser("third"))
    private val meeting =
        Meeting.createFixed(
            group = group,
            creator = firstMember,
            name = "광주 여행",
            location = null,
            dateRange = MeetingDateRange(LocalDate.of(2026, 9, 23), LocalDate.of(2026, 9, 23)),
            confirmedAt = Instant.parse("2026-09-20T00:00:00Z"),
            currentDate = LocalDate.of(2026, 9, 20),
        )
    private val first = MeetingParticipant.create(meeting, firstMember)
    private val second = MeetingParticipant.create(meeting, secondMember)
    private val third = MeetingParticipant.create(meeting, thirdMember)

    @Test
    fun `같은 방향의 부담 금액을 상계하면 정산 금액에 더한다`() {
        val request = pendingRequest(debtor = first, creditor = second, amount = 3_000L)

        request.offset(debtor = first, creditor = second, debtAmount = BigDecimal.valueOf(2_000L), offsetAt = OFFSET_AT)

        assertSame(first, request.sender)
        assertSame(second, request.receiver)
        assertEquals(BigDecimal.valueOf(5_000L), request.amount)
    }

    @Test
    fun `반대 방향의 부담 금액이 더 작으면 방향을 유지하고 차감한다`() {
        val request = pendingRequest(debtor = first, creditor = second, amount = 5_000L)

        request.offset(debtor = second, creditor = first, debtAmount = BigDecimal.valueOf(2_000L), offsetAt = OFFSET_AT)

        assertSame(first, request.sender)
        assertEquals(BigDecimal.valueOf(3_000L), request.amount)
        assertEquals(SettlementRequestStatus.PENDING, request.status)
    }

    @Test
    fun `상계로 금액이 바뀌면 리마인드 대기 시간을 초기화한다`() {
        val request = pendingRequest(debtor = first, creditor = second, amount = 5_000L)
        request.remind(OFFSET_AT)

        request.offset(debtor = first, creditor = second, debtAmount = BigDecimal.valueOf(1_000L), offsetAt = OFFSET_AT)

        assertNull(request.lastRemindedAt)
    }

    @Test
    fun `정산 요청의 두 참여자가 아닌 부담 금액은 상계할 수 없다`() {
        val request = pendingRequest(debtor = first, creditor = second, amount = 5_000L)

        assertFailsWith<IllegalArgumentException> {
            request.offset(debtor = third, creditor = second, debtAmount = BigDecimal.valueOf(1_000L), offsetAt = OFFSET_AT)
        }
    }

    @Test
    fun `완료된 정산 요청에는 상계할 수 없다`() {
        val request = pendingRequest(debtor = first, creditor = second, amount = 5_000L)
        request.complete(OFFSET_AT)

        assertFailsWith<IllegalStateException> {
            request.offset(debtor = first, creditor = second, debtAmount = BigDecimal.valueOf(1_000L), offsetAt = OFFSET_AT)
        }
    }

    private fun pendingRequest(
        debtor: MeetingParticipant,
        creditor: MeetingParticipant,
        amount: Long,
    ): SettlementRequest =
        SettlementRequest.create(
            meeting = meeting,
            currency = BillCurrency.KRW,
            debtor = debtor,
            creditor = creditor,
            debtAmount = BigDecimal.valueOf(amount),
        )

    private fun completedUser(providerUserId: String): User =
        User
            .createSocialUser(
                email = "$providerUserId@example.com",
                provider = SocialProvider.GOOGLE,
                providerUserId = providerUserId,
            ).also { user ->
                user.completeOnboarding(
                    profileImageObjectKey = null,
                    nickname = providerUserId,
                    lastName = "홍",
                    firstName = "길동",
                    bankAccount = BankAccount.create(bank = Bank.KB_KOOKMIN, accountNumber = "123456789012"),
                    completedAt = Instant.parse("2026-09-20T00:00:00Z"),
                )
            }

    companion object {
        private val OFFSET_AT: Instant = Instant.parse("2026-09-23T03:00:00Z")
    }
}
