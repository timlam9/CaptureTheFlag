package com.lamti.capturetheflag.data

import com.google.firebase.firestore.FirebaseFirestore
import com.lamti.capturetheflag.domain.CloudAnchorRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject

class CloudAnchorRepositoryImpl @Inject constructor(
    private val firestoreDatabase: FirebaseFirestore
) : CloudAnchorRepository {


    override suspend fun uploadAnchor(anchor: CloudAnchor): Boolean = withContext(Dispatchers.IO) {
        try {
            firestoreDatabase.collection(COLLECTION_ANCHORS)
                .document(DOCUMENT_FLAG)
                .set(anchor)
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun getUploadedAnchor(): CloudAnchor = withContext(Dispatchers.IO) {
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

