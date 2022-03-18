package com.lamti.capturetheflag.data.firestore

import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.lamti.capturetheflag.data.firestore.GamePlayerRaw.Companion.toRaw
import com.lamti.capturetheflag.domain.game.GamePlayer
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject

class FirebaseDatabaseRepository @Inject constructor(private val database: FirebaseDatabase) {

    fun uploadPlayerPosition(gameID: String, player: GamePlayer): Boolean = try {
        database.getReference(DATABASE_REFERENCE)
            .child(gameID)
            .child(player.id)
            .setValue(player.toRaw())
        true
    } catch (e: Exception) {
        Log.d(TAG, "Error uploading position: ${e.message}")
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
                        Log.d(TAG, "Value is: ${player.toGamePlayer()}")
                    }
                }
                trySend(players)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w(TAG, "Failed to read value.", error.toException())
            }
        })

        awaitClose { reference.removeEventListener(subscription) }
    }

    companion object {

        private const val DATABASE_REFERENCE = "root"
        private const val TAG = "TAGARA"
    }
}
