package com.lamti.capturetheflag.data.firestore

import com.google.android.gms.maps.model.LatLng
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.lamti.capturetheflag.data.firestore.GameRaw.Companion.toRaw
import com.lamti.capturetheflag.domain.game.ActivePlayer
import com.lamti.capturetheflag.domain.game.Game
import com.lamti.capturetheflag.domain.game.GameState
import com.lamti.capturetheflag.domain.game.GeofenceObject
import com.lamti.capturetheflag.domain.game.ProgressState
import com.lamti.capturetheflag.domain.player.Team
import com.lamti.capturetheflag.utils.EMPTY
import com.lamti.capturetheflag.utils.FIRESTORE_LOGGER_TAG
import com.lamti.capturetheflag.utils.emptyPosition
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.Date
import javax.inject.Inject

class GamesRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val ioDispatcher: CoroutineDispatcher
) {

    fun observeGame(gameID: String): Flow<Game> = callbackFlow {
        var snapshotListener: ListenerRegistration? = null
        try {
            snapshotListener = firestore
                .collection(COLLECTION_GAMES)
                .document(gameID)
                .addSnapshotListener { snapshot, _ ->
                    val state = snapshot?.toObject(GameRaw::class.java)?.toGame() ?: return@addSnapshotListener
                    trySend(state)
                }
        } catch (e: Exception) {
            Timber.e("Observe game error: ${e.message}")
        }

        awaitClose {
            snapshotListener?.remove()
        }
    }

    suspend fun getGame(id: String): Game? = withContext(ioDispatcher) {
        try {
            firestore
                .collection(COLLECTION_GAMES)
                .document(id)
                .get()
                .await()
                .toObject(GameRaw::class.java)
                ?.toGame()
        } catch (e: Exception) {
            Timber.e("[$FIRESTORE_LOGGER_TAG] ${e.message}")
            null
        }
    }

    suspend fun createGame(id: String, title: String, position: LatLng, userID: String): Boolean = initialGame(
        id = id,
        title = title,
        position = position,
        userID = userID
    ).toRaw().update()

    suspend fun updateGame(game: Game): Boolean = game.toRaw().update()

    private fun initialGame(
        id: String,
        title: String,
        position: LatLng,
        userID: String
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
        redPlayers = listOf(ActivePlayer(id = userID, hasLost = false)),
        greenPlayers = emptyList(),
        battles = emptyList()
    )

    private suspend fun GameRaw.update(): Boolean = withContext(ioDispatcher) {
        try {
            firestore
                .collection(COLLECTION_GAMES)
                .document(gameID)
                .set(this@update, SetOptions.merge())
                .await()
            true
        } catch (e: Exception) {
            Timber.e("[$FIRESTORE_LOGGER_TAG] ${e.message}")
            false
        }
    }

    companion object {

        private const val COLLECTION_GAMES = "games"
    }
}
