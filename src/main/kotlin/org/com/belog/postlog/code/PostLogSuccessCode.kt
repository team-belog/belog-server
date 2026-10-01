package org.com.belog.postlog.code

import org.com.belog.global.response.code.SuccessCode
import org.springframework.http.HttpStatus

enum class PostLogSuccessCode(
    override val status: HttpStatus,
    override val code: String,
    override val message: String,
) : SuccessCode {
    PHOTO_UPLOAD_URLS_ISSUED(HttpStatus.OK, "POST_LOG-S001", "사진 업로드 URL이 발급되었습니다."),
    PHOTOS_REGISTERED(HttpStatus.CREATED, "POST_LOG-S002", "사진 메타데이터가 등록되었습니다."),
    PHOTO_LIKED(HttpStatus.OK, "POST_LOG-S003", "사진에 좋아요를 등록했습니다."),
    PHOTO_LIKE_CANCELED(HttpStatus.OK, "POST_LOG-S004", "사진 좋아요를 취소했습니다."),
    PHOTOS_RETRIEVED(HttpStatus.OK, "POST_LOG-S005", "사진 목록이 조회되었습니다."),
    SUMMARY_RETRIEVED(HttpStatus.OK, "POST_LOG-S006", "Post-log 만남 정리 정보가 조회되었습니다."),
    TICKET_CREATED(HttpStatus.CREATED, "POST_LOG-S007", "Post-log 티켓이 생성되었습니다."),
    DRAFT_SAVED(HttpStatus.OK, "POST_LOG-S008", "Post-log 추억 문구가 임시저장되었습니다."),
    TICKET_RETRIEVED(HttpStatus.OK, "POST_LOG-S009", "Post-log 티켓이 조회되었습니다."),
    TICKET_CALENDAR_RETRIEVED(HttpStatus.OK, "POST_LOG-S010", "월별 티켓 달력이 조회되었습니다."),
}
