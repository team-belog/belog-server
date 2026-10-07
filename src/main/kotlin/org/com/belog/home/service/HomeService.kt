package org.com.belog.home.service

import org.com.belog.global.storage.S3ObjectReadUrlProvider
import org.com.belog.global.time.currentBusinessDate
import org.com.belog.home.domain.HomeMeetingProgressStatus
import org.com.belog.home.repository.HomeActiveMeetingRepository
import org.com.belog.home.repository.HomeCalendarRepository
import org.com.belog.home.repository.HomeCompletedMeetingRepository
import org.com.belog.home.service.query.ActiveMeetingCursor
import org.com.belog.home.service.query.ActiveMeetingCursorType
import org.com.belog.home.service.query.CompletedMeetingCursor
import org.com.belog.home.service.result.ActiveMeetingListResult
import org.com.belog.home.service.result.ActiveMeetingParticipantResult
import org.com.belog.home.service.result.ActiveMeetingResult
import org.com.belog.home.service.result.CompletedMeetingListResult
import org.com.belog.home.service.result.CompletedMeetingParticipantResult
import org.com.belog.home.service.result.CompletedMeetingResult
import org.com.belog.home.service.result.HomeCalendarMeetingResult
import org.com.belog.home.service.result.HomeCalendarResult
import org.com.belog.meeting.domain.MeetingStatus
import org.com.belog.meeting.repository.MeetingParticipantRepository
import org.com.belog.notification.service.NotificationQueryService
import org.com.belog.postlog.domain.PostLogTicket
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
    private val homeCompletedMeetingRepository: HomeCompletedMeetingRepository,
    private val meetingParticipantRepository: MeetingParticipantRepository,
    private val objectReadUrlProvider: S3ObjectReadUrlProvider,
    private val userService: UserService,
    private val notificationQueryService: NotificationQueryService,
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
            hasUnreadNotification = notificationQueryService.hasUnreadNotification(userId),
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
        val participantsByMeetingId = findParticipantsByMeetingId(pageItems.map { meeting -> requireNotNull(meeting.id) })

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

    @Transactional(readOnly = true)
    fun getCompletedMeetings(
        userId: Long,
        cursor: CompletedMeetingCursor?,
        size: Int,
    ): CompletedMeetingListResult {
        require(size in MIN_COMPLETED_MEETING_PAGE_SIZE..MAX_COMPLETED_MEETING_PAGE_SIZE) {
            "종료된 만남 조회 개수는 ${MIN_COMPLETED_MEETING_PAGE_SIZE}개 이상 " +
                "${MAX_COMPLETED_MEETING_PAGE_SIZE}개 이하여야 합니다."
        }

        val currentDate = clock.currentBusinessDate()
        val pageable = PageRequest.of(0, size + NEXT_PAGE_LOOKAHEAD_COUNT)
        val tickets =
            if (cursor == null) {
                homeCompletedMeetingRepository.findCompletedPage(
                    userId = userId,
                    currentDate = currentDate,
                    pageable = pageable,
                )
            } else {
                homeCompletedMeetingRepository.findCompletedPageAfter(
                    userId = userId,
                    currentDate = currentDate,
                    cursorEndDate = cursor.endDate,
                    cursorTicketId = cursor.ticketId,
                    pageable = pageable,
                )
            }
        val hasNext = tickets.size > size
        val pageItems = tickets.take(size)
        val participantsByMeetingId = findParticipantsByMeetingId(pageItems.map(PostLogTicket::sourceMeetingId))

        return CompletedMeetingListResult(
            items =
                pageItems.map { ticket ->
                    val participants = participantsByMeetingId[ticket.sourceMeetingId].orEmpty()

                    CompletedMeetingResult(
                        ticketId = requireNotNull(ticket.id),
                        name = ticket.meetingName,
                        memory = ticket.memory,
                        coverPhotoUrl = ticket.coverImageObjectKey?.let(objectReadUrlProvider::generateReadUrl),
                        startDate = requireNotNull(ticket.meetingStartDate),
                        endDate = requireNotNull(ticket.meetingEndDate),
                        location = ticket.meetingLocation,
                        participantCount = participants.size,
                        participants =
                            participants.map { participant ->
                                val groupMember = participant.groupMember
                                CompletedMeetingParticipantResult(
                                    groupMemberId = requireNotNull(groupMember.id),
                                    nickname = requireNotNull(groupMember.user.nickname),
                                )
                            },
                    )
                },
            nextCursor =
                pageItems
                    .lastOrNull()
                    ?.takeIf { hasNext }
                    ?.let { ticket ->
                        CompletedMeetingCursor(
                            endDate = requireNotNull(ticket.meetingEndDate),
                            ticketId = requireNotNull(ticket.id),
                        )
                    },
            hasNext = hasNext,
        )
    }

    private fun findParticipantsByMeetingId(meetingIds: List<Long>) =
        if (meetingIds.isEmpty()) {
            emptyMap()
        } else {
            meetingParticipantRepository
                .findAllWithUserByMeetingIdIn(meetingIds)
                .groupBy { participant -> requireNotNull(participant.meeting.id) }
        }

    companion object {
        private const val MIN_ACTIVE_MEETING_PAGE_SIZE = 1
        private const val MAX_ACTIVE_MEETING_PAGE_SIZE = 50
        private const val MIN_COMPLETED_MEETING_PAGE_SIZE = 1
        private const val MAX_COMPLETED_MEETING_PAGE_SIZE = 50
        private const val NEXT_PAGE_LOOKAHEAD_COUNT = 1
        private const val PREVIEW_PARTICIPANT_COUNT = 3
    }
}
