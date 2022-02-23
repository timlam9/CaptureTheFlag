package com.lamti.capturetheflag.data

import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

@IgnoreExtraProperties
data class CloudAnchor(
    val anchorID: String = "",
    @ServerTimestamp
    val timestamp: Date = Date()
)

//val pattern = "MMMM dd, HH:mm"
//val simpleDateFormat = SimpleDateFormat(pattern, Locale.getDefault())
//val date: String = simpleDateFormat.format(cloudAnchor.timestamp)
