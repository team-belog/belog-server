package org.com.belog.prelog.service

import org.com.belog.global.error.BusinessException
import org.com.belog.meeting.code.MeetingErrorCode
import org.com.belog.meeting.domain.MeetingParticipant
import org.com.belog.meeting.repository.MeetingParticipantRepository
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
    private val meetingParticipantRepository: MeetingParticipantRepository,
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
        val creator = findCreator(meetingId, creatorUserId)
        val currentDate = LocalDate.now(clock)
        validateMeetingNotEnded(creator, currentDate)

        return savePlan {
            Plan.createLink(
                meeting = creator.meeting,
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
        val creator = findCreator(meetingId, creatorUserId)
        val currentDate = LocalDate.now(clock)
        validateMeetingNotEnded(creator, currentDate)

        return savePlan {
            Plan.createMemo(
                meeting = creator.meeting,
                creator = creator,
                category = category,
                title = title,
                content = content,
                currentDate = currentDate,
            )
        }
    }

    private fun findCreator(
        meetingId: Long,
        creatorUserId: Long,
    ): MeetingParticipant =
        meetingParticipantRepository.findByMeetingIdAndGroupMemberUserId(meetingId, creatorUserId)
            ?: throwCreatorLookupException(meetingId)

    private fun throwCreatorLookupException(meetingId: Long): Nothing {
        if (!meetingRepository.existsById(meetingId)) {
            throw BusinessException(MeetingErrorCode.MEETING_NOT_FOUND)
        }
        throw BusinessException(MeetingErrorCode.NOT_MEETING_PARTICIPANT)
    }

    private fun validateMeetingNotEnded(
        creator: MeetingParticipant,
        currentDate: LocalDate,
    ) {
        if (creator.meeting.endDate?.isBefore(currentDate) == true) {
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
