package com.lamti.capturetheflag.data.firestore

import com.google.android.gms.maps.model.LatLng
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.toObject
import com.lamti.capturetheflag.data.firestore.BattleRaw.Companion.toRaw
import com.lamti.capturetheflag.data.firestore.GameRaw.Companion.toRaw
import com.lamti.capturetheflag.domain.game.ActivePlayer
import com.lamti.capturetheflag.domain.game.Battle
import com.lamti.capturetheflag.domain.game.BattleMiniGame
import com.lamti.capturetheflag.domain.game.BattleState
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

    suspend fun updateGame(game: Game): Boolean = game.toRaw().update()

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

    fun updateBattles(gameID: String, battle: Battle): Boolean {
        val gameRef: DocumentReference = firestore.collection(COLLECTION_GAMES).document(gameID)

        firestore.runTransaction { transaction ->
            val snapshot: DocumentSnapshot = transaction.get(gameRef)
            val battles: List<BattleRaw> = snapshot.toObject<GameRaw>()?.battles ?: emptyList()
            val mutableBattles: MutableList<BattleRaw> = battles.toMutableList()

            val battleWithSameID: BattleRaw? = battles.firstOrNull { it.battleID == battle.battleID }
            val battleWithSamePlayerID: BattleRaw? = battles.firstOrNull { battle ->
                val player: BattlingPlayerRaw? = battle.players.firstOrNull { player -> player.id == battle.battleID }
                player != null
            }

            // if current player isn't fighting
            if (battleWithSameID == null && battleWithSamePlayerID == null) mutableBattles.add(battle.toRaw())

            transaction.update(gameRef, FIELD_BATTLES, mutableBattles)
            null
        }
            .addOnFailureListener { e -> Timber.e("[$FIRESTORE_LOGGER_TAG] Transaction failure.", e) }

        return true
    }

    fun updateReadyToBattle(gameID: String, playerID: String): Boolean {
        val gameRef: DocumentReference = firestore.collection(COLLECTION_GAMES).document(gameID)

        firestore.runTransaction { transaction ->
            val snapshot: DocumentSnapshot = transaction.get(gameRef)
            val battles: List<BattleRaw> = snapshot.toObject<GameRaw>()?.battles ?: emptyList()
            val mutableBattles: MutableList<BattleRaw> = battles.toMutableList()
            val playerBattle = battles.first { battle -> battle.players.map { it.id }.contains(playerID) }

            val updatedPlayers = playerBattle.players.map { if (it.id == playerID) it.copy(ready = true) else it }
            val updatedState = if (updatedPlayers.all { it.ready }) BattleState.Started else BattleState.StandBy

            val updatedBattles = mutableBattles.map { battle ->
                if (battle.players.map { it.id }.contains(playerID))
                    battle.copy(
                        players = updatedPlayers,
                        state = updatedState.toString()
                    )
                else battle
            }

            transaction.update(gameRef, FIELD_BATTLES, updatedBattles)
            null
        }
            .addOnFailureListener { e -> Timber.e("[$FIRESTORE_LOGGER_TAG] Transaction failure.", e) }

        return true
    }

    companion object {

        private const val COLLECTION_GAMES = "games"
        private const val FIELD_BATTLES = "battles"
    }
}
