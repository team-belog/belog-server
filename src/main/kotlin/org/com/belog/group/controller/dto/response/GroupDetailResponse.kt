package org.com.belog.group.controller.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import org.com.belog.group.service.result.ActiveMeetingResult
import org.com.belog.group.service.result.GroupDetailResult
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
    @field:Schema(description = "참여자 닉네임 목록", example = "[\"이정원\", \"정다빈\", \"김성연\"]")
    val participantNicknames: List<String>,
    @field:Schema(description = "만남 참여자 수", example = "3")
    val participantCount: Int,
) {
    companion object {
        fun from(result: SchedulingMeetingResult): SchedulingMeetingResponse =
            SchedulingMeetingResponse(
                meetingId = result.meetingId,
                name = result.name,
                participantNicknames = result.participantNicknames,
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
                startDate = result.startDate,
                endDate = result.endDate,
            )
    }
}
