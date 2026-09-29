package org.com.belog.postlog.controller

import jakarta.validation.Valid
import org.com.belog.global.annotation.LoginUserId
import org.com.belog.global.response.CommonResponse
import org.com.belog.postlog.code.PostLogSuccessCode
import org.com.belog.postlog.controller.dto.request.PostLogPhotoUploadUrlsRequest
import org.com.belog.postlog.controller.dto.request.RegisterPostLogPhotosRequest
import org.com.belog.postlog.controller.dto.response.PostLogPhotoLikeResponse
import org.com.belog.postlog.controller.dto.response.PostLogPhotoUploadUrlsResponse
import org.com.belog.postlog.controller.dto.response.RegisterPostLogPhotosResponse
import org.com.belog.postlog.controller.swagger.PostLogSwagger
import org.com.belog.postlog.service.PostLogPhotoLikeService
import org.com.belog.postlog.service.PostLogPhotoService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/meetings/{meetingId}/post-log")
class PostLogController(
    private val photoService: PostLogPhotoService,
    private val photoLikeService: PostLogPhotoLikeService,
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

    @PostMapping("/photos")
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

    @PutMapping("/photos/{photoId}/likes/me")
    override fun likePhoto(
        @LoginUserId userId: Long,
        @PathVariable meetingId: Long,
        @PathVariable photoId: Long,
    ): ResponseEntity<CommonResponse<PostLogPhotoLikeResponse>> {
        val result =
            photoLikeService.likePhoto(
                meetingId = meetingId,
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

    @DeleteMapping("/photos/{photoId}/likes/me")
    override fun unlikePhoto(
        @LoginUserId userId: Long,
        @PathVariable meetingId: Long,
        @PathVariable photoId: Long,
    ): ResponseEntity<CommonResponse<PostLogPhotoLikeResponse>> {
        val result =
            photoLikeService.unlikePhoto(
                meetingId = meetingId,
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
