package org.com.belog.group.service.result

import org.com.belog.group.service.query.GroupListCursor

data class MyGroupListResult(
    val items: List<MyGroupSummaryResult>,
    val nextCursor: GroupListCursor?,
    val hasNext: Boolean,
)

data class MyGroupSummaryResult(
    val groupId: Long,
    val name: String,
    val coverImageUrl: String?,
    val memberCount: Int,
    val previewMembers: List<GroupPreviewMemberResult>,
    val pinned: Boolean,
    val canDeleteGroup: Boolean,
)

data class GroupPreviewMemberResult(
    val groupMemberId: Long,
    val nickname: String,
    val profileImageUrl: String?,
)
