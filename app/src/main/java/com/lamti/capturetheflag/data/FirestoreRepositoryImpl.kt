package com.lamti.capturetheflag.data

import com.google.firebase.firestore.FirebaseFirestore
import com.lamti.capturetheflag.domain.FirestoreRepository
import com.lamti.capturetheflag.domain.player.Player
import com.lamti.capturetheflag.domain.player.PlayerDetails
import com.lamti.capturetheflag.domain.player.Team
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class FirestoreRepositoryImpl @Inject constructor(private val firestore: FirebaseFirestore) : FirestoreRepository {

    override suspend fun getPlayer(id: String): Player = firestore
        .collection(COLLECTION_PLAYERS)
        .document(id)
        .get()
        .await()
        .toObject(PlayerRaw::class.java)
        ?.toPlayer() ?: Player(
        userID = "no id",
        team = Team.Red,
        details = PlayerDetails(fullName = "no name", username = "no username", email = "no email")
    )


    override suspend fun addPlayer(player: Player): Boolean {
        TODO("Not yet implemented")
    }

    companion object {

        private const val COLLECTION_PLAYERS = "players"
    }

}
