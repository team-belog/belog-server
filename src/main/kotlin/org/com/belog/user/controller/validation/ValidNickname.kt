package org.com.belog.user.controller.validation

import jakarta.validation.Constraint
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import jakarta.validation.Payload
import org.com.belog.user.domain.User
import kotlin.reflect.KClass

@Target(AnnotationTarget.FIELD, AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [NicknameValidator::class])
annotation class ValidNickname(
    val message: String = "닉네임은 1자 이상 8자 이하여야 합니다.",
    val nullableAllowed: Boolean = false,
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = [],
)

class NicknameValidator : ConstraintValidator<ValidNickname, String> {
    private var nullableAllowed: Boolean = false

    override fun initialize(constraintAnnotation: ValidNickname) {
        nullableAllowed = constraintAnnotation.nullableAllowed
    }

    override fun isValid(
        value: String?,
        context: ConstraintValidatorContext,
    ): Boolean {
        val normalizedNickname = value?.trim() ?: return nullableAllowed
        return normalizedNickname.length in User.NICKNAME_MIN_LENGTH..User.NICKNAME_MAX_LENGTH
    }
}
