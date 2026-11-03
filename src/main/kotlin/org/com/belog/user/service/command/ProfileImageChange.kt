package org.com.belog.user.service.command

import org.com.belog.user.domain.ProfileImageObjectKey

sealed interface ProfileImageChange {
    data class Update(
        val objectKey: ProfileImageObjectKey,
    ) : ProfileImageChange

    data object Reset : ProfileImageChange
}
