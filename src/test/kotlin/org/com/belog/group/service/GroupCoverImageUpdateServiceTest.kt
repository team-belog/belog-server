package org.com.belog.group.service

import org.com.belog.group.domain.Group
import org.com.belog.group.domain.GroupCoverImageObjectKey
import org.com.belog.group.domain.InviteCode
import org.com.belog.group.repository.GroupRepository
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.util.Optional
import kotlin.test.assertEquals

class GroupCoverImageUpdateServiceTest {
    private val groupRepository = mock(GroupRepository::class.java)
    private val service = GroupCoverImageUpdateService(groupRepository)

    @Test
    fun `그룹 커버 이미지 Object Key를 변경한다`() {
        val group = createGroup()
        val objectKey = GroupCoverImageObjectKey.create(15L, "group-covers/15/image.webp")
        `when`(groupRepository.findById(1L)).thenReturn(Optional.of(group))

        service.update(1L, objectKey)

        assertEquals(objectKey.value, group.coverImageObjectKey)
    }

    private fun createGroup(): Group =
        Group.create(
            name = "러닝 모임",
            coverImageObjectKey = null,
            inviteCode = InviteCode.create("AB12CD"),
        )
}
