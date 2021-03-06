package com.lamti.capturetheflag.domain

import com.google.android.gms.maps.model.LatLng
import com.lamti.capturetheflag.domain.game.Game
import com.lamti.capturetheflag.domain.game.GamePlayer
import com.lamti.capturetheflag.domain.player.Player
import kotlinx.coroutines.flow.Flow

interface FirestoreRepository {

    // Authentication
    suspend fun registerUser(email: String, password: String, username: String): Boolean

    suspend fun loginUser(email: String, password: String): Boolean

    fun logout()

    // Players
    fun observePlayer(): Flow<Player>

    suspend fun getPlayer(): Player?

    suspend fun updatePlayer(player: Player, clearCache: Boolean) :Boolean

    // Games
    fun observeGame(gameID: String): Flow<Game>

    suspend fun getGame(id: String): Game?

    suspend fun updateGame(game: Game): Boolean

    suspend fun createGame(id: String, title: String, position: LatLng, player: Player): Boolean

    // Database repository
    fun observePlayersPosition(gameID: String): Flow<List<GamePlayer>>

    suspend fun uploadGamePlayer(position: LatLng)

    suspend fun deleteGamePlayer(gameID: String, playerID: String): Boolean

    fun clearCache(): Boolean
}
