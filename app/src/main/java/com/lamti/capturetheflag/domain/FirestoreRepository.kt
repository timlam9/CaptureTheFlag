package com.lamti.capturetheflag.domain

import com.lamti.capturetheflag.domain.game.Flag
import com.lamti.capturetheflag.domain.game.Game
import com.lamti.capturetheflag.domain.game.GameState
import com.lamti.capturetheflag.domain.player.Player
import kotlinx.coroutines.flow.Flow

interface FirestoreRepository {

    fun observeGameState(id: String): Flow<GameState>

    fun observePlayer(): Flow<Player>

    suspend fun getPlayer(): Player?

    suspend fun getGame(id: String): Game

    suspend fun loginUser(email: String, password: String): Boolean

    suspend fun registerUser(email: String, password: String, username: String, fullName: String, onSuccess: () -> Unit)

    suspend fun discoverFlag(flagFound: Flag): Boolean

}
