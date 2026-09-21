package org.com.belog.meeting.controller.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import org.com.belog.meeting.service.result.CandidateDatePollResult
import org.com.belog.meeting.service.result.DatePollMemberResult
import org.com.belog.meeting.service.result.MeetingDatePollResults
import java.time.LocalDate

@Schema(description = "후보 일정 조율 현황")
data class MeetingDatePollResultsResponse(
    @field:Schema(description = "만남 ID", example = "7")
    val meetingId: Long,
    @field:Schema(description = "생성자를 포함한 전체 초대 인원", example = "5")
    val totalParticipantCount: Int,
    @field:Schema(description = "생성자를 포함한 응답 인원", example = "4")
    val respondedParticipantCount: Int,
    @field:Schema(description = "가능 인원 내림차순 후보 일정 결과")
    val candidateDateResults: List<CandidateDatePollResponse>,
) {
    companion object {
        fun from(result: MeetingDatePollResults): MeetingDatePollResultsResponse =
            MeetingDatePollResultsResponse(
                meetingId = result.meetingId,
                totalParticipantCount = result.totalParticipantCount,
                respondedParticipantCount = result.respondedParticipantCount,
                candidateDateResults = result.candidateDateResults.map(CandidateDatePollResponse::from),
            )
    }
}

@Schema(description = "후보 일정 조율 멤버")
data class DatePollMemberResponse(
    @field:Schema(description = "그룹 멤버 ID", example = "21")
    val groupMemberId: Long,
    @field:Schema(description = "닉네임", example = "참여자")
    val nickname: String,
) {
    companion object {
        fun from(result: DatePollMemberResult): DatePollMemberResponse =
            DatePollMemberResponse(
                groupMemberId = result.groupMemberId,
                nickname = result.nickname,
            )
    }
}

@Schema(description = "후보 일정별 조율 결과")
data class CandidateDatePollResponse(
    @field:Schema(description = "후보 일정 ID", example = "101")
    val candidateDateRangeId: Long,
    @field:Schema(description = "시작일", example = "2026-10-03")
    val startDate: LocalDate,
    @field:Schema(description = "종료일", example = "2026-10-04")
    val endDate: LocalDate,
    @field:Schema(description = "가능 인원 기준 순위", example = "1")
    val rank: Int,
    @field:Schema(description = "가능 인원", example = "3")
    val availableCount: Int,
    @field:Schema(description = "가능 멤버 목록")
    val availableMembers: List<DatePollMemberResponse>,
    @field:Schema(description = "응답했지만 해당 일정이 불가능한 멤버 목록")
    val unavailableMembers: List<DatePollMemberResponse>,
) {
    companion object {
        fun from(result: CandidateDatePollResult): CandidateDatePollResponse =
            CandidateDatePollResponse(
                candidateDateRangeId = result.candidateDateRangeId,
                startDate = result.startDate,
                endDate = result.endDate,
                rank = result.rank,
                availableCount = result.availableCount,
                availableMembers = result.availableMembers.map(DatePollMemberResponse::from),
                unavailableMembers = result.unavailableMembers.map(DatePollMemberResponse::from),
            )
    }
}
