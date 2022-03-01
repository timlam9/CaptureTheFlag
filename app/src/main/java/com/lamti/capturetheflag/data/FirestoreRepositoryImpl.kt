package com.lamti.capturetheflag.data

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.lamti.capturetheflag.domain.FirestoreRepository
import com.lamti.capturetheflag.domain.game.Flag
import com.lamti.capturetheflag.domain.game.Game
import com.lamti.capturetheflag.domain.game.GameState
import com.lamti.capturetheflag.domain.player.Player
import com.lamti.capturetheflag.domain.player.PlayerDetails
import com.lamti.capturetheflag.domain.player.Team
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject


const val playerID = "qTrrjR2cvHCtSeYcARAS"
const val gameID = "XzlbdvpAWxNaOeVOKx7T"

@ExperimentalCoroutinesApi
class FirestoreRepositoryImpl @Inject constructor(private val firestore: FirebaseFirestore) : FirestoreRepository {

    override suspend fun getPlayer(playerID: String): Player = firestore
        .collection(COLLECTION_GAMES)
        .document(gameID)
        .collection(COLLECTION_PLAYERS)
        .document(playerID)
        .get()
        .await()
        .toObject(PlayerRaw::class.java)
        ?.toPlayer() ?: Player(
        userID = "no id",
        team = Team.Red,
        details = PlayerDetails(fullName = "no name", username = "no username", email = "no email")
    )

    override suspend fun getGame(id: String): Game = firestore
        .collection(COLLECTION_GAMES)
        .document(gameID)
        .get()
        .await()
        .toObject(GameRaw::class.java)
        ?.toGame() ?: GameRaw().toGame()

    override fun observeGameState(id: String): Flow<GameState> = callbackFlow {
        val snapshotListener = firestore
            .collection(COLLECTION_GAMES)
            .document(gameID)
            .addSnapshotListener { snapshot, e ->
                val state = snapshot?.toObject(GameRaw::class.java)?.toGame()?.gameState ?: return@addSnapshotListener
                trySend(state).isSuccess
            }
        awaitClose {
            snapshotListener.remove()
        }
    }

    override suspend fun addPlayer(player: Player): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun discoverFlag(flagFound: Flag): Boolean {
        val game = getGame(gameID)
        val player = getPlayer(playerID)

        val updatedGame = when {
            player.team == Team.Red && flagFound == Flag.Green -> game.copy(
                gameState = game.gameState.copy(
                    isGreenFlagDiscovered = true
                )
            )
            player.team == Team.Green && flagFound == Flag.Red -> game.copy(
                gameState = game.gameState.copy(
                    isRedFlagDiscovered = true
                )
            )
            else -> return false
        }

        firestore.collection(COLLECTION_GAMES)
            .document(gameID)
            .set(updatedGame, SetOptions.merge())
            .await()

        return true
    }

    companion object {

        private const val COLLECTION_PLAYERS = "players"
        private const val COLLECTION_GAMES = "games"
    }

}
