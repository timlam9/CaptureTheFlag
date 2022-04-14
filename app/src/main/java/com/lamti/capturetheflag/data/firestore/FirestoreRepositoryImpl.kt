package com.lamti.capturetheflag.data.firestore

import com.google.android.gms.maps.model.LatLng
import com.lamti.capturetheflag.data.authentication.AuthenticationRepository
import com.lamti.capturetheflag.domain.FirestoreRepository
import com.lamti.capturetheflag.domain.game.Game
import com.lamti.capturetheflag.domain.game.GamePlayer
import com.lamti.capturetheflag.domain.player.Player
import com.lamti.capturetheflag.domain.player.PlayerDetails
import com.lamti.capturetheflag.utils.EMPTY
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class FirestoreRepositoryImpl @Inject constructor(
    private val authenticationRepository: AuthenticationRepository,
    private val databaseRepository: FirebaseDatabaseRepository,
    private val playersRepository: PlayersRepository,
    private val gamesRepository: GamesRepository
) : FirestoreRepository {

    // Authentication
    private val userID = authenticationRepository.getCurrentUser()?.uid ?: EMPTY

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

    override suspend fun getPlayer(): Player? = playersRepository.getPlayer(userID)

    override suspend fun updatePlayer(player: Player) = playersRepository.updatePlayer(player)

    // Games
    override fun observeGame(gameID: String): Flow<Game> = gamesRepository.observeGame(gameID)

    override suspend fun getGame(id: String): Game? = gamesRepository.getGame(id)

    override suspend fun updateGame(game: Game): Boolean = gamesRepository.updateGame(game)

    override suspend fun createGame(
        id: String,
        title: String,
        position: LatLng,
        player: Player
    ): Boolean = gamesRepository.createGame(
        id = id,
        title = title,
        position = position,
        userID = player.userID
    )

    // Firebase Database
    override fun observePlayersPosition(gameID: String) = databaseRepository.observePlayersPosition(gameID)

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

    override suspend fun deleteGamePlayer(gameID: String, playerID: String): Boolean =
        databaseRepository.deleteGamePlayer(gameID, userID)

    override fun clearCache(): Boolean = gamesRepository.clearCache()
}
