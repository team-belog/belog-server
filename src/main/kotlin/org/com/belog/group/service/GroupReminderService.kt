package org.com.belog.group.service

import org.com.belog.group.repository.GroupMemberRepository
import org.com.belog.group.repository.GroupRepository
import org.com.belog.meeting.domain.MeetingStatus
import org.com.belog.meeting.repository.MeetingRepository
import org.com.belog.notification.service.NotificationService
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class GroupReminderService(
    private val groupRepository: GroupRepository,
    private val groupMemberRepository: GroupMemberRepository,
    private val meetingRepository: MeetingRepository,
    private val notificationService: NotificationService,
) {
    @Transactional(readOnly = true)
    fun findInactiveGroupIds(
        currentDate: LocalDate,
        cursor: Long?,
        size: Int,
    ): List<Long> {
        require(size > 0) { "리마인드 대상 조회 개수는 1개 이상이어야 합니다." }

        return meetingRepository.findInactiveGroupIds(
            lastEndDate = currentDate.minusDays(GROUP_INACTIVE_DAYS),
            cursor = cursor,
            confirmedStatus = MeetingStatus.CONFIRMED,
            schedulingStatus = MeetingStatus.SCHEDULING,
            pageable = PageRequest.of(0, size),
        )
    }

    @Transactional
    fun remindInactiveGroup(
        groupId: Long,
        currentDate: LocalDate,
    ) {
        groupRepository.findActiveById(groupId) ?: return
        val lastMeeting =
            meetingRepository
                .findLatestEndedMeetings(groupId, currentDate, MeetingStatus.CONFIRMED, PageRequest.of(0, 1))
                .firstOrNull() ?: return
        if (!lastMeeting.isEndedOnOrBefore(currentDate.minusDays(GROUP_INACTIVE_DAYS)) || hasNewMeeting(groupId, currentDate)) {
            return
        }

        val lastMeetingId = checkNotNull(lastMeeting.id) { "마지막 만남의 ID가 없습니다." }
        val commands =
            groupMemberRepository.findAllWithUserByGroupId(groupId).map { member ->
                GroupNotificationCommands.groupInactive60Days(
                    groupId = groupId,
                    lastMeetingId = lastMeetingId,
                    recipientUserId = checkNotNull(member.user.id) { "그룹 멤버의 사용자 ID가 없습니다." },
                )
            }
        notificationService.createAll(commands)
    }

    private fun hasNewMeeting(
        groupId: Long,
        currentDate: LocalDate,
    ): Boolean =
        meetingRepository.findSchedulingMeetings(groupId, MeetingStatus.SCHEDULING).isNotEmpty() ||
            meetingRepository.findActiveMeetings(groupId, currentDate, MeetingStatus.CONFIRMED).isNotEmpty()

    companion object {
        private const val GROUP_INACTIVE_DAYS = 60L
    }
}
