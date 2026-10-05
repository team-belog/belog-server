package org.com.belog.prelog.domain

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import java.math.BigDecimal
import java.math.RoundingMode

private const val PLAN_LOCATION_EXTERNAL_PLACE_ID_MAX_LENGTH = 512
private const val PLAN_LOCATION_PLACE_NAME_MAX_LENGTH = 100
private const val PLAN_LOCATION_ADDRESS_MAX_LENGTH = 500
private const val PLAN_LOCATION_COORDINATE_SCALE = 7

@Embeddable
class PlanLocation protected constructor(
    @Enumerated(EnumType.STRING)
    @Column(name = "map_provider", length = 20)
    val provider: MapProvider,
    @Column(name = "external_place_id", length = PLAN_LOCATION_EXTERNAL_PLACE_ID_MAX_LENGTH)
    val externalPlaceId: String?,
    @Column(name = "place_name", length = PLAN_LOCATION_PLACE_NAME_MAX_LENGTH)
    val placeName: String?,
    @Column(name = "address", length = PLAN_LOCATION_ADDRESS_MAX_LENGTH)
    val address: String?,
    @Column(name = "latitude", precision = 10, scale = PLAN_LOCATION_COORDINATE_SCALE)
    val latitude: BigDecimal?,
    @Column(name = "longitude", precision = 11, scale = PLAN_LOCATION_COORDINATE_SCALE)
    val longitude: BigDecimal?,
) {
    companion object {
        const val EXTERNAL_PLACE_ID_MAX_LENGTH = PLAN_LOCATION_EXTERNAL_PLACE_ID_MAX_LENGTH
        const val PLACE_NAME_MAX_LENGTH = PLAN_LOCATION_PLACE_NAME_MAX_LENGTH
        const val ADDRESS_MAX_LENGTH = PLAN_LOCATION_ADDRESS_MAX_LENGTH

        private val MIN_LATITUDE = BigDecimal("-90")
        private val MAX_LATITUDE = BigDecimal("90")
        private val MIN_LONGITUDE = BigDecimal("-180")
        private val MAX_LONGITUDE = BigDecimal("180")

        fun create(
            provider: MapProvider,
            externalPlaceId: String?,
            placeName: String?,
            address: String?,
            latitude: BigDecimal,
            longitude: BigDecimal,
        ): PlanLocation =
            PlanLocation(
                provider = provider,
                externalPlaceId = normalizeOptional(externalPlaceId, EXTERNAL_PLACE_ID_MAX_LENGTH, "외부 장소 ID"),
                placeName = normalizeOptional(placeName, PLACE_NAME_MAX_LENGTH, "장소명"),
                address = normalizeOptional(address, ADDRESS_MAX_LENGTH, "주소"),
                latitude = validateCoordinate(latitude, MIN_LATITUDE, MAX_LATITUDE, "위도"),
                longitude = validateCoordinate(longitude, MIN_LONGITUDE, MAX_LONGITUDE, "경도"),
            )

        fun unresolved(
            provider: MapProvider,
            externalPlaceId: String?,
        ): PlanLocation =
            PlanLocation(
                provider = provider,
                externalPlaceId = normalizeOptional(externalPlaceId, EXTERNAL_PLACE_ID_MAX_LENGTH, "외부 장소 ID"),
                placeName = null,
                address = null,
                latitude = null,
                longitude = null,
            )

        private fun normalizeOptional(
            value: String?,
            maxLength: Int,
            fieldName: String,
        ): String? {
            val normalized = value?.trim()?.takeIf(String::isNotEmpty) ?: return null
            require(normalized.length <= maxLength) {
                "$fieldName 은(는) ${maxLength}자를 초과할 수 없습니다."
            }
            return normalized
        }

        private fun validateCoordinate(
            value: BigDecimal,
            min: BigDecimal,
            max: BigDecimal,
            fieldName: String,
        ): BigDecimal {
            require(value >= min && value <= max) {
                "$fieldName 값이 허용 범위를 벗어났습니다."
            }
            return value.setScale(PLAN_LOCATION_COORDINATE_SCALE, RoundingMode.HALF_UP)
        }
    }
}
