package com.lamti.capturetheflag.domain

import com.google.android.gms.maps.model.LatLng
import com.lamti.capturetheflag.domain.game.Battle
import com.lamti.capturetheflag.domain.game.BattleMiniGame
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

    suspend fun updatePlayer(player: Player) :Boolean

    // Games
    fun observeGame(gameID: String): Flow<Game>

    suspend fun getGame(id: String): Game?

    suspend fun updateGame(gameID: String, transform: (Game) -> Game): Boolean

    suspend fun createGame(id: String, title: String, miniGame: BattleMiniGame, position: LatLng, player: Player): Boolean

    suspend fun deleteGame(gameID: String): Boolean

    suspend fun leaveGame(gameID: String, playerID: String): Boolean

    // Database repository
    fun observePlayersPosition(gameID: String): Flow<List<GamePlayer>>

    suspend fun uploadGamePlayer(position: LatLng)

    suspend fun deleteGamePlayer(gameID: String, playerID: String): Boolean

    suspend fun deleteFirebaseGame(gameID: String): Boolean

    suspend fun updateBattles(gameID: String, battle: Battle): Boolean

    suspend fun updateReadyToBattle(gameID: String, battleID: String, playerID: String): Boolean

    suspend fun finishBattle(gameID: String, battleID: String, playerID: String, winnerName: String): Boolean

    suspend fun acknowledgeBattle(gameID: String, battleID: String, playerID: String, playerName: String): Boolean
}
