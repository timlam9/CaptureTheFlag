package com.lamti.capturetheflag.domain.anchors

import java.util.Date

data class CloudAnchor(
    val anchorID: String,
    val timestamp: Date = Date()
)
