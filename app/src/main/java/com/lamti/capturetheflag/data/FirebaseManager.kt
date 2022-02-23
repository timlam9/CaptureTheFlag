package com.lamti.capturetheflag.data

import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class FirebaseManager {

    private val firestoreDatabase = Firebase.firestore

    suspend fun uploadAnchor(id: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val anchor = CloudAnchor(anchorID = id)
            firestoreDatabase.collection(COLLECTION_ANCHORS)
                .document(DOCUMENT_FLAG)
                .set(anchor)
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getUploadedAnchorID(): CloudAnchor = withContext(Dispatchers.IO) {
        firestoreDatabase.collection(COLLECTION_ANCHORS)
            .document(DOCUMENT_FLAG)
            .get()
            .await()
            .toObject(CloudAnchor::class.java) ?: CloudAnchor()
    }

    @ExperimentalCoroutinesApi
    fun getList(): Flow<List<CloudAnchor>> = callbackFlow {
        val subscription = firestoreDatabase.collection("collection").addSnapshotListener { snapshot, _ ->
            if (snapshot == null) {
                return@addSnapshotListener
            }
            try {
                val anchors: List<CloudAnchor> = snapshot.toObjects(CloudAnchor::class.java)
                trySend(anchors)
            } catch (e: Throwable) {
                // Event couldn't be sent to the flow
            }
        }

        awaitClose { subscription.remove() }
    }

    companion object {

        private const val COLLECTION_ANCHORS = "anchors"
        private const val DOCUMENT_FLAG = "flag"

    }

}

