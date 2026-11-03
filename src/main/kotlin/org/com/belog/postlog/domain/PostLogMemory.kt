package org.com.belog.postlog.domain

object PostLogMemory {
    const val MIN_LENGTH = 1
    const val MAX_LENGTH = 80
    const val LENGTH_CHECK_CONSTRAINT = "CHAR_LENGTH(TRIM(memory)) BETWEEN $MIN_LENGTH AND $MAX_LENGTH"

    fun normalize(memory: String): String {
        val normalizedMemory = memory.trim()
        require(normalizedMemory.length in MIN_LENGTH..MAX_LENGTH) {
            "추억 문구는 ${MIN_LENGTH}자 이상 ${MAX_LENGTH}자 이하여야 합니다."
        }
        return normalizedMemory
    }
}
