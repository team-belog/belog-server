package org.com.belog.user.controller.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.AssertTrue
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import org.com.belog.user.controller.validation.ValidNickname
import org.com.belog.user.domain.ProfileImageObjectKey
import org.com.belog.user.domain.User

@Schema(description = "내 프로필 수정 요청. 전달하지 않은 항목은 기존 값을 유지합니다.")
data class UpdateUserProfileRequest(
    @field:Schema(
        description = "변경할 닉네임. 생략하면 기존 닉네임을 유지합니다.",
        example = "피블",
        minLength = User.NICKNAME_MIN_LENGTH,
        maxLength = User.NICKNAME_MAX_LENGTH,
        requiredMode = Schema.RequiredMode.NOT_REQUIRED,
        nullable = true,
    )
    @field:ValidNickname(nullableAllowed = true)
    val nickname: String? = null,
    @field:Schema(
        description =
            "프로필 이미지 변경 타입. CUSTOM은 S3에 업로드한 이미지로 변경하며 profileImageObjectKey가 필요합니다. " +
                "DEFAULT는 앱 기본 이미지로 변경하며 profileImageObjectKey를 전달하지 않습니다. " +
                "생략하면 기존 프로필 이미지를 유지합니다.",
        example = "CUSTOM",
        allowableValues = ["CUSTOM", "DEFAULT"],
        requiredMode = Schema.RequiredMode.NOT_REQUIRED,
        nullable = true,
    )
    val profileImageType: ProfileImageUpdateType? = null,
    @field:Schema(
        description =
            "CUSTOM 타입에서 사용할 S3 object key. 프로필 이미지 업로드 URL 발급 API가 반환한 objectKey를 전달합니다.",
        example = "users/15/profile/550e8400-e29b-41d4-a716-446655440000.webp",
        maxLength = ProfileImageObjectKey.MAX_LENGTH,
        requiredMode = Schema.RequiredMode.NOT_REQUIRED,
        nullable = true,
    )
    @field:Size(
        max = ProfileImageObjectKey.MAX_LENGTH,
        message = "프로필 이미지 object key는 ${ProfileImageObjectKey.MAX_LENGTH}자를 초과할 수 없습니다.",
    )
    @field:Pattern(regexp = "^\\S+$", message = "프로필 이미지 object key에는 공백을 포함할 수 없습니다.")
    val profileImageObjectKey: String? = null,
) {
    @get:AssertTrue(message = "수정할 닉네임 또는 프로필 이미지 정보를 입력해 주세요.")
    @get:Schema(hidden = true)
    val isUpdateSpecified: Boolean
        get() = nickname != null || profileImageType != null

    @get:AssertTrue(
        message = "CUSTOM 타입은 profileImageObjectKey가 필요하고, DEFAULT 타입은 profileImageObjectKey를 포함할 수 없습니다.",
    )
    @get:Schema(hidden = true)
    val isProfileImageFieldsValid: Boolean
        get() =
            when (profileImageType) {
                ProfileImageUpdateType.CUSTOM -> !profileImageObjectKey.isNullOrBlank()
                ProfileImageUpdateType.DEFAULT -> profileImageObjectKey == null
                null -> profileImageObjectKey == null
            }
}

@Schema(
    description =
        "프로필 이미지 변경 타입. CUSTOM은 직접 업로드한 이미지를 사용하고, DEFAULT는 앱 기본 이미지를 사용합니다.",
    enumAsRef = true,
)
enum class ProfileImageUpdateType {
    CUSTOM,
    DEFAULT,
}
