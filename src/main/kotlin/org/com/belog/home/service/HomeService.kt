package org.com.belog.home.service

import org.com.belog.global.time.currentBusinessDate
import org.com.belog.home.domain.HomeMeetingProgressStatus
import org.com.belog.home.repository.HomeActiveMeetingRepository
import org.com.belog.home.repository.HomeCalendarRepository
import org.com.belog.home.service.query.ActiveMeetingCursor
import org.com.belog.home.service.query.ActiveMeetingCursorType
import org.com.belog.home.service.result.ActiveMeetingListResult
import org.com.belog.home.service.result.ActiveMeetingParticipantResult
import org.com.belog.home.service.result.ActiveMeetingResult
import org.com.belog.home.service.result.HomeCalendarMeetingResult
import org.com.belog.home.service.result.HomeCalendarResult
import org.com.belog.meeting.domain.Meeting
import org.com.belog.meeting.domain.MeetingStatus
import org.com.belog.meeting.repository.MeetingParticipantRepository
import org.com.belog.user.service.UserService
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.YearMonth

@Service
class HomeService(
    private val homeCalendarRepository: HomeCalendarRepository,
    private val homeActiveMeetingRepository: HomeActiveMeetingRepository,
    private val meetingParticipantRepository: MeetingParticipantRepository,
    private val userService: UserService,
    private val clock: Clock,
) {
    @Transactional(readOnly = true)
    fun getCalendar(
        userId: Long,
        yearMonth: YearMonth,
    ): HomeCalendarResult {
        val meetings =
            homeCalendarRepository.findMeetingsOverlappingMonth(
                userId = userId,
                status = MeetingStatus.CONFIRMED,
                monthStart = yearMonth.atDay(1),
                monthEnd = yearMonth.atEndOfMonth(),
            )

        return HomeCalendarResult(
            yearMonth = yearMonth,
            today = clock.currentBusinessDate(),
            meetings =
                meetings.map { meeting ->
                    HomeCalendarMeetingResult(
                        meetingId = requireNotNull(meeting.id),
                        startDate = requireNotNull(meeting.startDate),
                        endDate = requireNotNull(meeting.endDate),
                    )
                },
        )
    }

    @Transactional(readOnly = true)
    fun getActiveMeetings(
        userId: Long,
        query: String?,
        cursor: ActiveMeetingCursor?,
        size: Int,
    ): ActiveMeetingListResult {
        require(size in MIN_ACTIVE_MEETING_PAGE_SIZE..MAX_ACTIVE_MEETING_PAGE_SIZE) {
            "진행 중인 만남 조회 개수는 ${MIN_ACTIVE_MEETING_PAGE_SIZE}개 이상 " +
                "${MAX_ACTIVE_MEETING_PAGE_SIZE}개 이하여야 합니다."
        }

        val currentDate = clock.currentBusinessDate()
        val normalizedQuery = query?.trim().orEmpty()
        val pageable = PageRequest.of(0, size + NEXT_PAGE_LOOKAHEAD_COUNT)
        val meetings =
            if (cursor == null) {
                homeActiveMeetingRepository.findActivePage(
                    userId = userId,
                    schedulingStatus = MeetingStatus.SCHEDULING,
                    confirmedStatus = MeetingStatus.CONFIRMED,
                    currentDate = currentDate,
                    query = normalizedQuery,
                    pageable = pageable,
                )
            } else {
                when (cursor.type) {
                    ActiveMeetingCursorType.SCHEDULING ->
                        homeActiveMeetingRepository.findActivePageAfterScheduling(
                            userId = userId,
                            schedulingStatus = MeetingStatus.SCHEDULING,
                            confirmedStatus = MeetingStatus.CONFIRMED,
                            currentDate = currentDate,
                            query = normalizedQuery,
                            cursorMeetingId = cursor.meetingId,
                            pageable = pageable,
                        )

                    ActiveMeetingCursorType.CONFIRMED ->
                        homeActiveMeetingRepository.findActivePageAfterConfirmed(
                            userId = userId,
                            confirmedStatus = MeetingStatus.CONFIRMED,
                            currentDate = currentDate,
                            query = normalizedQuery,
                            cursorStartDate = requireNotNull(cursor.startDate),
                            cursorMeetingId = cursor.meetingId,
                            pageable = pageable,
                        )
                }
            }
        val hasNext = meetings.size > size
        val pageItems = meetings.take(size)
        val participantsByMeetingId = findParticipantsByMeetingId(pageItems)

        return ActiveMeetingListResult(
            items =
                pageItems.map { meeting ->
                    val meetingId = requireNotNull(meeting.id)
                    val startDate = meeting.startDate
                    val participants = participantsByMeetingId[meetingId].orEmpty()

                    ActiveMeetingResult(
                        meetingId = meetingId,
                        name = meeting.name,
                        startDate = startDate,
                        endDate = meeting.endDate,
                        groupName = meeting.group.name,
                        progressStatus =
                            when (meeting.status) {
                                MeetingStatus.SCHEDULING -> HomeMeetingProgressStatus.SCHEDULING
                                MeetingStatus.CONFIRMED ->
                                    if (currentDate.isBefore(requireNotNull(startDate))) {
                                        HomeMeetingProgressStatus.UPCOMING
                                    } else {
                                        HomeMeetingProgressStatus.IN_PROGRESS
                                    }
                            },
                        participantCount = participants.size,
                        previewParticipants =
                            participants.take(PREVIEW_PARTICIPANT_COUNT).map { participant ->
                                val groupMember = participant.groupMember
                                ActiveMeetingParticipantResult(
                                    groupMemberId = requireNotNull(groupMember.id),
                                    nickname = requireNotNull(groupMember.user.nickname),
                                    profileImageUrl = userService.resolveProfileImageUrl(groupMember.user),
                                )
                            },
                    )
                },
            nextCursor =
                pageItems
                    .lastOrNull()
                    ?.takeIf { hasNext }
                    ?.let { meeting ->
                        ActiveMeetingCursor(
                            type =
                                when (meeting.status) {
                                    MeetingStatus.SCHEDULING -> ActiveMeetingCursorType.SCHEDULING
                                    MeetingStatus.CONFIRMED -> ActiveMeetingCursorType.CONFIRMED
                                },
                            startDate = meeting.startDate,
                            meetingId = requireNotNull(meeting.id),
                        )
                    },
            hasNext = hasNext,
        )
    }

    private fun findParticipantsByMeetingId(meetings: List<Meeting>) =
        if (meetings.isEmpty()) {
            emptyMap()
        } else {
            meetingParticipantRepository
                .findAllWithUserByMeetingIdIn(meetings.map { meeting -> requireNotNull(meeting.id) })
                .groupBy { participant -> requireNotNull(participant.meeting.id) }
        }

    companion object {
        private const val MIN_ACTIVE_MEETING_PAGE_SIZE = 1
        private const val MAX_ACTIVE_MEETING_PAGE_SIZE = 50
        private const val NEXT_PAGE_LOOKAHEAD_COUNT = 1
        private const val PREVIEW_PARTICIPANT_COUNT = 3
    }
}
