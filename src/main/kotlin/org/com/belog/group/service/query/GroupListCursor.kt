package org.com.belog.group.service.query

data class GroupListCursor(
    val pinned: Boolean,
    val groupMemberId: Long,
) {
    init {
        require(groupMemberId > 0) { "그룹 목록 커서의 그룹 멤버 ID는 양수여야 합니다." }
    }
}
