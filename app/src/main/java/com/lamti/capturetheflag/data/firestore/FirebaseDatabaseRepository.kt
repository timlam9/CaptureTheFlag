package com.lamti.capturetheflag.data.firestore

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.lamti.capturetheflag.data.firestore.GamePlayerRaw.Companion.toRaw
import com.lamti.capturetheflag.domain.game.GamePlayer
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

class FirebaseDatabaseRepository @Inject constructor(
    private val database: FirebaseDatabase,
    private val ioDispatcher: CoroutineDispatcher
) {

    fun updateGamePlayer(gameID: String, player: GamePlayer): Boolean = try {
        database.getReference(DATABASE_REFERENCE)
            .child(gameID)
            .child(player.id)
            .setValue(player.toRaw())
        true
    } catch (e: Exception) {
        Timber.e("Update game player error: ${e.message}")
        false
    }

    fun observePlayersPosition(gameID: String): Flow<List<GamePlayer>> = callbackFlow {
        val reference = database.getReference(DATABASE_REFERENCE).child(gameID)
        val subscription = reference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val players = mutableListOf<GamePlayer>()
                for (snapshot in dataSnapshot.children) {
                    val player: GamePlayerRaw? = snapshot.getValue(GamePlayerRaw::class.java)
                    if (player != null) {
                        players.add(player.toGamePlayer())
                    }
                }
                trySend(players)
            }

            override fun onCancelled(error: DatabaseError) {
                Timber.e("Observe players position error: ${error.message}")
            }
        })

        awaitClose { reference.removeEventListener(subscription) }
    }

    suspend fun deleteGamePlayer(gameID: String, userID: String): Boolean = withContext(ioDispatcher) {
        try {
            database.getReference(DATABASE_REFERENCE)
                .child(gameID)
                .child(userID)
                .removeValue()
                .await()
            true
        } catch (e: Exception) {
            Timber.e("Delete game player error: ${e.message}")
            false
        }
    }

    suspend fun deleteGame(gameID: String): Boolean = withContext(ioDispatcher) {
        try {
            database.getReference(DATABASE_REFERENCE)
                .child(gameID)
                .removeValue()
                .await()
            true
        } catch (e: Exception) {
            Timber.e("Delete game error: ${e.message}")
            false
        }
    }

    companion object {

        private const val DATABASE_REFERENCE = "root"
    }
}
