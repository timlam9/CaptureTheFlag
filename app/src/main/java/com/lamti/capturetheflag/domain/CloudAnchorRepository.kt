package com.lamti.capturetheflag.domain

import com.lamti.capturetheflag.data.CloudAnchor

interface CloudAnchorRepository {

    suspend fun uploadAnchor(anchor: CloudAnchor): Boolean

    suspend fun getUploadedAnchor() : CloudAnchor

}
