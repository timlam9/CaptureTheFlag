package com.lamti.capturetheflag.domain

import com.google.android.gms.maps.model.LatLng
import com.lamti.capturetheflag.domain.game.Flag
import com.lamti.capturetheflag.domain.game.Game
import com.lamti.capturetheflag.domain.game.GamePlayer
import com.lamti.capturetheflag.domain.game.GameState
import com.lamti.capturetheflag.domain.game.ProgressState
import com.lamti.capturetheflag.domain.player.Player
import com.lamti.capturetheflag.domain.player.Team
import kotlinx.coroutines.flow.Flow

interface FirestoreRepository {

    suspend fun uploadGamePlayer(position: LatLng)

    fun observePlayersPosition(gameID: String): Flow<List<GamePlayer>>

    fun observePlayer(): Flow<Player>

    fun observeGame(): Flow<Game>

    fun observeGameState(id: String): Flow<GameState>

    suspend fun registerUser(email: String, password: String, username: String, fullName: String, onSuccess: () -> Unit)

    suspend fun loginUser(email: String, password: String): Boolean

    suspend fun joinPlayer(gameID: String)

    suspend fun connectPlayer(): Boolean

    suspend fun getPlayer(): Player?

    suspend fun updatePlayerStatus(status: Player.Status)

    suspend fun setPlayerTeam(team: Team)

    suspend fun createGame(id: String, title: String, position: LatLng): Boolean

    suspend fun getGame(id: String): Game?

    suspend fun endGame(team: Team)

    suspend fun quitGame(): Boolean

    suspend fun updateGameStatus(gameID: String, state: ProgressState)

    suspend fun updateSafehousePosition(gameID: String, position: LatLng)

    suspend fun discoverFlag(flagFound: Flag): Boolean

    suspend fun captureFlag(): Boolean

    suspend fun createBattle(opponentID: String): Boolean

    suspend fun lost()
}
