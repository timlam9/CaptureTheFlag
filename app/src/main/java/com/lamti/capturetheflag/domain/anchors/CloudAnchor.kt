package com.lamti.capturetheflag.domain.anchors

import com.google.android.gms.maps.model.LatLng
import java.util.Date

data class CloudAnchor(
    val anchorID: String,
    val timestamp: Date = Date(),
    val position: LatLng
)
