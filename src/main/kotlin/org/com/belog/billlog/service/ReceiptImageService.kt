package org.com.belog.billlog.service

import org.com.belog.billlog.code.BillLogErrorCode
import org.com.belog.billlog.domain.ReceiptImageFormat
import org.com.belog.billlog.domain.ReceiptImageUpload
import org.com.belog.billlog.infrastructure.S3ReceiptImageUploadUrlProvider
import org.com.belog.global.error.BusinessException
import org.com.belog.group.code.GroupErrorCode
import org.com.belog.group.repository.GroupMemberRepository
import org.com.belog.meeting.code.MeetingErrorCode
import org.com.belog.meeting.repository.MeetingRepository
import org.springframework.stereotype.Service

@Service
class ReceiptImageService(
    private val meetingRepository: MeetingRepository,
    private val groupMemberRepository: GroupMemberRepository,
    private val uploadUrlProvider: S3ReceiptImageUploadUrlProvider,
) {
    fun issueUploadUrl(
        meetingId: Long,
        userId: Long,
        contentType: String,
        fileSize: Long,
    ): ReceiptImageUpload {
        val format =
            ReceiptImageFormat.fromContentType(contentType)
                ?: throw BusinessException(BillLogErrorCode.UNSUPPORTED_RECEIPT_IMAGE_TYPE)

        if (fileSize !in 1..ReceiptImageFormat.MAX_FILE_SIZE_BYTES) {
            throw BusinessException(BillLogErrorCode.INVALID_RECEIPT_IMAGE_SIZE)
        }

        val meeting =
            meetingRepository.findByIdWithGroup(meetingId)
                ?: throw BusinessException(MeetingErrorCode.MEETING_NOT_FOUND)
        val groupId = checkNotNull(meeting.group.id) { "Bill-log 대상 만남의 그룹 ID가 없습니다." }

        if (!groupMemberRepository.existsByGroupIdAndUserId(groupId, userId)) {
            throw BusinessException(GroupErrorCode.NOT_GROUP_MEMBER)
        }

        return uploadUrlProvider.issueUploadUrl(
            meetingId = meetingId,
            userId = userId,
            format = format,
            fileSize = fileSize,
        )
    }
}
