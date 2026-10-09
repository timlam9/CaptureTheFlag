package com.lamti.capturetheflag.data.firestore

import com.google.android.gms.maps.model.LatLng
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.lamti.capturetheflag.data.firestore.GameRaw.Companion.toRaw
import com.lamti.capturetheflag.domain.game.ActivePlayer
import com.lamti.capturetheflag.domain.game.Battle
import com.lamti.capturetheflag.domain.game.BattleMiniGame
import com.lamti.capturetheflag.domain.game.BattleSynchronization
import com.lamti.capturetheflag.domain.game.Game
import com.lamti.capturetheflag.domain.game.GameState
import com.lamti.capturetheflag.domain.game.GeofenceObject
import com.lamti.capturetheflag.domain.game.ProgressState
import com.lamti.capturetheflag.domain.player.Team
import com.lamti.capturetheflag.presentation.ui.DEFAULT_FLAG_RADIUS
import com.lamti.capturetheflag.presentation.ui.DEFAULT_GAME_RADIUS
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
    private val ioDispatcher: CoroutineDispatcher,
    database: FirebaseDatabase
) {
    @Volatile private var serverTimeOffset = 0L

    init {
        // RTDB exposes Firebase's clock offset, shared by all battle deadlines.
        database.getReference(".info/serverTimeOffset").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                serverTimeOffset = snapshot.getValue(Long::class.java) ?: 0L
            }
            override fun onCancelled(error: DatabaseError) {
                Timber.w("Firebase clock offset unavailable: ${error.message}")
            }
        })
    }

    fun battleTimeMillis(): Long = System.currentTimeMillis() + serverTimeOffset

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
            Timber.e("[$FIRESTORE_LOGGER_TAG] Observe game error: ${e.message}")
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

    suspend fun createGame(id: String, title: String, miniGame: BattleMiniGame, position: LatLng, userID: String): Boolean =
        initialGame(
            id = id,
            title = title,
            miniGame = miniGame,
            position = position,
            userID = userID
        ).toRaw().update()

    suspend fun updateGame(gameID: String, transform: (Game) -> Game): Boolean = withContext(ioDispatcher) {
        try {
            val gameRef = firestore.collection(COLLECTION_GAMES).document(gameID)
            firestore.runTransaction { transaction ->
                val latestGame = requireNotNull(transaction.get(gameRef).toObject(GameRaw::class.java)) {
                    "Game $gameID does not exist"
                }.toGame()
                val updatedGame = transform(latestGame)
                require(updatedGame.gameID == gameID && updatedGame.gameID == latestGame.gameID) {
                    "Game transforms must preserve the game ID"
                }
                require(updatedGame.battles == latestGame.battles) {
                    "Battle updates must use dedicated battle methods"
                }
                transaction.set(gameRef, updatedGame.toRaw())
                true
            }.await()
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.e("[$FIRESTORE_LOGGER_TAG] Update game error: ${e.message}")
            false
        }
    }

    private fun initialGame(
        id: String,
        title: String,
        miniGame: BattleMiniGame = BattleMiniGame.None,
        flagRadius: Float = DEFAULT_FLAG_RADIUS,
        gameRadius: Float = DEFAULT_GAME_RADIUS,
        position: LatLng,
        userID: String
    ) = Game(
        gameID = id,
        title = title,
        flagRadius = flagRadius,
        gameRadius = gameRadius,
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
        battleMiniGame = miniGame,
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

    suspend fun deleteGame(gameID: String): Boolean = withContext(ioDispatcher) {
        try {
            firestore
                .collection(COLLECTION_GAMES)
                .document(gameID)
                .delete()
                .await()
            true
        } catch (e: Exception) {
            Timber.e("[$FIRESTORE_LOGGER_TAG] ${e.message}")
            false
        }
    }

    suspend fun updateBattles(gameID: String, battle: Battle): Boolean =
        mutateBattle(gameID) { BattleSynchronization.add(it, battle, battleTimeMillis()) }

    suspend fun joinBattle(gameID: String, battleID: String, playerID: String): Boolean =
        mutateBattle(gameID) { BattleSynchronization.join(it, battleID, playerID, battleTimeMillis()) }

    suspend fun startBattle(gameID: String, battleID: String, playerID: String): Boolean =
        mutateBattle(gameID) { BattleSynchronization.start(it, battleID, playerID, battleTimeMillis()) }

    suspend fun updateReadyToBattle(gameID: String, battleID: String, playerID: String): Boolean =
        mutateBattle(gameID) { BattleSynchronization.ready(it, battleID, playerID, battleTimeMillis()) }

    suspend fun finishBattle(gameID: String, battleID: String, playerID: String, winnerName: String): Boolean =
        mutateBattle(gameID) { BattleSynchronization.finish(it, battleID, playerID, winnerName) }

    suspend fun acknowledgeBattle(gameID: String, battleID: String, playerID: String, playerName: String): Boolean =
        mutateBattle(gameID) { BattleSynchronization.acknowledge(it, battleID, playerID, playerName) }

    suspend fun leaveGame(gameID: String, playerID: String): Boolean =
        mutateBattle(gameID) { BattleSynchronization.leave(it, playerID) }

    private suspend fun mutateBattle(gameID: String, transition: (Game) -> Game?): Boolean =
        withContext(ioDispatcher) {
            try {
                val gameRef = firestore.collection(COLLECTION_GAMES).document(gameID)
                firestore.runTransaction { transaction ->
                    val latest = transaction.get(gameRef).toObject(GameRaw::class.java)?.toGame()
                        ?: return@runTransaction false
                    val updated = transition(latest) ?: return@runTransaction false
                    // Firestore retries this pure transition against the latest committed state.
                    if (updated != latest) transaction.set(gameRef, updated.toRaw(), SetOptions.merge())
                    true
                }.await()
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "[$FIRESTORE_LOGGER_TAG] Battle transaction failed")
                false
            }
        }

    companion object {

        private const val COLLECTION_GAMES = "games"
    }
}
