package com.lamti.capturetheflag.data.anchors

import com.lamti.capturetheflag.data.firestore.GamesRepository
import com.lamti.capturetheflag.domain.anchors.CloudAnchorRepository
import com.lamti.capturetheflag.domain.game.Game
import javax.inject.Inject

class CloudAnchorRepositoryImpl @Inject constructor(private val gamesRepository: GamesRepository) : CloudAnchorRepository {

    override suspend fun uploadGeofenceObject(game: Game): Boolean = gamesRepository.updateGame(game = game)

}

