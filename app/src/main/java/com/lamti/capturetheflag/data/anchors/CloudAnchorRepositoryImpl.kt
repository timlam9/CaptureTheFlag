package com.lamti.capturetheflag.data.anchors

import com.google.firebase.firestore.FirebaseFirestore
import com.lamti.capturetheflag.data.anchors.CloudAnchorRaw.Companion.toRaw
import com.lamti.capturetheflag.data.firestore.FirestoreRepositoryImpl.Companion.COLLECTION_GAMES
import com.lamti.capturetheflag.domain.FirestoreRepository
import com.lamti.capturetheflag.domain.anchors.CloudAnchor
import com.lamti.capturetheflag.domain.anchors.CloudAnchorRepository
import com.lamti.capturetheflag.utils.EMPTY
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject

class CloudAnchorRepositoryImpl @Inject constructor(
    private val firestoreDatabase: FirebaseFirestore,
    private val firestoreRepository: FirestoreRepository
) : CloudAnchorRepository {

    override suspend fun uploadAnchor(anchor: CloudAnchor): Boolean = withContext(Dispatchers.IO) {
        try {
            val player = firestoreRepository.getPlayer()
            val team = player?.gameDetails?.team?.name ?: "Unknown"
            val game = firestoreRepository.getGame(player?.gameDetails?.gameID ?: EMPTY)
            val gameId = game.gameID

            firestoreDatabase
                .collection(COLLECTION_GAMES)
                .document(gameId)
                .collection(COLLECTION_ANCHORS)
                .document("${team}Flag")
                .set(anchor.toRaw())
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun getUploadedAnchor(): CloudAnchor = withContext(Dispatchers.IO) {
        val player = firestoreRepository.getPlayer()
        val team = player?.gameDetails?.team?.name ?: "Unknown"
        val opponentTeam = when(team) {
            "Red" -> "Green"
            "Green" -> "Red"
            else -> "Unknown"
        }
        val game = firestoreRepository.getGame(player?.gameDetails?.gameID ?: EMPTY)
        val gameId = game.gameID

        firestoreDatabase
            .collection(COLLECTION_GAMES)
            .document(gameId)
            .collection(COLLECTION_ANCHORS)
            .document("${opponentTeam}Flag")
            .get()
            .await()
            .toObject(CloudAnchorRaw::class.java)
            ?.toCloudAnchor() ?: CloudAnchorRaw().toCloudAnchor()
    }

    @ExperimentalCoroutinesApi
    fun getList(): Flow<List<CloudAnchorRaw>> = callbackFlow {
        val subscription = firestoreDatabase.collection("collection").addSnapshotListener { snapshot, _ ->
            if (snapshot == null) {
                return@addSnapshotListener
            }
            try {
                val anchors: List<CloudAnchorRaw> = snapshot.toObjects(CloudAnchorRaw::class.java)
                trySend(anchors)
            } catch (e: Throwable) {
                // Event couldn't be sent to the flow
            }
        }

        awaitClose { subscription.remove() }
    }

    companion object {

        private const val COLLECTION_ANCHORS = "anchors"

    }

}

