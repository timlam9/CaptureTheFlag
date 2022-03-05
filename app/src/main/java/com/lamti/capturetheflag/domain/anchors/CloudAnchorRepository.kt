package com.lamti.capturetheflag.domain.anchors

import com.lamti.capturetheflag.domain.game.Game
import com.lamti.capturetheflag.domain.game.GeofenceObject


interface CloudAnchorRepository {

    suspend fun uploadGeofenceObject(game: Game): Boolean

    suspend fun getUploadedGeofenceObject() : GeofenceObject

}
