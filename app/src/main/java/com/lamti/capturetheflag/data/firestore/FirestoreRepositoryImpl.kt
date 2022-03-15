package com.lamti.capturetheflag.data.firestore

import android.util.Log
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.lamti.capturetheflag.data.authentication.AuthenticationRepository
import com.lamti.capturetheflag.data.authentication.PlayerRaw
import com.lamti.capturetheflag.data.authentication.PlayerRaw.Companion.toRaw
import com.lamti.capturetheflag.data.firestore.GamePlayerRaw.Companion.toRaw
import com.lamti.capturetheflag.data.firestore.GameRaw.Companion.toRaw
import com.lamti.capturetheflag.domain.FirestoreRepository
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
    private val authenticationRepository: AuthenticationRepository
) : FirestoreRepository {

    private val userID = authenticationRepository.currentUser?.uid ?: EMPTY

    override suspend fun uploadPlayerPosition(position: LatLng) {
        val currentPlayer = getPlayer() ?: return
        val gameDetails = currentPlayer.gameDetails ?: return
        val gameID = gameDetails.gameID

        val gamePlayer = GamePlayer(
            id = userID,
            team = gameDetails.team,
            position = position,
            carryingFlag = false,
            username = currentPlayer.details.username
        ).toRaw()

        firestore
            .collection(COLLECTION_GAMES)
            .document(gameID)
            .collection(COLLECTION_PLAYERS)
            .document(userID)
            .set(gamePlayer)
    }

    override fun observePlayersPosition(gameID: String): Flow<List<GamePlayer>> = callbackFlow {
        val subscription = firestore
            .collection(COLLECTION_GAMES)
            .document(gameID)
            .collection(COLLECTION_PLAYERS)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot == null) return@addSnapshotListener
                try {
                    val players: List<GamePlayer> = snapshot.toObjects(GamePlayerRaw::class.java).map { it.toGamePlayer() }
                    trySend(players)
                } catch (e: Throwable) {
                    // Event couldn't be sent to the flow
                }
            }

        awaitClose { subscription.remove() }
    }

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
        username: String,
        fullName: String,
        onSuccess: () -> Unit
    ) {
        val uid = authenticationRepository.registerUser(email = email, password = password)
        val newUser = Player(
            userID = uid,
            status = Player.Status.Online,
            details = PlayerDetails(
                fullName = fullName,
                username = username,
                email = email
            ),
            gameDetails = null
        )
        addPlayer(newUser)
        onSuccess()
    }

    override suspend fun loginUser(email: String, password: String) = authenticationRepository.loginUser(email, password)

    override suspend fun joinPlayer(gameID: String) {
        val currentPlayer = getPlayer()
        val updatedPlayer = currentPlayer!!.copy(
            gameDetails = GameDetails(
                gameID = gameID,
                team = Team.Unknown,
                rank = GameDetails.Rank.Soldier
            ),
            status = Player.Status.Connecting
        ).toRaw()

        firestore
            .collection(COLLECTION_PLAYERS)
            .document(userID)
            .set(updatedPlayer)
            .await()
    }

    override suspend fun connectPlayer(): Boolean {
        val currentPlayer = getPlayer()
        val updatedPlayer = currentPlayer!!.copy(status = Player.Status.Playing)

        firestore
            .collection(COLLECTION_PLAYERS)
            .document(userID)
            .set(updatedPlayer)
            .await()

        return true
    }

    override suspend fun getPlayer(): Player? = firestore
        .collection(COLLECTION_PLAYERS)
        .document(userID)
        .get()
        .await()
        .toObject(PlayerRaw::class.java)
        ?.toPlayer()

    override suspend fun updatePlayerStatus(status: Player.Status) {
        val currentPlayer = getPlayer()
        val updatedPlayer = currentPlayer!!.copy(status = status).toRaw()

        firestore
            .collection(COLLECTION_PLAYERS)
            .document(userID)
            .set(updatedPlayer)
            .await()
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

        val updatedPlayer = currentPlayer!!.copy(
            gameDetails = gameDetails?.copy(
                gameID = gameID,
                team = team,
                rank = rank
            )
        ).toRaw()

        firestore
            .collection(COLLECTION_PLAYERS)
            .document(userID)
            .set(updatedPlayer)
            .await()

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

        firestore
            .collection(COLLECTION_GAMES)
            .document(gameID)
            .set(updatedGame.toRaw())
            .await()
    }

    override suspend fun createGame(id: String, title: String, position: LatLng): Game {
        val game = Game(
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
                greenFlagGrabbed = null,
                redFlagGrabbed = null,
                state = ProgressState.Created
            ),
            redPlayers = listOf(userID),
            greenPlayers = emptyList()
        )

        firestore
            .collection(COLLECTION_GAMES)
            .document(id)
            .set(game.toRaw())
            .await()

        val currentPlayer = getPlayer()
        val updatedPlayer = currentPlayer!!.copy(
            gameDetails = currentPlayer.gameDetails?.copy(gameID = id) ?: GameDetails(
                gameID = id,
                team = Team.Red,
                rank = GameDetails.Rank.Captain
            ),
            status = Player.Status.Connecting
        ).toRaw()

        firestore
            .collection(COLLECTION_PLAYERS)
            .document(userID)
            .set(updatedPlayer)
            .await()

        return game
    }

    override suspend fun endGame(team: Team) {
        val currentPlayer = getPlayer() ?: return
        val gameDetails = currentPlayer.gameDetails ?: return
        val gameID = gameDetails.gameID
        val currentGame = getGame(gameID) ?: return
        val updatedGame: GameRaw =
            currentGame.copy(
                gameState = currentGame.gameState.copy(
                    safehouse = currentGame.gameState.safehouse,
                    greenFlag = currentGame.gameState.greenFlag,
                    redFlag = currentGame.gameState.redFlag,
                    greenFlagGrabbed = currentGame.gameState.greenFlagGrabbed,
                    redFlagGrabbed = currentGame.gameState.redFlagGrabbed,
                    state = ProgressState.Ended
                )
            ).toRaw()

        firestore
            .collection(COLLECTION_GAMES)
            .document(gameID)
            .set(updatedGame)
            .await()
    }

    override suspend fun quitGame(): Boolean {
        val player = getPlayer() ?: return false
        val playerID = player.userID

        val updatedPlayer = player.copy(
            status = Player.Status.Online,
            gameDetails = null
        )

        firestore
            .collection(COLLECTION_PLAYERS)
            .document(playerID)
            .set(updatedPlayer)
            .await()

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
        val updatedGame = currentGame.copy(
            gameState = currentGame.gameState.copy(state = state)
        ).toRaw()

        firestore
            .collection(COLLECTION_GAMES)
            .document(gameID)
            .set(updatedGame)
            .await()
    }

    override suspend fun updateSafehousePosition(gameID: String, position: LatLng) {
        val currentGame = getGame(gameID) ?: return
        val updatedGame = currentGame.copy(
            gameState = currentGame.gameState.copy(
                safehouse = currentGame.gameState.safehouse.copy(
                    position = position
                )
            )
        ).toRaw()

        firestore
            .collection(COLLECTION_GAMES)
            .document(gameID)
            .set(updatedGame)
            .await()
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

        val gameRaw = updatedGame.toRaw()

        firestore.collection(COLLECTION_GAMES)
            .document(gameID)
            .set(gameRaw, SetOptions.merge())
            .await()

        return true
    }

    override suspend fun grabTheFlag(): Boolean {
        val currentPlayer = getPlayer() ?: return false
        val playerID = currentPlayer.userID
        val gameDetails = currentPlayer.gameDetails ?: return false
        val playerTeam = gameDetails.team
        val gameID = gameDetails.gameID
        val currentGame = getGame(gameID) ?: return false
        val updatedGame: GameRaw = when (playerTeam) {
            Team.Red -> currentGame.copy(gameState = currentGame.gameState.copy(greenFlagGrabbed = playerID))
            Team.Green -> currentGame.copy(gameState = currentGame.gameState.copy(redFlagGrabbed = playerID))
            Team.Unknown -> currentGame.copy(gameState = currentGame.gameState.copy())
        }.toRaw()

        firestore
            .collection(COLLECTION_GAMES)
            .document(gameID)
            .set(updatedGame)
            .await()

        return true
    }

    private suspend fun addPlayer(newUser: Player) {
        firestore
            .collection(COLLECTION_PLAYERS)
            .document(newUser.userID)
            .set(newUser.toRaw())
            .await()
    }

    companion object {

        private const val COLLECTION_PLAYERS = "players"
        const val COLLECTION_GAMES = "games"
    }
}
