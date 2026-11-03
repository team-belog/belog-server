package org.com.belog.home.controller.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import org.com.belog.home.controller.cursor.ActiveMeetingCursorCodec
import org.com.belog.home.domain.HomeMeetingProgressStatus
import org.com.belog.home.service.result.ActiveMeetingListResult
import org.com.belog.home.service.result.ActiveMeetingParticipantResult
import org.com.belog.home.service.result.ActiveMeetingResult
import java.time.LocalDate

@Schema(description = "진행 중인 만남 목록 조회 결과")
data class ActiveMeetingListResponse(
    @field:Schema(description = "진행 중인 만남 목록")
    val items: List<ActiveMeetingResponse>,
    @field:Schema(description = "다음 페이지 커서. 다음 페이지가 없으면 null", nullable = true)
    val nextCursor: String?,
    @field:Schema(description = "다음 페이지 존재 여부", example = "true")
    val hasNext: Boolean,
) {
    companion object {
        fun from(result: ActiveMeetingListResult): ActiveMeetingListResponse =
            ActiveMeetingListResponse(
                items = result.items.map(ActiveMeetingResponse::from),
                nextCursor = result.nextCursor?.let(ActiveMeetingCursorCodec::encode),
                hasNext = result.hasNext,
            )
    }
}

@Schema(description = "진행 중인 만남")
data class ActiveMeetingResponse(
    @field:Schema(description = "만남 ID", example = "11")
    val meetingId: Long,
    @field:Schema(description = "만남명", example = "1박 2일 광주 여행")
    val name: String,
    @field:Schema(description = "시작일. 일정 조율 중에는 null", example = "2026-08-20", nullable = true)
    val startDate: LocalDate?,
    @field:Schema(description = "종료일. 일정 조율 중에는 null", example = "2026-08-21", nullable = true)
    val endDate: LocalDate?,
    @field:Schema(description = "그룹명", example = "피놀리와 기니휘기")
    val groupName: String,
    @field:Schema(description = "만남 진행 상태. SCHEDULING, UPCOMING, IN_PROGRESS 중 하나", example = "UPCOMING")
    val progressStatus: HomeMeetingProgressStatus,
    @field:Schema(description = "전체 참여자 수", example = "3")
    val participantCount: Int,
    @field:Schema(description = "최대 3명의 참여자 미리보기")
    val previewParticipants: List<ActiveMeetingParticipantResponse>,
) {
    companion object {
        fun from(result: ActiveMeetingResult): ActiveMeetingResponse =
            ActiveMeetingResponse(
                meetingId = result.meetingId,
                name = result.name,
                startDate = result.startDate,
                endDate = result.endDate,
                groupName = result.groupName,
                progressStatus = result.progressStatus,
                participantCount = result.participantCount,
                previewParticipants = result.previewParticipants.map(ActiveMeetingParticipantResponse::from),
            )
    }
}

@Schema(description = "진행 중인 만남 참여자 미리보기")
data class ActiveMeetingParticipantResponse(
    @field:Schema(description = "그룹 멤버 ID", example = "21")
    val groupMemberId: Long,
    @field:Schema(description = "닉네임", example = "이정원")
    val nickname: String,
    @field:Schema(description = "프로필 이미지 조회 URL. 이미지가 없으면 null", nullable = true)
    val profileImageUrl: String?,
) {
    companion object {
        fun from(result: ActiveMeetingParticipantResult): ActiveMeetingParticipantResponse =
            ActiveMeetingParticipantResponse(
                groupMemberId = result.groupMemberId,
                nickname = result.nickname,
                profileImageUrl = result.profileImageUrl,
            )
    }
}
