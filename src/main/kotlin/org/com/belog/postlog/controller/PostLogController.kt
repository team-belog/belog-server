package org.com.belog.postlog.controller

import jakarta.validation.Valid
import org.com.belog.global.annotation.LoginUserId
import org.com.belog.global.response.CommonResponse
import org.com.belog.postlog.code.PostLogSuccessCode
import org.com.belog.postlog.controller.dto.request.PostLogPhotoUploadUrlsRequest
import org.com.belog.postlog.controller.dto.response.PostLogPhotoUploadUrlsResponse
import org.com.belog.postlog.controller.swagger.PostLogSwagger
import org.com.belog.postlog.service.PostLogPhotoService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/meetings/{meetingId}/post-log")
class PostLogController(
    private val photoService: PostLogPhotoService,
) : PostLogSwagger {
    @PostMapping("/photos/upload-urls")
    override fun issuePhotoUploadUrls(
        @LoginUserId userId: Long,
        @PathVariable meetingId: Long,
        @Valid @RequestBody request: PostLogPhotoUploadUrlsRequest,
    ): ResponseEntity<CommonResponse<PostLogPhotoUploadUrlsResponse>> {
        val results =
            photoService.issueUploadUrls(
                meetingId = meetingId,
                userId = userId,
                targets = request.photos.map { photo -> photo.toTarget() },
            )

        return ResponseEntity
            .status(PostLogSuccessCode.PHOTO_UPLOAD_URLS_ISSUED.status)
            .body(
                CommonResponse.success(
                    PostLogSuccessCode.PHOTO_UPLOAD_URLS_ISSUED,
                    PostLogPhotoUploadUrlsResponse.from(results),
                ),
            )
    }
}
