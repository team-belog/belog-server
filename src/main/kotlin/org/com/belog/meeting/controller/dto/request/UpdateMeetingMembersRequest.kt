package org.com.belog.meeting.controller.dto.request

import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size
import org.com.belog.group.domain.Group

@Schema(description = "만남 참여자 수정 요청")
data class UpdateMeetingMembersRequest(
    @field:ArraySchema(
        arraySchema =
            Schema(
                description =
                    "수정 후 만남에 참여할 그룹 멤버 ID 전체 목록. 방장은 항상 참여자로 유지되므로 제외합니다. " +
                        "기존 참여자 중 목록에 없는 멤버는 만남에서 제외되고, 새로 포함된 멤버는 추가됩니다.",
                example = "[21, 23]",
                requiredMode = Schema.RequiredMode.REQUIRED,
            ),
        schema = Schema(implementation = Long::class),
        maxItems = MAX_PARTICIPANT_COUNT,
    )
    @field:Size(
        max = MAX_PARTICIPANT_COUNT,
        message = "참여자는 방장을 제외하고 최대 ${MAX_PARTICIPANT_COUNT}명까지 지정할 수 있습니다.",
    )
    val participantMemberIds: List<Long>,
) {
    companion object {
        private const val OWNER_COUNT = 1
        private const val MAX_PARTICIPANT_COUNT = Group.MAX_MEMBER_COUNT - OWNER_COUNT
    }
}
