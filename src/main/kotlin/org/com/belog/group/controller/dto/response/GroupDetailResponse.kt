package org.com.belog.group.controller.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import org.com.belog.group.service.result.ActiveMeetingResult
import org.com.belog.group.service.result.GroupDetailResult
import org.com.belog.group.service.result.GroupMeetingParticipantResult
import org.com.belog.group.service.result.SchedulingMeetingResult
import java.time.LocalDate

@Schema(description = "그룹 상세 조회 결과")
data class GroupDetailResponse(
    @field:Schema(description = "그룹 ID", example = "1")
    val groupId: Long,
    @field:Schema(description = "그룹명", example = "피놀리와 기니휘기")
    val name: String,
    @field:Schema(description = "그룹 커버 이미지 조회 URL. 이미지가 없으면 null", nullable = true)
    val coverImageUrl: String?,
    @field:Schema(description = "그룹 초대 코드", example = "QCRJNN")
    val inviteCode: String,
    @field:Schema(description = "현재 그룹 멤버 수", example = "15")
    val memberCount: Int,
    @field:Schema(description = "로그인 사용자의 커버 이미지 변경 권한", example = "true")
    val canEditCoverImage: Boolean,
    @field:Schema(description = "로그인 사용자의 그룹 삭제 권한", example = "true")
    val canDeleteGroup: Boolean,
    @field:Schema(description = "일정 조율 중인 만남 목록")
    val schedulingMeetings: List<SchedulingMeetingResponse>,
    @field:Schema(description = "종료되지 않은 확정 만남 목록")
    val activeMeetings: List<ActiveMeetingResponse>,
) {
    companion object {
        fun from(result: GroupDetailResult): GroupDetailResponse =
            GroupDetailResponse(
                groupId = result.groupId,
                name = result.name,
                coverImageUrl = result.coverImageUrl,
                inviteCode = result.inviteCode,
                memberCount = result.memberCount,
                canEditCoverImage = result.canEditCoverImage,
                canDeleteGroup = result.canDeleteGroup,
                schedulingMeetings = result.schedulingMeetings.map(SchedulingMeetingResponse::from),
                activeMeetings = result.activeMeetings.map(ActiveMeetingResponse::from),
            )
    }
}

@Schema(description = "일정 조율 중인 만남")
data class SchedulingMeetingResponse(
    @field:Schema(description = "만남 ID", example = "10")
    val meetingId: Long,
    @field:Schema(description = "만남명", example = "1박 2일 광주 여행")
    val name: String,
    @field:Schema(description = "로그인 사용자의 만남장 여부", example = "true")
    val isMeetingOwner: Boolean,
    @field:Schema(description = "참여자 목록. 현재 만남장 우선, 나머지는 참여자 ID 오름차순")
    val participants: List<GroupMeetingParticipantResponse>,
    @field:Schema(description = "만남 참여자 수", example = "3")
    val participantCount: Int,
) {
    companion object {
        fun from(result: SchedulingMeetingResult): SchedulingMeetingResponse =
            SchedulingMeetingResponse(
                meetingId = result.meetingId,
                name = result.name,
                isMeetingOwner = result.isMeetingOwner,
                participants = result.participants.map(GroupMeetingParticipantResponse::from),
                participantCount = result.participantCount,
            )
    }
}

@Schema(description = "종료되지 않은 확정 만남")
data class ActiveMeetingResponse(
    @field:Schema(description = "만남 ID", example = "11")
    val meetingId: Long,
    @field:Schema(description = "만남명", example = "여름 부산 여행")
    val name: String,
    @field:Schema(description = "로그인 사용자의 만남장 여부", example = "false")
    val isMeetingOwner: Boolean,
    @field:Schema(description = "시작일", example = "2026-09-25")
    val startDate: LocalDate,
    @field:Schema(description = "종료일", example = "2026-09-26")
    val endDate: LocalDate,
) {
    companion object {
        fun from(result: ActiveMeetingResult): ActiveMeetingResponse =
            ActiveMeetingResponse(
                meetingId = result.meetingId,
                name = result.name,
                isMeetingOwner = result.isMeetingOwner,
                startDate = result.startDate,
                endDate = result.endDate,
            )
    }
}

@Schema(description = "그룹 조회의 만남 참여자")
data class GroupMeetingParticipantResponse(
    @field:Schema(description = "참여자 표시 닉네임", example = "이정원")
    val nickname: String,
    @field:Schema(description = "해당 참여자의 현재 만남장 여부", example = "true")
    val isMeetingOwner: Boolean,
) {
    companion object {
        fun from(result: GroupMeetingParticipantResult): GroupMeetingParticipantResponse =
            GroupMeetingParticipantResponse(
                nickname = result.nickname,
                isMeetingOwner = result.isMeetingOwner,
            )
    }
}
