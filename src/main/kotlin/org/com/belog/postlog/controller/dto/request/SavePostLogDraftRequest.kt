package org.com.belog.postlog.controller.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import org.com.belog.postlog.controller.validation.ValidPostLogMemory
import org.com.belog.postlog.domain.PostLogMemory

@Schema(description = "Post-log 임시저장 요청")
data class SavePostLogDraftRequest(
    @field:Schema(
        description = "임시저장할 추억 문구",
        example = "함께한 광주 여행을 오래 기억하자",
        minLength = PostLogMemory.MIN_LENGTH,
        maxLength = PostLogMemory.MAX_LENGTH,
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    @field:NotBlank(message = "추억 문구는 공백일 수 없습니다.")
    @field:ValidPostLogMemory
    val memory: String,
)
