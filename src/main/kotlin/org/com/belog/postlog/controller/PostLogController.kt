package org.com.belog.postlog.controller

import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.com.belog.global.annotation.LoginUserId
import org.com.belog.global.response.CommonResponse
import org.com.belog.postlog.code.PostLogSuccessCode
import org.com.belog.postlog.controller.dto.request.CreatePostLogTicketRequest
import org.com.belog.postlog.controller.dto.request.PostLogPhotoUploadUrlsRequest
import org.com.belog.postlog.controller.dto.request.RegisterPostLogPhotosRequest
import org.com.belog.postlog.controller.dto.request.SavePostLogDraftRequest
import org.com.belog.postlog.controller.dto.response.PostLogPhotoLikeResponse
import org.com.belog.postlog.controller.dto.response.PostLogPhotoListResponse
import org.com.belog.postlog.controller.dto.response.PostLogPhotoUploadUrlsResponse
import org.com.belog.postlog.controller.dto.response.PostLogSummaryResponse
import org.com.belog.postlog.controller.dto.response.PostLogTicketResponse
import org.com.belog.postlog.controller.dto.response.RegisterPostLogPhotosResponse
import org.com.belog.postlog.controller.swagger.PostLogSwagger
import org.com.belog.postlog.service.PostLogPhotoLikeService
import org.com.belog.postlog.service.PostLogPhotoService
import org.com.belog.postlog.service.PostLogService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class PostLogController(
    private val postLogService: PostLogService,
    private val photoService: PostLogPhotoService,
    private val photoLikeService: PostLogPhotoLikeService,
) : PostLogSwagger {
    @PutMapping("/api/v1/meetings/{meetingId}/post-log/draft")
    override fun saveDraft(
        @LoginUserId userId: Long,
        @PathVariable meetingId: Long,
        @Valid @RequestBody request: SavePostLogDraftRequest,
    ): ResponseEntity<CommonResponse<Nothing>> {
        postLogService.saveDraft(
            meetingId = meetingId,
            userId = userId,
            memory = request.memory,
        )

        return ResponseEntity
            .status(PostLogSuccessCode.DRAFT_SAVED.status)
            .body(CommonResponse.success(PostLogSuccessCode.DRAFT_SAVED))
    }

    @PostMapping("/api/v1/meetings/{meetingId}/post-log/ticket")
    override fun createTicket(
        @LoginUserId userId: Long,
        @PathVariable meetingId: Long,
        @Valid @RequestBody request: CreatePostLogTicketRequest,
    ): ResponseEntity<CommonResponse<PostLogTicketResponse>> {
        val result =
            postLogService.createTicket(
                meetingId = meetingId,
                userId = userId,
                memory = request.memory,
            )

        return ResponseEntity
            .status(PostLogSuccessCode.TICKET_CREATED.status)
            .body(
                CommonResponse.success(
                    PostLogSuccessCode.TICKET_CREATED,
                    PostLogTicketResponse.from(result),
                ),
            )
    }

    @GetMapping("/api/v1/post-logs/{postLogId}/ticket")
    override fun getTicket(
        @LoginUserId userId: Long,
        @PathVariable postLogId: Long,
    ): ResponseEntity<CommonResponse<PostLogTicketResponse>> {
        val result = postLogService.getTicket(postLogId = postLogId, userId = userId)

        return ResponseEntity
            .status(PostLogSuccessCode.TICKET_RETRIEVED.status)
            .body(
                CommonResponse.success(
                    PostLogSuccessCode.TICKET_RETRIEVED,
                    PostLogTicketResponse.from(result),
                ),
            )
    }

    @GetMapping("/api/v1/meetings/{meetingId}/post-log")
    override fun getSummary(
        @LoginUserId userId: Long,
        @PathVariable meetingId: Long,
    ): ResponseEntity<CommonResponse<PostLogSummaryResponse>> {
        val result = postLogService.getSummary(meetingId = meetingId, userId = userId)

        return ResponseEntity
            .status(PostLogSuccessCode.SUMMARY_RETRIEVED.status)
            .body(
                CommonResponse.success(
                    PostLogSuccessCode.SUMMARY_RETRIEVED,
                    PostLogSummaryResponse.from(result),
                ),
            )
    }

    @GetMapping("/api/v1/meetings/{meetingId}/post-log-photos")
    override fun getPhotos(
        @LoginUserId userId: Long,
        @PathVariable meetingId: Long,
        @RequestParam(required = false) cursor: String?,
        @RequestParam(defaultValue = "50") @Min(1) @Max(50) size: Int,
    ): ResponseEntity<CommonResponse<PostLogPhotoListResponse>> {
        val result =
            photoService.getPhotos(
                meetingId = meetingId,
                userId = userId,
                cursor = cursor,
                size = size,
            )

        return ResponseEntity
            .status(PostLogSuccessCode.PHOTOS_RETRIEVED.status)
            .body(
                CommonResponse.success(
                    PostLogSuccessCode.PHOTOS_RETRIEVED,
                    PostLogPhotoListResponse.from(result),
                ),
            )
    }

    @PostMapping("/api/v1/meetings/{meetingId}/post-log-photos/upload-urls")
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

    @PostMapping("/api/v1/meetings/{meetingId}/post-log-photos")
    override fun registerPhotos(
        @LoginUserId userId: Long,
        @PathVariable meetingId: Long,
        @Valid @RequestBody request: RegisterPostLogPhotosRequest,
    ): ResponseEntity<CommonResponse<RegisterPostLogPhotosResponse>> {
        val photos =
            photoService.registerPhotos(
                meetingId = meetingId,
                userId = userId,
                targets = request.photos.map { photo -> photo.toTarget() },
            )

        return ResponseEntity
            .status(PostLogSuccessCode.PHOTOS_REGISTERED.status)
            .body(
                CommonResponse.success(
                    PostLogSuccessCode.PHOTOS_REGISTERED,
                    RegisterPostLogPhotosResponse.from(photos),
                ),
            )
    }

    @PutMapping("/api/v1/post-log-photos/{photoId}/likes/me")
    override fun likePhoto(
        @LoginUserId userId: Long,
        @PathVariable photoId: Long,
    ): ResponseEntity<CommonResponse<PostLogPhotoLikeResponse>> {
        val result =
            photoLikeService.likePhoto(
                photoId = photoId,
                userId = userId,
            )

        return ResponseEntity
            .status(PostLogSuccessCode.PHOTO_LIKED.status)
            .body(
                CommonResponse.success(
                    PostLogSuccessCode.PHOTO_LIKED,
                    PostLogPhotoLikeResponse.from(result),
                ),
            )
    }

    @DeleteMapping("/api/v1/post-log-photos/{photoId}/likes/me")
    override fun unlikePhoto(
        @LoginUserId userId: Long,
        @PathVariable photoId: Long,
    ): ResponseEntity<CommonResponse<PostLogPhotoLikeResponse>> {
        val result =
            photoLikeService.unlikePhoto(
                photoId = photoId,
                userId = userId,
            )

        return ResponseEntity
            .status(PostLogSuccessCode.PHOTO_LIKE_CANCELED.status)
            .body(
                CommonResponse.success(
                    PostLogSuccessCode.PHOTO_LIKE_CANCELED,
                    PostLogPhotoLikeResponse.from(result),
                ),
            )
    }
}
