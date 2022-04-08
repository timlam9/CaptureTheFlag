package com.lamti.capturetheflag.data.firestore

import com.google.android.gms.maps.model.LatLng
import com.lamti.capturetheflag.data.authentication.AuthenticationRepository
import com.lamti.capturetheflag.domain.FirestoreRepository
import com.lamti.capturetheflag.domain.game.ActivePlayer
import com.lamti.capturetheflag.domain.game.Battle
import com.lamti.capturetheflag.domain.game.Flag
import com.lamti.capturetheflag.domain.game.Game
import com.lamti.capturetheflag.domain.game.GamePlayer
import com.lamti.capturetheflag.domain.game.ProgressState
import com.lamti.capturetheflag.domain.player.GameDetails
import com.lamti.capturetheflag.domain.player.Player
import com.lamti.capturetheflag.domain.player.PlayerDetails
import com.lamti.capturetheflag.domain.player.Team
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

    override suspend fun getPlayer(): Player? = playersRepository.getPlayer(userID)

    override suspend fun joinPlayer(player: Player, gameID: String) = playersRepository.joinPlayer(player, gameID)

    override suspend fun connectPlayer(player: Player): Boolean = playersRepository.connectPlayer(player)

    override suspend fun updatePlayer(player: Player) = playersRepository.updatePlayer(player)


    // Games
    override fun observeGame(gameID: String): Flow<Game> = gamesRepository.observeGame(gameID = gameID)

    override suspend fun getGame(id: String): Game? = gamesRepository.getGame(id)

    override suspend fun endGame(game: Game, team: Team) = gamesRepository.updateGame(
        game = game
            .copy(
                gameState = game.gameState.copy(
                    state = ProgressState.Ended,
                    winners = team
                )
            )
    )

    override suspend fun updateSafehousePosition(game: Game, position: LatLng): Boolean = gamesRepository.updateGame(
        game = game.copy(
            gameState = game.gameState.copy(
                state = ProgressState.SettingFlags,
                safehouse = game.gameState.safehouse.copy(position = position)
            )
        )
    )

    override suspend fun createBattle(opponentID: String, game: Game): Boolean = gamesRepository.updateGame(
        game = game
            .copy(
                battles = game.battles + Battle(
                    battleID = userID,
                    playersIDs = listOf(userID, opponentID)
                )
            )
    )


    // Player and Game
    override suspend fun discoverFlag(flagFound: Flag): Boolean {
        val player = getPlayer()
        val gameID = player?.gameDetails?.gameID ?: return false
        val team = player.gameDetails.team
        val currentGame = getGame(gameID) ?: return false

        val game: Game = when {
            team == Team.Red && flagFound == Flag.Green -> currentGame.copy(
                gameState = currentGame.gameState.copy(
                    greenFlag = currentGame.gameState.greenFlag.copy(isDiscovered = true)
                )
            )
            team == Team.Green && flagFound == Flag.Red -> currentGame.copy(
                gameState = currentGame.gameState.copy(
                    redFlag = currentGame.gameState.redFlag.copy(isDiscovered = true)
                )
            )
            else -> return false
        }

        return gamesRepository.updateGame(game)
    }

    override suspend fun captureFlag(): Boolean {
        val currentPlayer = getPlayer() ?: return false
        val gameDetails = currentPlayer.gameDetails ?: return false
        val currentGame = getGame(gameDetails.gameID) ?: return false

        val game = when (gameDetails.team) {
            Team.Red -> currentGame.copy(gameState = currentGame.gameState.copy(greenFlagCaptured = currentPlayer.userID))
            Team.Green -> currentGame.copy(gameState = currentGame.gameState.copy(redFlagCaptured = currentPlayer.userID))
            Team.Unknown -> currentGame.copy(gameState = currentGame.gameState.copy())
        }

        return gamesRepository.updateGame(game)
    }

    override suspend fun createGame(id: String, title: String, position: LatLng, player: Player): Boolean {
        gamesRepository.createGame(
            id = id,
            title = title,
            position = position,
            userID = player.userID
        )
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

        gamesRepository.updateGame(
            game = game
                .copy(
                    battles = updatedBattles,
                    gameState = updatedGameState,
                    redPlayers = redPlayers,
                    greenPlayers = greenPlayers
                )
        )
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

    private suspend fun addPlayerToGame(currentGame: Game, playerTeam: Team, playerID: String) {
        val game: Game = when (playerTeam) {
            Team.Red -> {
                val newList = currentGame.redPlayers.toMutableList()
                newList.add(ActivePlayer(id = playerID, hasLost = false))
                currentGame.copy(redPlayers = newList)
            }
            Team.Green -> {
                val newList = currentGame.greenPlayers.toMutableList()
                newList.add(ActivePlayer(id = playerID, hasLost = false))
                currentGame.copy(greenPlayers = newList)
            }
            Team.Unknown -> currentGame
        }

        gamesRepository.updateGame(game)
    }


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
}
