package com.lamti.capturetheflag.data.anchors

import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import com.lamti.capturetheflag.data.firestore.emptyGeoPoint
import com.lamti.capturetheflag.data.firestore.toGeoPoint
import com.lamti.capturetheflag.data.firestore.toLatLng
import com.lamti.capturetheflag.domain.anchors.CloudAnchor
import com.lamti.capturetheflag.utils.EMPTY
import java.util.Date

@IgnoreExtraProperties
data class CloudAnchorRaw(
    val anchorID: String = EMPTY,
    @ServerTimestamp
    val timestamp: Date = Date(),
    val position: GeoPoint = emptyGeoPoint
) {

    fun toCloudAnchor() = CloudAnchor(
        anchorID = anchorID,
        timestamp = timestamp,
        position = position.toLatLng()
    )

    companion object {

        fun CloudAnchor.toRaw() = CloudAnchorRaw(
            anchorID = anchorID,
            timestamp = timestamp,
            position = position.toGeoPoint()
        )

    }

}

//val pattern = "MMMM dd, HH:mm"
//val simpleDateFormat = SimpleDateFormat(pattern, Locale.getDefault())
//val date: String = simpleDateFormat.format(cloudAnchor.timestamp)
