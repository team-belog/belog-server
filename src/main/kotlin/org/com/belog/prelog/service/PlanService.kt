package org.com.belog.prelog.service

import org.com.belog.global.error.BusinessException
import org.com.belog.group.code.GroupErrorCode
import org.com.belog.group.domain.GroupMember
import org.com.belog.group.repository.GroupMemberRepository
import org.com.belog.meeting.code.MeetingErrorCode
import org.com.belog.meeting.domain.Meeting
import org.com.belog.meeting.repository.MeetingRepository
import org.com.belog.prelog.code.PreLogErrorCode
import org.com.belog.prelog.domain.Plan
import org.com.belog.prelog.domain.PlanCategory
import org.com.belog.prelog.repository.PlanRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDate

@Service
class PlanService(
    private val meetingRepository: MeetingRepository,
    private val groupMemberRepository: GroupMemberRepository,
    private val planRepository: PlanRepository,
    private val clock: Clock,
) {
    @Transactional
    fun createLinkPlan(
        meetingId: Long,
        creatorUserId: Long,
        category: PlanCategory,
        title: String,
        url: String,
    ): Plan {
        val meeting = findMeeting(meetingId)
        val creator = findCreator(meeting, creatorUserId)
        val currentDate = LocalDate.now(clock)
        validateMeetingNotEnded(meeting, currentDate)

        return savePlan {
            Plan.createLink(
                meeting = meeting,
                creator = creator,
                category = category,
                title = title,
                url = url,
                currentDate = currentDate,
            )
        }
    }

    @Transactional
    fun createMemoPlan(
        meetingId: Long,
        creatorUserId: Long,
        category: PlanCategory,
        title: String,
        content: String,
    ): Plan {
        val meeting = findMeeting(meetingId)
        val creator = findCreator(meeting, creatorUserId)
        val currentDate = LocalDate.now(clock)
        validateMeetingNotEnded(meeting, currentDate)

        return savePlan {
            Plan.createMemo(
                meeting = meeting,
                creator = creator,
                category = category,
                title = title,
                content = content,
                currentDate = currentDate,
            )
        }
    }

    private fun findMeeting(meetingId: Long): Meeting =
        meetingRepository.findById(meetingId).orElseThrow {
            BusinessException(MeetingErrorCode.MEETING_NOT_FOUND)
        }

    private fun findCreator(
        meeting: Meeting,
        creatorUserId: Long,
    ): GroupMember {
        val groupId = checkNotNull(meeting.group.id) { "계획 대상 만남의 그룹 ID가 없습니다." }
        return groupMemberRepository.findByGroupIdAndUserId(groupId, creatorUserId)
            ?: throw BusinessException(GroupErrorCode.NOT_GROUP_MEMBER)
    }

    private fun validateMeetingNotEnded(
        meeting: Meeting,
        currentDate: LocalDate,
    ) {
        if (meeting.endDate?.isBefore(currentDate) == true) {
            throw BusinessException(PreLogErrorCode.MEETING_ALREADY_ENDED)
        }
    }

    private fun savePlan(createPlan: () -> Plan): Plan {
        val plan =
            try {
                createPlan()
            } catch (exception: IllegalArgumentException) {
                throw BusinessException(PreLogErrorCode.INVALID_PLAN, exception)
            }

        return planRepository.save(plan)
    }
}
