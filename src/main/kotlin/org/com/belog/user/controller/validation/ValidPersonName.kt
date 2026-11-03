package org.com.belog.user.controller.validation

import jakarta.validation.Constraint
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import jakarta.validation.Payload
import kotlin.reflect.KClass

@Target(AnnotationTarget.FIELD, AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [PersonNameValidator::class])
annotation class ValidPersonName(
    val maxLength: Int,
    val message: String,
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = [],
)

class PersonNameValidator : ConstraintValidator<ValidPersonName, String> {
    private var maxLength: Int = 0

    override fun initialize(constraintAnnotation: ValidPersonName) {
        maxLength = constraintAnnotation.maxLength
    }

    override fun isValid(
        value: String?,
        context: ConstraintValidatorContext,
    ): Boolean {
        val normalizedName = value?.trim() ?: return false
        return normalizedName.isNotEmpty() && normalizedName.length <= maxLength
    }
}
