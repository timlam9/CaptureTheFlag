package com.lamti.capturetheflag.data.firestore

import android.util.Log
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.lamti.capturetheflag.data.authentication.AuthenticationRepository
import com.lamti.capturetheflag.data.authentication.PlayerRaw
import com.lamti.capturetheflag.data.authentication.PlayerRaw.Companion.toRaw
import com.lamti.capturetheflag.data.firestore.GameRaw.Companion.toRaw
import com.lamti.capturetheflag.domain.FirestoreRepository
import com.lamti.capturetheflag.domain.game.Battle
import com.lamti.capturetheflag.domain.game.Flag
import com.lamti.capturetheflag.domain.game.Game
import com.lamti.capturetheflag.domain.game.GamePlayer
import com.lamti.capturetheflag.domain.game.GameState
import com.lamti.capturetheflag.domain.game.GeofenceObject
import com.lamti.capturetheflag.domain.game.ProgressState
import com.lamti.capturetheflag.domain.player.GameDetails
import com.lamti.capturetheflag.domain.player.Player
import com.lamti.capturetheflag.domain.player.PlayerDetails
import com.lamti.capturetheflag.domain.player.Team
import com.lamti.capturetheflag.utils.EMPTY
import com.lamti.capturetheflag.utils.emptyPosition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Date
import javax.inject.Inject

class FirestoreRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val authenticationRepository: AuthenticationRepository,
    private val databaseRepository: FirebaseDatabaseRepository
) : FirestoreRepository {

    private val userID = authenticationRepository.currentUser?.uid ?: EMPTY

    override suspend fun uploadGamePlayer(position: LatLng) {
        val currentPlayer = getPlayer() ?: return
        if (currentPlayer.status != Player.Status.Playing) return
        val gameDetails = currentPlayer.gameDetails ?: return
        val gameID = gameDetails.gameID

        val gamePlayer = GamePlayer(
            id = userID,
            team = gameDetails.team,
            position = position,
            username = currentPlayer.details.username
        )

        databaseRepository.updateGamePlayer(gameID, gamePlayer)
    }

    override fun observePlayersPosition(gameID: String) = databaseRepository.observePlayersPosition(gameID)

    override fun observePlayer(): Flow<Player> = callbackFlow {
        var snapshotListener: ListenerRegistration? = null
        try {
            snapshotListener = firestore
                .collection(COLLECTION_PLAYERS)
                .document(userID)
                .addSnapshotListener { snapshot, e ->
                    val state = snapshot?.toObject(PlayerRaw::class.java)?.toPlayer() ?: return@addSnapshotListener
                    trySend(state).isSuccess
                }
        } catch (e: Exception) {
            Log.d("TAGARA", "error: ${e.message}")
        }

        awaitClose {
            snapshotListener?.remove()
        }
    }

    override fun observeGame(): Flow<Game> = callbackFlow {
        var snapshotListener: ListenerRegistration? = null
        try {
            val player = getPlayer()
            val gameID = player?.gameDetails?.gameID ?: EMPTY

            snapshotListener = firestore
                .collection(COLLECTION_GAMES)
                .document(gameID)
                .addSnapshotListener { snapshot, e ->
                    val state = snapshot?.toObject(GameRaw::class.java)?.toGame() ?: return@addSnapshotListener
                    trySend(state).isSuccess
                }
        } catch (e: Exception) {
            Log.d("TAGARA", "Game error: ${e.message}")
        }

        awaitClose {
            snapshotListener?.remove()
        }
    }

    override fun observeGameState(id: String): Flow<GameState> = callbackFlow {
        var snapshotListener: ListenerRegistration? = null
        try {
            snapshotListener = firestore
                .collection(COLLECTION_GAMES)
                .document(id)
                .addSnapshotListener { snapshot, e ->
                    val state = snapshot?.toObject(GameRaw::class.java)?.toGame()?.gameState ?: return@addSnapshotListener
                    trySend(state).isSuccess
                }
        } catch (e: Exception) {
            Log.d("TAGARA", "error: ${e.message}")
        }

        awaitClose {
            snapshotListener?.remove()
        }
    }

    override suspend fun registerUser(
        email: String,
        password: String,
        username: String
    ): Boolean = authenticationRepository.registerUser(email = email, password = password)?.let { uid ->
        Player(
            userID = uid,
            status = Player.Status.Online,
            details = PlayerDetails(
                username = username,
                email = email
            ),
            gameDetails = null
        ).toRaw()
            .update()
        true
    } ?: false

    override suspend fun loginUser(email: String, password: String) = authenticationRepository.loginUser(email, password)

    override suspend fun joinPlayer(gameID: String) {
        val currentPlayer = getPlayer()
        currentPlayer!!
            .copy(
                gameDetails = GameDetails(
                    gameID = gameID,
                    team = Team.Unknown,
                    rank = GameDetails.Rank.Soldier
                ),
                status = Player.Status.Connecting
            )
            .toRaw()
            .update()
    }

    override suspend fun connectPlayer(): Boolean {
        getPlayer()!!.copy(status = Player.Status.Playing).toRaw().update()
        return true
    }

    override suspend fun getPlayer(): Player? = try {
        firestore
            .collection(COLLECTION_PLAYERS)
            .document(userID)
            .get()
            .await()
            .toObject(PlayerRaw::class.java)
            ?.toPlayer()
    } catch (e: Exception) {
        Log.d("TAGARA", e.message.toString())
        null
    }


    override suspend fun updatePlayerStatus(status: Player.Status) {
        getPlayer()!!.copy(status = status).toRaw().update()
    }

    override suspend fun setPlayerTeam(team: Team) {
        val currentPlayer = getPlayer()
        val playerID = currentPlayer?.userID ?: EMPTY

        val gameDetails = getPlayer()?.gameDetails
        val gameID = gameDetails?.gameID ?: EMPTY
        val game = getGame(gameID)

        val rank = if (team == Team.Green) {
            if (game == null || game.greenPlayers.isEmpty())
                GameDetails.Rank.Leader
            else
                GameDetails.Rank.Soldier
        } else
            GameDetails.Rank.Soldier

        currentPlayer!!
            .copy(
                gameDetails = gameDetails?.copy(
                    gameID = gameID,
                    team = team,
                    rank = rank
                )
            )
            .toRaw()
            .update()

        addPlayerToGame(gameID, team, playerID)
    }

    private suspend fun addPlayerToGame(gameID: String, playerTeam: Team, playerID: String) {
        val game = getGame(gameID) ?: return

        val updatedGame: Game = when (playerTeam) {
            Team.Red -> {
                val newList = game.redPlayers.toMutableList()
                newList.add(playerID)
                game.copy(redPlayers = newList)
            }
            Team.Green -> {
                val newList = game.greenPlayers.toMutableList()
                newList.add(playerID)
                game.copy(greenPlayers = newList)
            }
            Team.Unknown -> game
        }

        updatedGame.toRaw().update()
    }

    override suspend fun createGame(id: String, title: String, position: LatLng): Boolean {
        try {
            initialGame(id, title, position).toRaw().update()
        } catch (e: Exception) {
            Log.d("TAGARA", e.message.toString())
            return false
        }

        try {
            val currentPlayer = getPlayer()
            currentPlayer!!
                .copy(
                    gameDetails = GameDetails(
                        gameID = id,
                        team = Team.Red,
                        rank = GameDetails.Rank.Captain
                    ),
                    status = Player.Status.Connecting
                )
                .toRaw()
                .update()
        } catch (e: Exception) {
            Log.d("TAGARA", e.message.toString())
            return false
        }

        return true
    }

    private fun initialGame(
        id: String,
        title: String,
        position: LatLng
    ) = Game(
        gameID = id,
        title = title,
        gameState = GameState(
            safehouse = GeofenceObject(
                position = position,
                isPlaced = true,
                isDiscovered = true,
                id = EMPTY,
                timestamp = Date()
            ),
            greenFlag = GeofenceObject(
                position = emptyPosition(),
                isPlaced = false,
                isDiscovered = false,
                id = EMPTY,
                timestamp = Date()
            ),
            redFlag = GeofenceObject(
                position = emptyPosition(),
                isPlaced = false,
                isDiscovered = false,
                id = EMPTY,
                timestamp = Date()
            ),
            greenFlagCaptured = null,
            redFlagCaptured = null,
            state = ProgressState.Created,
            winners = Team.Unknown
        ),
        redPlayers = listOf(userID),
        greenPlayers = emptyList(),
        battles = emptyList()
    )

    override suspend fun endGame(team: Team) {
        val currentPlayer = getPlayer() ?: return
        val gameDetails = currentPlayer.gameDetails ?: return
        val gameID = gameDetails.gameID
        val currentGame = getGame(gameID) ?: return

        currentGame
            .copy(
                gameState = currentGame.gameState.copy(
                    safehouse = currentGame.gameState.safehouse,
                    greenFlag = currentGame.gameState.greenFlag,
                    redFlag = currentGame.gameState.redFlag,
                    greenFlagCaptured = currentGame.gameState.greenFlagCaptured,
                    redFlagCaptured = currentGame.gameState.redFlagCaptured,
                    state = ProgressState.Ended,
                    winners = team
                )
            )
            .toRaw()
            .update()
    }

    override suspend fun quitGame(): Boolean {
        val player = getPlayer() ?: return false

        player
            .copy(
                status = Player.Status.Online,
                gameDetails = null
            )
            .toRaw()
            .update()

        return true
    }

    override suspend fun getGame(id: String): Game? = withContext(Dispatchers.IO) {
        try {
            firestore
                .collection(COLLECTION_GAMES)
                .document(id)
                .get()
                .await()
                .toObject(GameRaw::class.java)
                ?.toGame()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override suspend fun updateGameStatus(gameID: String, state: ProgressState) {
        val currentGame = getGame(gameID) ?: return
        currentGame
            .copy(gameState = currentGame.gameState.copy(state = state))
            .toRaw()
            .update()

    }

    override suspend fun updateSafehousePosition(gameID: String, position: LatLng) {
        val currentGame = getGame(gameID) ?: return
        currentGame
            .copy(
                gameState = currentGame.gameState.copy(
                    safehouse = currentGame.gameState.safehouse.copy(
                        position = position
                    )
                )
            )
            .toRaw()
            .update()
    }

    override suspend fun discoverFlag(flagFound: Flag): Boolean {
        val player = getPlayer()
        val gameID = player?.gameDetails?.gameID ?: return false
        val team = player.gameDetails.team
        val game = getGame(gameID) ?: return false

        val updatedGame: Game = when {
            team == Team.Red && flagFound == Flag.Green -> game.copy(
                gameState = game.gameState.copy(
                    greenFlag = game.gameState.greenFlag.copy(isDiscovered = true)
                )
            )
            team == Team.Green && flagFound == Flag.Red -> game.copy(
                gameState = game.gameState.copy(
                    redFlag = game.gameState.redFlag.copy(isDiscovered = true)
                )
            )
            else -> return false
        }

        updatedGame.toRaw().update()

        return true
    }

    override suspend fun captureFlag(): Boolean {
        val currentPlayer = getPlayer() ?: return false
        val playerID = currentPlayer.userID
        val gameDetails = currentPlayer.gameDetails ?: return false
        val playerTeam = gameDetails.team
        val gameID = gameDetails.gameID
        val currentGame = getGame(gameID) ?: return false
        when (playerTeam) {
            Team.Red -> currentGame.copy(gameState = currentGame.gameState.copy(greenFlagCaptured = playerID))
            Team.Green -> currentGame.copy(gameState = currentGame.gameState.copy(redFlagCaptured = playerID))
            Team.Unknown -> currentGame.copy(gameState = currentGame.gameState.copy())
        }.toRaw().update()

        return true
    }

    override suspend fun createBattle(opponentID: String): Boolean {
        val player = getPlayer() ?: return false
        val gameID = player.gameDetails?.gameID ?: return false
        val currentGame = getGame(gameID) ?: return false

        val newBattle = Battle(
            battleID = userID,
            playersIDs = listOf(userID, opponentID)
        )
        currentGame
            .copy(battles = currentGame.battles + newBattle)
            .toRaw()
            .update()

        return true
    }

    override suspend fun lost() {
        val player = getPlayer() ?: return
        val gameID = player.gameDetails?.gameID ?: return
        val currentGame = getGame(gameID) ?: return

        val updatedBattles: MutableList<Battle> = currentGame.battles.toMutableList()
        updatedBattles.removeIf { it.playersIDs.contains(player.userID) }

        val updatedGameState = when (player.userID) {
            currentGame.gameState.redFlagCaptured -> currentGame.gameState.copy(redFlagCaptured = null)
            currentGame.gameState.greenFlagCaptured -> currentGame.gameState.copy(greenFlagCaptured = null)
            else -> currentGame.gameState
        }

        currentGame
            .copy(
                battles = updatedBattles,
                gameState = updatedGameState
            )
            .toRaw()
            .update()

        player.copy(status = Player.Status.Lost).toRaw().update()

        databaseRepository.deleteGamePlayer(gameID, player.userID)
    }

    override fun logout() {
        authenticationRepository.logout()
    }

    private suspend fun PlayerRaw.update() {
        firestore
            .collection(COLLECTION_PLAYERS)
            .document(userID)
            .set(this, SetOptions.merge())
            .await()
    }

    private suspend fun GameRaw.update() {
        firestore
            .collection(COLLECTION_GAMES)
            .document(gameID)
            .set(this, SetOptions.merge())
            .await()
    }

    companion object {

        private const val COLLECTION_PLAYERS = "players"
        const val COLLECTION_GAMES = "games"
    }
}
