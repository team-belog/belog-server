package org.com.belog.billlog.infrastructure

import org.com.belog.billlog.code.BillLogErrorCode
import org.com.belog.billlog.domain.ReceiptImageFormat
import org.com.belog.billlog.domain.ReceiptImageObjectKey
import org.com.belog.global.error.BusinessException
import org.com.belog.global.storage.S3ObjectMetadata
import org.com.belog.global.storage.S3ObjectMetadataProvider
import org.com.belog.global.storage.S3ObjectNotFoundException
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import software.amazon.awssdk.services.s3.model.S3Exception
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class S3ReceiptImageObjectVerifierTest {
    private val metadataProvider = mock(S3ObjectMetadataProvider::class.java)
    private val verifier = S3ReceiptImageObjectVerifier(metadataProvider)
    private val objectKey = ReceiptImageObjectKey.create(7L, 15L, "bill-log/receipts/7/15/550e8400-e29b-41d4-a716-446655440000.jpg")

    @Test
    fun `S3에 존재하는 올바른 영수증 이미지 객체를 허용한다`() {
        `when`(metadataProvider.get(objectKey.value)).thenReturn(S3ObjectMetadata("image/jpeg", 1024L))

        verifier.verify(objectKey)
    }

    @Test
    fun `S3 객체가 없으면 영수증 이미지 없음 오류를 반환한다`() {
        val cause = S3ObjectNotFoundException(objectKey.value, S3Exception.builder().statusCode(404).build())
        `when`(metadataProvider.get(objectKey.value)).thenThrow(cause)

        val exception = assertFailsWith<BusinessException> { verifier.verify(objectKey) }

        assertEquals(BillLogErrorCode.RECEIPT_IMAGE_NOT_FOUND, exception.errorCode)
    }

    @Test
    fun `Content-Type과 확장자가 다르면 거부한다`() {
        assertInvalidMetadata(S3ObjectMetadata("image/png", 1024L))
    }

    @Test
    fun `크기가 0인 객체는 거부한다`() {
        assertInvalidMetadata(S3ObjectMetadata("image/jpeg", 0L))
    }

    @Test
    fun `최대 크기를 초과한 객체는 거부한다`() {
        assertInvalidMetadata(S3ObjectMetadata("image/jpeg", ReceiptImageFormat.MAX_FILE_SIZE_BYTES + 1))
    }

    private fun assertInvalidMetadata(metadata: S3ObjectMetadata) {
        `when`(metadataProvider.get(objectKey.value)).thenReturn(metadata)

        val exception = assertFailsWith<BusinessException> { verifier.verify(objectKey) }

        assertEquals(BillLogErrorCode.INVALID_RECEIPT_IMAGE_METADATA, exception.errorCode)
    }
}
