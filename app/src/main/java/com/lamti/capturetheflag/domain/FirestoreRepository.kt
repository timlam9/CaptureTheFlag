package com.lamti.capturetheflag.domain

import com.google.android.gms.maps.model.LatLng
import com.lamti.capturetheflag.domain.game.Flag
import com.lamti.capturetheflag.domain.game.Game
import com.lamti.capturetheflag.domain.game.GamePlayer
import com.lamti.capturetheflag.domain.player.Player
import com.lamti.capturetheflag.domain.player.Team
import kotlinx.coroutines.flow.Flow

interface FirestoreRepository {

    suspend fun uploadGamePlayer(position: LatLng)

    fun observePlayersPosition(gameID: String): Flow<List<GamePlayer>>

    fun observePlayer(): Flow<Player>

    fun observeGame(): Flow<Game>

    suspend fun registerUser(email: String, password: String, username: String): Boolean

    suspend fun loginUser(email: String, password: String): Boolean

    suspend fun joinPlayer(player: Player, gameID: String)

    suspend fun connectPlayer(player: Player): Boolean

    suspend fun getPlayer(): Player?

    suspend fun updatePlayer(player: Player) :Boolean

    suspend fun setPlayerTeam(player: Player)

    suspend fun createGame(id: String, title: String, position: LatLng, player: Player): Boolean

    suspend fun getGame(id: String): Game?

    suspend fun endGame(game: Game, team: Team): Boolean

    suspend fun updateSafehousePosition(game: Game, position: LatLng): Boolean

    suspend fun discoverFlag(flagFound: Flag): Boolean

    suspend fun captureFlag(): Boolean

    suspend fun createBattle(opponentID: String, game: Game): Boolean

    suspend fun lost(player: Player, game: Game)

    fun logout()

}
