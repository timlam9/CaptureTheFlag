package com.lamti.capturetheflag.domain.anchors


interface CloudAnchorRepository {

    suspend fun uploadAnchor(anchor: CloudAnchor): Boolean

    suspend fun getUploadedAnchor() : CloudAnchor

}
