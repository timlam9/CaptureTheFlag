package com.lamti.capturetheflag.data.firestore

import com.google.android.gms.maps.model.LatLng
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.lamti.capturetheflag.data.authentication.AuthenticationRepository
import com.lamti.capturetheflag.data.firestore.GameRaw.Companion.toRaw
import com.lamti.capturetheflag.domain.FirestoreRepository
import com.lamti.capturetheflag.domain.game.ActivePlayer
import com.lamti.capturetheflag.domain.game.Battle
import com.lamti.capturetheflag.domain.game.Flag
import com.lamti.capturetheflag.domain.game.Game
import com.lamti.capturetheflag.domain.game.GamePlayer
import com.lamti.capturetheflag.domain.game.GameState
import com.lamti.capturetheflag.domain.game.GeofenceObject
import com.lamti.capturetheflag.domain.game.ProgressState
import com.lamti.capturetheflag.domain.player.GameDetails
import com.lamti.capturetheflag.domain.player.Player
import com.lamti.capturetheflag.domain.player.PlayerDetails
import com.lamti.capturetheflag.domain.player.Team
import com.lamti.capturetheflag.utils.EMPTY
import com.lamti.capturetheflag.utils.FIRESTORE_LOGGER_TAG
import com.lamti.capturetheflag.utils.emptyPosition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.Date
import javax.inject.Inject

class FirestoreRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val authenticationRepository: AuthenticationRepository,
    private val databaseRepository: FirebaseDatabaseRepository,
    private val playersRepository: PlayersRepository
) : FirestoreRepository {

    // Authentication
    private val userID = authenticationRepository.currentUser?.uid ?: EMPTY

    override suspend fun registerUser(
        email: String,
        password: String,
        username: String
    ): Boolean = authenticationRepository.registerUser(email = email, password = password)?.let { uid ->
        playersRepository.updatePlayer(
            Player(
                userID = uid,
                status = Player.Status.Online,
                details = PlayerDetails(username = username, email = email),
                gameDetails = null
            )
        )
        true
    } ?: false

    override suspend fun loginUser(email: String, password: String) = authenticationRepository.loginUser(email, password)

    override fun logout() = authenticationRepository.logout()


    // Players
    override fun observePlayer(): Flow<Player> = playersRepository.observePlayer(userID)

    override suspend fun joinPlayer(player: Player, gameID: String) = playersRepository.joinPlayer(player, gameID)

    override suspend fun connectPlayer(player: Player): Boolean = playersRepository.connectPlayer(player)

    override suspend fun getPlayer(): Player? = playersRepository.getPlayer(userID)

    override suspend fun updatePlayer(player: Player) = playersRepository.updatePlayer(player)


    // Firebase Database
    override suspend fun uploadGamePlayer(position: LatLng) {
        val currentPlayer = getPlayer() ?: return
        if (currentPlayer.status != Player.Status.Playing) return
        val gameDetails = currentPlayer.gameDetails ?: return

        databaseRepository.updateGamePlayer(
            gameID = gameDetails.gameID,
            player = GamePlayer(
                id = userID,
                team = gameDetails.team,
                position = position,
                username = currentPlayer.details.username
            )
        )
    }

    override fun observePlayersPosition(gameID: String) = databaseRepository.observePlayersPosition(gameID)


    // Games
    override fun observeGame(): Flow<Game> = callbackFlow {
        var snapshotListener: ListenerRegistration? = null
        try {
            val player = getPlayer()
            val gameID = player?.gameDetails?.gameID ?: EMPTY

            snapshotListener = firestore
                .collection(COLLECTION_GAMES)
                .document(gameID)
                .addSnapshotListener { snapshot, e ->
                    val state = snapshot?.toObject(GameRaw::class.java)?.toGame() ?: return@addSnapshotListener
                    trySend(state).isSuccess
                }
        } catch (e: Exception) {
            Timber.e("Observe game error: ${e.message}")
        }

        awaitClose {
            snapshotListener?.remove()
        }
    }

    override suspend fun createGame(id: String, title: String, position: LatLng, player: Player): Boolean {
        try {
            initialGame(id, title, position).toRaw().update()
        } catch (e: Exception) {
            Timber.e("Update initial game error: ${e.message}")
            return false
        }

        playersRepository.updatePlayer(
            player = player
                .copy(
                    gameDetails = GameDetails(
                        gameID = id,
                        team = Team.Red,
                        rank = GameDetails.Rank.Captain
                    ),
                    status = Player.Status.Connecting
                )
        )

        return true
    }

    private fun initialGame(
        id: String,
        title: String,
        position: LatLng
    ) = Game(
        gameID = id,
        title = title,
        gameState = GameState(
            safehouse = GeofenceObject(
                position = position,
                isPlaced = true,
                isDiscovered = true,
                id = EMPTY,
                timestamp = Date()
            ),
            greenFlag = GeofenceObject(
                position = emptyPosition(),
                isPlaced = false,
                isDiscovered = false,
                id = EMPTY,
                timestamp = Date()
            ),
            redFlag = GeofenceObject(
                position = emptyPosition(),
                isPlaced = false,
                isDiscovered = false,
                id = EMPTY,
                timestamp = Date()
            ),
            greenFlagCaptured = null,
            redFlagCaptured = null,
            state = ProgressState.Created,
            winners = Team.Unknown
        ),
        redPlayers = listOf(ActivePlayer(id = userID, hasLost = false)),
        greenPlayers = emptyList(),
        battles = emptyList()
    )

    override suspend fun endGame(game: Game, team: Team) = game
        .copy(
            gameState = game.gameState.copy(
                state = ProgressState.Ended,
                winners = team
            )
        )
        .toRaw()
        .update()

    override suspend fun getGame(id: String): Game? = withContext(Dispatchers.IO) {
        try {
            firestore
                .collection(COLLECTION_GAMES)
                .document(id)
                .get()
                .await()
                .toObject(GameRaw::class.java)
                ?.toGame()
        } catch (e: Exception) {
            Timber.e("[$FIRESTORE_LOGGER_TAG] ${e.message}")
            null
        }
    }

    override suspend fun updateSafehousePosition(game: Game, position: LatLng): Boolean = game
        .copy(
            gameState = game.gameState.copy(
                state = ProgressState.SettingFlags,
                safehouse = game.gameState.safehouse.copy(position = position)
            )
        )
        .toRaw()
        .update()

    override suspend fun discoverFlag(flagFound: Flag): Boolean {
        val player = getPlayer()
        val gameID = player?.gameDetails?.gameID ?: return false
        val team = player.gameDetails.team
        val game = getGame(gameID) ?: return false

        val updatedGame: Game = when {
            team == Team.Red && flagFound == Flag.Green -> game.copy(
                gameState = game.gameState.copy(
                    greenFlag = game.gameState.greenFlag.copy(isDiscovered = true)
                )
            )
            team == Team.Green && flagFound == Flag.Red -> game.copy(
                gameState = game.gameState.copy(
                    redFlag = game.gameState.redFlag.copy(isDiscovered = true)
                )
            )
            else -> return false
        }

        updatedGame.toRaw().update()
        return true
    }

    override suspend fun captureFlag(): Boolean {
        val currentPlayer = getPlayer() ?: return false
        val gameDetails = currentPlayer.gameDetails ?: return false
        val currentGame = getGame(gameDetails.gameID) ?: return false

        return when (gameDetails.team) {
            Team.Red -> currentGame.copy(gameState = currentGame.gameState.copy(greenFlagCaptured = currentPlayer.userID))
            Team.Green -> currentGame.copy(gameState = currentGame.gameState.copy(redFlagCaptured = currentPlayer.userID))
            Team.Unknown -> currentGame.copy(gameState = currentGame.gameState.copy())
        }.toRaw().update()
    }

    override suspend fun createBattle(opponentID: String, game: Game): Boolean = game
        .copy(
            battles = game.battles + Battle(
                battleID = userID,
                playersIDs = listOf(userID, opponentID)
            )
        )
        .toRaw()
        .update()

    private suspend fun GameRaw.update(): Boolean = try {
        firestore
            .collection(COLLECTION_GAMES)
            .document(gameID)
            .set(this, SetOptions.merge())
            .await()
        true
    } catch (e: Exception) {
        Timber.e("[$FIRESTORE_LOGGER_TAG] ${e.message}")
        false
    }


    // Player and Game
    override suspend fun lost(player: Player, game: Game) {

        val updatedBattles: MutableList<Battle> = game.battles.toMutableList()
        updatedBattles.removeIf { it.playersIDs.contains(player.userID) }

        val updatedGameState = when (player.userID) {
            game.gameState.redFlagCaptured -> game.gameState.copy(redFlagCaptured = null)
            game.gameState.greenFlagCaptured -> game.gameState.copy(greenFlagCaptured = null)
            else -> game.gameState
        }

        val (redPlayers, greenPlayers) = when (player.gameDetails?.team) {
            Team.Red -> {
                val redPlayers = game.redPlayers.map { if (it.id == player.userID) it.copy(hasLost = true) else it }
                Pair(redPlayers, game.greenPlayers)
            }
            Team.Green -> {
                val greenPlayers = game.greenPlayers.map {
                    if (it.id == player.userID) it.copy(hasLost = true) else it
                }
                Pair(game.redPlayers, greenPlayers)
            }
            else -> Pair(game.redPlayers, game.greenPlayers)
        }

        game
            .copy(
                battles = updatedBattles,
                gameState = updatedGameState,
                redPlayers = redPlayers,
                greenPlayers = greenPlayers
            )
            .toRaw()
            .update()

        playersRepository.updatePlayer(player = player.copy(status = Player.Status.Lost))
        databaseRepository.deleteGamePlayer(game.gameID, player.userID)
    }

    override suspend fun setPlayerTeam(player: Player) {
        val team = player.gameDetails?.team ?: Team.Unknown
        val gameDetails = player.gameDetails
        val gameID = gameDetails?.gameID ?: EMPTY
        val game = getGame(gameID) ?: return

        val rank = when (team) {
            Team.Green -> {
                when (game.greenPlayers.isEmpty()) {
                    true -> GameDetails.Rank.Leader
                    false -> GameDetails.Rank.Soldier
                }
            }
            else -> GameDetails.Rank.Soldier
        }

        playersRepository.updatePlayer(
            player = player
                .copy(
                    gameDetails = gameDetails?.copy(
                        gameID = gameID,
                        team = team,
                        rank = rank
                    )
                )
        )
        addPlayerToGame(game, team, player.userID)
    }

    private suspend fun addPlayerToGame(game: Game, playerTeam: Team, playerID: String) {
        val updatedGame: Game = when (playerTeam) {
            Team.Red -> {
                val newList = game.redPlayers.toMutableList()
                newList.add(ActivePlayer(id = playerID, hasLost = false))
                game.copy(redPlayers = newList)
            }
            Team.Green -> {
                val newList = game.greenPlayers.toMutableList()
                newList.add(ActivePlayer(id = playerID, hasLost = false))
                game.copy(greenPlayers = newList)
            }
            Team.Unknown -> game
        }

        updatedGame.toRaw().update()
    }

    companion object {

        const val COLLECTION_GAMES = "games"
    }
}
