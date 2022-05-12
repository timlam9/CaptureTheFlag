package com.lamti.capturetheflag.data.firestore

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.lamti.capturetheflag.data.authentication.PlayerRaw
import com.lamti.capturetheflag.data.authentication.PlayerRaw.Companion.toRaw
import com.lamti.capturetheflag.domain.player.Player
import com.lamti.capturetheflag.utils.FIRESTORE_LOGGER_TAG
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

class PlayersRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val ioDispatcher: CoroutineDispatcher
) {

    fun observePlayer(userID: String): Flow<Player> = callbackFlow {
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
            Timber.e("Observe player error: ${e.message}")
        }

        awaitClose {
            snapshotListener?.remove()
        }
    }

    suspend fun getPlayer(userID: String): Player? = withContext(ioDispatcher) {
        try {
            firestore
                .collection(COLLECTION_PLAYERS)
                .document(userID)
                .get()
                .await()
                .toObject(PlayerRaw::class.java)
                ?.toPlayer()
        } catch (e: Exception) {
            Timber.e("Get player error: ${e.message}")
            null
        }
    }

    suspend fun updatePlayer(player: Player, clearCache: Boolean): Boolean {
        if (clearCache) firestore.clearPersistence()
        return player.toRaw().update()
    }

    private suspend fun PlayerRaw.update(): Boolean = withContext(ioDispatcher) {
        try {
            firestore
                .collection(COLLECTION_PLAYERS)
                .document(userID)
                .set(this@update, SetOptions.merge())
                .await()
            true
        } catch (e: Exception) {
            Timber.e("[$FIRESTORE_LOGGER_TAG] ${e.message}")
            false
        }
    }

    companion object {

        private const val COLLECTION_PLAYERS = "players"
    }
}
