package com.lamti.capturetheflag.data.anchors

import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import com.lamti.capturetheflag.domain.anchors.CloudAnchor
import com.lamti.capturetheflag.utils.EMPTY
import java.util.Date

@IgnoreExtraProperties
data class CloudAnchorRaw(
    val anchorID: String = EMPTY,
    @ServerTimestamp
    val timestamp: Date = Date()
) {

    fun toCloudAnchor() = CloudAnchor(
        anchorID = anchorID,
        timestamp = timestamp
    )

    companion object {

        fun CloudAnchor.toRaw() = CloudAnchorRaw(
            anchorID = anchorID,
            timestamp = timestamp
        )

    }

}

//val pattern = "MMMM dd, HH:mm"
//val simpleDateFormat = SimpleDateFormat(pattern, Locale.getDefault())
//val date: String = simpleDateFormat.format(cloudAnchor.timestamp)
