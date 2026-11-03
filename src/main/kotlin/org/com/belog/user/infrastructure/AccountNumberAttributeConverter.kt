package org.com.belog.user.infrastructure

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter
import org.com.belog.user.config.AccountNumberEncryptionProperties
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.security.GeneralSecurityException
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

@Component
@Converter
class AccountNumberAttributeConverter(
    properties: AccountNumberEncryptionProperties,
) : AttributeConverter<String, String> {
    private val secretKey = SecretKeySpec(properties.decodedEncryptionKey, AES_ALGORITHM)
    private val secureRandom = SecureRandom()

    override fun convertToDatabaseColumn(attribute: String?): String? {
        if (attribute == null) {
            return null
        }

        return try {
            val nonce = ByteArray(GCM_NONCE_LENGTH_BYTES).also(secureRandom::nextBytes)
            val cipher = createCipher(Cipher.ENCRYPT_MODE, nonce)
            val encryptedAccountNumber = cipher.doFinal(attribute.toByteArray(StandardCharsets.UTF_8))

            listOf(
                ENCRYPTION_VERSION,
                encoder.encodeToString(nonce),
                encoder.encodeToString(encryptedAccountNumber),
            ).joinToString(SEPARATOR)
        } catch (exception: GeneralSecurityException) {
            throw IllegalStateException("계좌번호를 암호화할 수 없습니다.", exception)
        }
    }

    override fun convertToEntityAttribute(dbData: String?): String? {
        if (dbData == null) {
            return null
        }

        return try {
            val parts = dbData.split(SEPARATOR)
            require(parts.size == ENCRYPTED_VALUE_PART_COUNT && parts[0] == ENCRYPTION_VERSION) {
                "지원하지 않는 계좌번호 암호문 형식입니다."
            }

            val nonce = decoder.decode(parts[1])
            require(nonce.size == GCM_NONCE_LENGTH_BYTES) { "올바르지 않은 계좌번호 암호문입니다." }

            val cipher = createCipher(Cipher.DECRYPT_MODE, nonce)
            val accountNumber = cipher.doFinal(decoder.decode(parts[2]))
            String(accountNumber, StandardCharsets.UTF_8)
        } catch (exception: IllegalArgumentException) {
            throw IllegalStateException("계좌번호 암호문을 읽을 수 없습니다.", exception)
        } catch (exception: GeneralSecurityException) {
            throw IllegalStateException("계좌번호 암호문을 복호화할 수 없습니다.", exception)
        }
    }

    private fun createCipher(
        mode: Int,
        nonce: ByteArray,
    ): Cipher =
        Cipher.getInstance(CIPHER_TRANSFORMATION).apply {
            init(mode, secretKey, GCMParameterSpec(GCM_TAG_LENGTH_BITS, nonce))
            updateAAD(ADDITIONAL_AUTHENTICATED_DATA)
        }

    companion object {
        const val ENCRYPTED_COLUMN_MAX_LENGTH = 128

        private const val AES_ALGORITHM = "AES"
        private const val CIPHER_TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_NONCE_LENGTH_BYTES = 12
        private const val GCM_TAG_LENGTH_BITS = 128
        private const val ENCRYPTION_VERSION = "v1"
        private const val ENCRYPTED_VALUE_PART_COUNT = 3
        private const val SEPARATOR = "."
        private val ADDITIONAL_AUTHENTICATED_DATA = "belog:account-number:v1".toByteArray(StandardCharsets.UTF_8)
        private val encoder = Base64.getUrlEncoder().withoutPadding()
        private val decoder = Base64.getUrlDecoder()
    }
}
