package org.com.belog.group.infrastructure

import org.com.belog.group.config.GroupInviteProperties
import org.com.belog.group.domain.InviteCode
import org.springframework.stereotype.Component
import org.springframework.web.util.UriComponentsBuilder

@Component
class GroupInviteLinkGenerator(
    private val properties: GroupInviteProperties,
) {
    fun generate(inviteCode: InviteCode): String =
        UriComponentsBuilder
            .fromUri(properties.baseUrl)
            .pathSegment(inviteCode.value)
            .build()
            .toUriString()
}
