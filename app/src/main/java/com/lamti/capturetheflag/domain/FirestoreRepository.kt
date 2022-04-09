package com.lamti.capturetheflag.domain

import com.google.android.gms.maps.model.LatLng
import com.lamti.capturetheflag.domain.game.Flag
import com.lamti.capturetheflag.domain.game.Game
import com.lamti.capturetheflag.domain.game.GamePlayer
import com.lamti.capturetheflag.domain.player.Player
import com.lamti.capturetheflag.domain.player.Team
import kotlinx.coroutines.flow.Flow

interface FirestoreRepository {

    // Authentication
    suspend fun registerUser(email: String, password: String, username: String): Boolean

    suspend fun loginUser(email: String, password: String): Boolean

    fun logout()


    // Players
    fun observePlayer(): Flow<Player>

    suspend fun getPlayer(): Player?

    suspend fun updatePlayer(player: Player) :Boolean


    // Games
    fun observeGame(gameID: String): Flow<Game>

    suspend fun getGame(id: String): Game?

    suspend fun endGame(game: Game, team: Team): Boolean

    suspend fun updateSafehousePosition(game: Game, position: LatLng): Boolean

    suspend fun createBattle(opponentID: String, game: Game): Boolean


    // Players and Games
    suspend fun discoverFlag(flagFound: Flag): Boolean

    suspend fun captureFlag(): Boolean

    suspend fun createGame(id: String, title: String, position: LatLng, player: Player): Boolean

    suspend fun lost(player: Player, game: Game)

    suspend fun setPlayerTeam(player: Player)


    // Database repository
    fun observePlayersPosition(gameID: String): Flow<List<GamePlayer>>

    suspend fun uploadGamePlayer(position: LatLng)
}
