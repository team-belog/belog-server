package org.com.belog.user.config

import org.springframework.boot.context.properties.ConfigurationProperties
import java.util.Base64

@ConfigurationProperties(prefix = "app.security.account-number")
class AccountNumberEncryptionProperties(
    encryptionKey: String,
) {
    internal val decodedEncryptionKey: ByteArray = decodeEncryptionKey(encryptionKey)

    private fun decodeEncryptionKey(encryptionKey: String): ByteArray {
        val decodedKey =
            try {
                Base64.getDecoder().decode(encryptionKey)
            } catch (exception: IllegalArgumentException) {
                throw IllegalArgumentException("계좌번호 암호화 키는 Base64 형식이어야 합니다.", exception)
            }

        require(decodedKey.size == AES_256_KEY_LENGTH_BYTES) {
            "계좌번호 암호화 키는 ${AES_256_KEY_LENGTH_BYTES}바이트여야 합니다."
        }
        return decodedKey
    }

    companion object {
        private const val AES_256_KEY_LENGTH_BYTES = 32
    }
}
