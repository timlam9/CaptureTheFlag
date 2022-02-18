package com.lamti.capturetheflag.data

import android.util.Log
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class FirebaseManager {

    private val db = Firebase.firestore

    fun uploadAnchor(id: String) {
        val anchor = FireAnchor(anchorID = id)

        db.collection(COLLECTION_ANCHORS)
            .document(DOCUMENT_FLAG)
            .set(anchor)
            .addOnSuccessListener { documentReference ->
                Log.d(TAG, "ID uploaded: $documentReference")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "ID upload failed: $e")
            }
    }

    fun getUploadedAnchorID(onSuccess: (id: String) -> Unit) {
        db.collection(COLLECTION_ANCHORS)
            .document(DOCUMENT_FLAG)
            .get()
            .addOnSuccessListener {
                val anchor = it.toObject(FireAnchor::class.java) ?: return@addOnSuccessListener
                onSuccess(anchor.anchorID)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "ID retrieval failed: $e")
            }
    }

    companion object {

        private const val COLLECTION_ANCHORS = "anchors"
        private const val DOCUMENT_FLAG = "flag"
        private const val TAG = "firestore db"

    }

    data class FireAnchor(val anchorID: String = "")

}

