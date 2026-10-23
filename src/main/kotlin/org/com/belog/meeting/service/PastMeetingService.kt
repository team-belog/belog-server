package org.com.belog.meeting.service

import org.com.belog.global.error.BusinessException
import org.com.belog.global.time.currentBusinessDate
import org.com.belog.group.code.GroupErrorCode
import org.com.belog.group.repository.GroupMemberRepository
import org.com.belog.group.repository.GroupRepository
import org.com.belog.meeting.domain.MeetingStatus
import org.com.belog.meeting.repository.MeetingRepository
import org.com.belog.meeting.service.result.PastMeetingListResult
import org.com.belog.meeting.service.result.PastMeetingResult
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock

@Service
class PastMeetingService(
    private val groupRepository: GroupRepository,
    private val groupMemberRepository: GroupMemberRepository,
    private val meetingRepository: MeetingRepository,
    private val clock: Clock,
) {
    @Transactional(readOnly = true)
    fun getPastMeetings(
        groupId: Long,
        userId: Long,
        cursor: Long?,
        size: Int,
    ): PastMeetingListResult {
        require(size in MIN_PAST_MEETING_PAGE_SIZE..MAX_PAST_MEETING_PAGE_SIZE) {
            "지난 만남 조회 개수는 ${MIN_PAST_MEETING_PAGE_SIZE}개 이상 ${MAX_PAST_MEETING_PAGE_SIZE}개 이하여야 합니다."
        }
        validateGroupMember(groupId, userId)

        val meetings =
            meetingRepository.findPastMeetingPage(
                groupId = groupId,
                currentDate = clock.currentBusinessDate(),
                cursor = cursor,
                status = MeetingStatus.CONFIRMED,
                pageable = PageRequest.of(0, size + NEXT_PAGE_LOOKAHEAD_COUNT),
            )
        val hasNext = meetings.size > size
        val pageItems = meetings.take(size)

        return PastMeetingListResult(
            items =
                pageItems.map { meeting ->
                    PastMeetingResult(
                        meetingId = requireNotNull(meeting.id),
                        name = meeting.name,
                        startDate = requireNotNull(meeting.startDate),
                        endDate = requireNotNull(meeting.endDate),
                    )
                },
            nextCursor = pageItems.lastOrNull()?.id?.takeIf { hasNext },
            hasNext = hasNext,
        )
    }

    private fun validateGroupMember(
        groupId: Long,
        userId: Long,
    ) {
        if (!groupRepository.existsByIdAndDeletedAtIsNull(groupId)) {
            throw BusinessException(GroupErrorCode.GROUP_NOT_FOUND)
        }
        if (!groupMemberRepository.existsByGroupIdAndUserIdAndWithdrawnAtIsNull(groupId, userId)) {
            throw BusinessException(GroupErrorCode.NOT_GROUP_MEMBER)
        }
    }

    companion object {
        private const val MIN_PAST_MEETING_PAGE_SIZE = 1
        private const val MAX_PAST_MEETING_PAGE_SIZE = 50
        private const val NEXT_PAGE_LOOKAHEAD_COUNT = 1
    }
}
