package org.com.belog.prelog.infrastructure.google

import org.springframework.stereotype.Component
import java.math.BigDecimal

@Component
class GooglePlusCodeDecoder {
    fun decode(value: String): GoogleMapCoordinates? {
        val normalized = value.trim().uppercase()
        if (!isValidFullCode(normalized)) {
            return null
        }

        val code = normalized.replace(SEPARATOR.toString(), "").replace(PADDING.toString(), "")
        var latitude = MIN_LATITUDE
        var longitude = MIN_LONGITUDE
        var latitudeResolution = PAIR_RESOLUTIONS.first()
        var longitudeResolution = PAIR_RESOLUTIONS.first()
        val pairLength = minOf(code.length, PAIR_CODE_LENGTH)

        for (index in 0 until pairLength step 2) {
            val resolution = PAIR_RESOLUTIONS[index / 2]
            latitude += alphabetIndex(code[index]) * resolution
            longitude += alphabetIndex(code[index + 1]) * resolution
            latitudeResolution = resolution
            longitudeResolution = resolution
        }

        for (index in PAIR_CODE_LENGTH until code.length) {
            val alphabetIndex = alphabetIndex(code[index])
            latitudeResolution /= GRID_ROWS
            longitudeResolution /= GRID_COLUMNS
            latitude += alphabetIndex / GRID_COLUMNS * latitudeResolution
            longitude += alphabetIndex % GRID_COLUMNS * longitudeResolution
        }

        return GoogleMapCoordinates(
            latitude = BigDecimal.valueOf(latitude + latitudeResolution / 2),
            longitude = BigDecimal.valueOf(longitude + longitudeResolution / 2),
        )
    }

    private fun isValidFullCode(value: String): Boolean {
        if (value.indexOf(SEPARATOR) != SEPARATOR_POSITION || value.indexOf(SEPARATOR) != value.lastIndexOf(SEPARATOR)) {
            return false
        }

        val code = value.replace(SEPARATOR.toString(), "").replace(PADDING.toString(), "")
        val pairLength = minOf(code.length, PAIR_CODE_LENGTH)
        return code.length in MIN_CODE_LENGTH..MAX_CODE_LENGTH &&
            pairLength % 2 == 0 &&
            code.all(ALPHABET::contains)
    }

    private fun alphabetIndex(character: Char): Double = ALPHABET.indexOf(character).toDouble()

    companion object {
        private const val ALPHABET = "23456789CFGHJMPQRVWX"
        private const val SEPARATOR = '+'
        private const val PADDING = '0'
        private const val SEPARATOR_POSITION = 8
        private const val PAIR_CODE_LENGTH = 10
        private const val MIN_CODE_LENGTH = 10
        private const val MAX_CODE_LENGTH = 15
        private const val MIN_LATITUDE = -90.0
        private const val MIN_LONGITUDE = -180.0
        private const val GRID_ROWS = 5.0
        private const val GRID_COLUMNS = 4.0
        private val PAIR_RESOLUTIONS = doubleArrayOf(20.0, 1.0, 0.05, 0.0025, 0.000125)
    }
}
