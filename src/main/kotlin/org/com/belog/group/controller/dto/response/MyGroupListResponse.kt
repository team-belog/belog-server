package org.com.belog.group.controller.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import org.com.belog.group.controller.cursor.GroupListCursorCodec
import org.com.belog.group.service.result.GroupPreviewMemberResult
import org.com.belog.group.service.result.MyGroupListResult
import org.com.belog.group.service.result.MyGroupSummaryResult

@Schema(description = "내 그룹 목록 조회 결과")
data class MyGroupListResponse(
    @field:Schema(description = "내 그룹 목록")
    val items: List<MyGroupResponse>,
    @field:Schema(description = "다음 페이지 커서. 다음 페이지가 없으면 null", nullable = true)
    val nextCursor: String?,
    @field:Schema(description = "다음 페이지 존재 여부", example = "true")
    val hasNext: Boolean,
) {
    companion object {
        fun from(result: MyGroupListResult): MyGroupListResponse =
            MyGroupListResponse(
                items = result.items.map(MyGroupResponse::from),
                nextCursor = result.nextCursor?.let(GroupListCursorCodec::encode),
                hasNext = result.hasNext,
            )
    }
}

@Schema(description = "내 그룹 요약")
data class MyGroupResponse(
    @field:Schema(description = "그룹 ID", example = "1")
    val groupId: Long,
    @field:Schema(description = "그룹명", example = "피놀리와 기니휘기")
    val name: String,
    @field:Schema(description = "그룹 커버 이미지 조회 URL. 이미지가 없으면 null", nullable = true)
    val coverImageUrl: String?,
    @field:Schema(description = "현재 그룹 멤버 수", example = "3")
    val memberCount: Int,
    @field:Schema(description = "OWNER 우선, 가입 순으로 정렬된 최대 3명의 멤버 미리보기")
    val previewMembers: List<GroupPreviewMemberResponse>,
    @field:Schema(description = "로그인 사용자의 그룹 고정 여부", example = "true")
    val pinned: Boolean,
    @field:Schema(description = "로그인 사용자의 그룹 삭제 권한", example = "true")
    val canDeleteGroup: Boolean,
) {
    companion object {
        fun from(result: MyGroupSummaryResult): MyGroupResponse =
            MyGroupResponse(
                groupId = result.groupId,
                name = result.name,
                coverImageUrl = result.coverImageUrl,
                memberCount = result.memberCount,
                previewMembers = result.previewMembers.map(GroupPreviewMemberResponse::from),
                pinned = result.pinned,
                canDeleteGroup = result.canDeleteGroup,
            )
    }
}

@Schema(description = "그룹 멤버 미리보기")
data class GroupPreviewMemberResponse(
    @field:Schema(description = "그룹 멤버 ID", example = "21")
    val groupMemberId: Long,
    @field:Schema(description = "멤버 닉네임", example = "이정원")
    val nickname: String,
    @field:Schema(description = "프로필 이미지 조회 URL. 이미지가 없으면 null", nullable = true)
    val profileImageUrl: String?,
) {
    companion object {
        fun from(result: GroupPreviewMemberResult): GroupPreviewMemberResponse =
            GroupPreviewMemberResponse(
                groupMemberId = result.groupMemberId,
                nickname = result.nickname,
                profileImageUrl = result.profileImageUrl,
            )
    }
}
