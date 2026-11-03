package org.com.belog.prelog.infrastructure

import org.com.belog.prelog.domain.MapProvider
import java.net.URI

interface MapUrlDetector {
    val provider: MapProvider

    fun supports(uri: URI): Boolean
}
