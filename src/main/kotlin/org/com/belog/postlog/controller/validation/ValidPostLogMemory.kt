package org.com.belog.postlog.controller.validation

import jakarta.validation.Constraint
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import jakarta.validation.Payload
import org.com.belog.postlog.domain.PostLogMemory
import kotlin.reflect.KClass

const val POST_LOG_MEMORY_LENGTH_MESSAGE =
    "추억 문구는 ${PostLogMemory.MIN_LENGTH}자 이상 ${PostLogMemory.MAX_LENGTH}자 이하여야 합니다."

@Target(AnnotationTarget.FIELD, AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [PostLogMemoryValidator::class])
annotation class ValidPostLogMemory(
    val message: String = POST_LOG_MEMORY_LENGTH_MESSAGE,
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = [],
)

class PostLogMemoryValidator : ConstraintValidator<ValidPostLogMemory, String> {
    override fun isValid(
        value: String?,
        context: ConstraintValidatorContext,
    ): Boolean {
        val normalizedMemory = value?.trim() ?: return false
        return normalizedMemory.length in PostLogMemory.MIN_LENGTH..PostLogMemory.MAX_LENGTH
    }
}
