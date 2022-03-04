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
import com.lamti.capturetheflag.domain.game.Flag
import com.lamti.capturetheflag.domain.game.Game
import com.lamti.capturetheflag.domain.game.GameState
import com.lamti.capturetheflag.domain.game.GeofenceObject
import com.lamti.capturetheflag.domain.game.ProgressState
import com.lamti.capturetheflag.domain.player.GameDetails
import com.lamti.capturetheflag.domain.player.Player
import com.lamti.capturetheflag.domain.player.PlayerDetails
import com.lamti.capturetheflag.domain.player.Team
import com.lamti.capturetheflag.utils.EMPTY
import com.lamti.capturetheflag.utils.emptyPosition
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@ExperimentalCoroutinesApi
class FirestoreRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val authenticationRepository: AuthenticationRepository
) : FirestoreRepository {

    private val userID = authenticationRepository.currentUser?.uid ?: EMPTY

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

    override suspend fun createGame(id: String, title: String, position: LatLng): Game {
        val game = Game(
            gameID = id,
            title = title,
            gameState = GameState(
                safehouse = GeofenceObject(
                    position = position,
                    isPlaced = true,
                    isDiscovered = true
                ),
                greenFlag = GeofenceObject(
                    position = emptyPosition(),
                    isPlaced = false,
                    isDiscovered = false
                ),
                redFlag = GeofenceObject(
                    position = emptyPosition(),
                    isPlaced = false,
                    isDiscovered = false
                ),
                state = ProgressState.Created
            )
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
            status = Player.Status.Connected
        ).toRaw()

        firestore
            .collection(COLLECTION_PLAYERS)
            .document(userID)
            .set(updatedPlayer)
            .await()

        return game
    }

    override suspend fun getGame(id: String): Game = firestore
        .collection(COLLECTION_GAMES)
        .document(id)
        .get()
        .await()
        .toObject(GameRaw::class.java)
        ?.toGame() ?: GameRaw().toGame()

    override suspend fun updateGameStatus(gameID: String, state: ProgressState) {
        val currentGame = getGame(gameID)
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
        val currentGame = getGame(gameID)
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

    override suspend fun discoverFlag(flagFound: Flag): Boolean {
        val player = getPlayer()
        val gameID = player?.gameDetails?.gameID ?: return false
        val team = player.gameDetails.team
        val game = getGame(gameID)

        val updatedGame = when {
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

    private suspend fun addPlayer(newUser: Player) {
        firestore
            .collection(COLLECTION_PLAYERS)
            .document(newUser.userID)
            .set(newUser)
            .await()
    }

    companion object {

        private const val COLLECTION_PLAYERS = "players"
        private const val COLLECTION_GAMES = "games"
    }

}
