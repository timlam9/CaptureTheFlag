package com.lamti.capturetheflag.data.anchors

import com.google.firebase.firestore.FirebaseFirestore
import com.lamti.capturetheflag.data.firestore.FirestoreRepositoryImpl.Companion.COLLECTION_GAMES
import com.lamti.capturetheflag.data.firestore.GameRaw.Companion.toRaw
import com.lamti.capturetheflag.data.firestore.GeofenceObjectRaw
import com.lamti.capturetheflag.domain.FirestoreRepository
import com.lamti.capturetheflag.domain.anchors.CloudAnchorRepository
import com.lamti.capturetheflag.domain.game.Game
import com.lamti.capturetheflag.domain.game.GeofenceObject
import com.lamti.capturetheflag.utils.EMPTY
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject

class CloudAnchorRepositoryImpl @Inject constructor(
    private val firestoreDatabase: FirebaseFirestore,
    private val firestoreRepository: FirestoreRepository
) : CloudAnchorRepository {

    override suspend fun uploadGeofenceObject(game: Game): Boolean = withContext(Dispatchers.IO) {
        try {
            firestoreDatabase
                .collection(COLLECTION_GAMES)
                .document(game.gameID)
                .set(game.toRaw())
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun getUploadedGeofenceObject(): GeofenceObject = withContext(Dispatchers.IO) {
        val player = firestoreRepository.getPlayer()
        val game = firestoreRepository.getGame(player?.gameDetails?.gameID ?: EMPTY)
        val gameId = game?.gameID ?: EMPTY

        firestoreDatabase
            .collection(COLLECTION_GAMES)
            .document(gameId)
            .get()
            .await()
            .toObject(GeofenceObjectRaw::class.java)
            ?.toGeofenceObject() ?: GeofenceObjectRaw().toGeofenceObject()
    }

}

