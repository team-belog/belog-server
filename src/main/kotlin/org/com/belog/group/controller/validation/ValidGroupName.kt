package org.com.belog.group.controller.validation

import jakarta.validation.Constraint
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import jakarta.validation.Payload
import org.com.belog.group.domain.Group
import kotlin.reflect.KClass

const val GROUP_NAME_LENGTH_MESSAGE =
    "그룹명은 ${Group.NAME_MIN_LENGTH}자 이상 ${Group.NAME_MAX_LENGTH}자 이하여야 합니다."

@Target(AnnotationTarget.FIELD, AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [GroupNameValidator::class])
annotation class ValidGroupName(
    val message: String = GROUP_NAME_LENGTH_MESSAGE,
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = [],
)

class GroupNameValidator : ConstraintValidator<ValidGroupName, String> {
    override fun isValid(
        value: String?,
        context: ConstraintValidatorContext,
    ): Boolean {
        val normalizedName = value?.trim() ?: return false
        return normalizedName.length in Group.NAME_MIN_LENGTH..Group.NAME_MAX_LENGTH
    }
}
