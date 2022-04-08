package com.lamti.capturetheflag.domain.anchors

import com.lamti.capturetheflag.domain.game.Game

interface CloudAnchorRepository {

    suspend fun uploadGeofenceObject(game: Game): Boolean
}
