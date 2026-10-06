package org.com.belog.billlog.service

import org.com.belog.billlog.code.BillLogErrorCode
import org.com.belog.billlog.infrastructure.S3ReceiptImageObjectVerifier
import org.com.belog.billlog.infrastructure.S3ReceiptImageUploadUrlProvider
import org.com.belog.global.error.BusinessException
import org.com.belog.global.storage.S3ObjectReadUrlProvider
import org.com.belog.group.code.GroupErrorCode
import org.com.belog.group.domain.Group
import org.com.belog.group.repository.GroupMemberRepository
import org.com.belog.meeting.domain.Meeting
import org.com.belog.meeting.repository.MeetingRepository
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ReceiptImageServiceTest {
    private val meetingRepository = mock(MeetingRepository::class.java)
    private val groupMemberRepository = mock(GroupMemberRepository::class.java)
    private val uploadUrlProvider = mock(S3ReceiptImageUploadUrlProvider::class.java)
    private val objectVerifier = mock(S3ReceiptImageObjectVerifier::class.java)
    private val readUrlProvider = mock(S3ObjectReadUrlProvider::class.java)
    private val service = ReceiptImageService(meetingRepository, groupMemberRepository, uploadUrlProvider, objectVerifier, readUrlProvider)

    @Test
    fun `그룹 멤버가 자신이 업로드한 영수증 이미지를 분석 대상으로 준비한다`() {
        givenMeetingGroup()
        `when`(groupMemberRepository.existsByGroupIdAndUserId(GROUP_ID, USER_ID)).thenReturn(true)
        `when`(readUrlProvider.generateReadUrl(OWN_OBJECT_KEY)).thenReturn("https://receipt.example/image.jpg")

        val source = service.prepareAnalysisSource(MEETING_ID, USER_ID, OWN_OBJECT_KEY)

        assertEquals(OWN_OBJECT_KEY, source.objectKey.value)
        assertEquals("https://receipt.example/image.jpg", source.readUrl)
    }

    @Test
    fun `비그룹 멤버는 영수증을 분석할 수 없다`() {
        givenMeetingGroup()

        val exception = assertFailsWith<BusinessException> { service.prepareAnalysisSource(MEETING_ID, USER_ID, OWN_OBJECT_KEY) }

        assertEquals(GroupErrorCode.NOT_GROUP_MEMBER, exception.errorCode)
        verifyNoInteractions(objectVerifier, readUrlProvider)
    }

    @Test
    fun `그룹 멤버도 다른 사용자의 영수증을 분석할 수 없다`() {
        givenMeetingGroup()
        `when`(groupMemberRepository.existsByGroupIdAndUserId(GROUP_ID, USER_ID)).thenReturn(true)

        val exception = assertFailsWith<BusinessException> { service.prepareAnalysisSource(MEETING_ID, USER_ID, OTHER_USER_OBJECT_KEY) }

        assertEquals(BillLogErrorCode.INVALID_RECEIPT_IMAGE_OBJECT_KEY, exception.errorCode)
        verifyNoInteractions(objectVerifier, readUrlProvider)
    }

    private fun givenMeetingGroup() {
        val meeting = mock(Meeting::class.java)
        val group = mock(Group::class.java)
        `when`(meeting.group).thenReturn(group)
        `when`(group.id).thenReturn(GROUP_ID)
        `when`(meetingRepository.findByIdWithGroup(MEETING_ID)).thenReturn(meeting)
    }

    companion object {
        private const val MEETING_ID = 7L
        private const val GROUP_ID = 3L
        private const val USER_ID = 15L
        private const val OWN_OBJECT_KEY = "bill-log/receipts/7/15/550e8400-e29b-41d4-a716-446655440000.jpg"
        private const val OTHER_USER_OBJECT_KEY = "bill-log/receipts/7/99/550e8400-e29b-41d4-a716-446655440000.jpg"
    }
}
