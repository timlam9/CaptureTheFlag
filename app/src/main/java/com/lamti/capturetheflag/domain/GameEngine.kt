package com.lamti.capturetheflag.domain

import android.graphics.Bitmap
import android.graphics.Color
import android.location.Location
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import com.google.android.gms.maps.model.LatLng
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.lamti.capturetheflag.data.location.LocationRepository
import com.lamti.capturetheflag.data.location.geofences.GeofencingRepository
import com.lamti.capturetheflag.domain.game.ActivePlayer
import com.lamti.capturetheflag.domain.game.Battle
import com.lamti.capturetheflag.domain.game.BattleMiniGame
import com.lamti.capturetheflag.domain.game.BattleState
import com.lamti.capturetheflag.domain.game.BattlingPlayer
import com.lamti.capturetheflag.domain.game.Game
import com.lamti.capturetheflag.domain.game.GamePlayer
import com.lamti.capturetheflag.domain.game.GameState
import com.lamti.capturetheflag.domain.game.ProgressState
import com.lamti.capturetheflag.domain.player.GameDetails
import com.lamti.capturetheflag.domain.player.Player
import com.lamti.capturetheflag.domain.player.Team
import com.lamti.capturetheflag.presentation.ui.DEFAULT_BATTLE_RANGE
import com.lamti.capturetheflag.presentation.ui.components.navigation.Screen
import com.lamti.capturetheflag.presentation.ui.fragments.ar.ArMode
import com.lamti.capturetheflag.presentation.ui.getRandomString
import com.lamti.capturetheflag.presentation.ui.toLatLng
import com.lamti.capturetheflag.utils.EMPTY
import com.lamti.capturetheflag.utils.GEOFENCE_LOGGER_TAG
import com.lamti.capturetheflag.utils.LOGGER_TAG
import com.lamti.capturetheflag.utils.emptyPosition
import com.lamti.capturetheflag.utils.isInRangeOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject

class GameEngine @Inject constructor(
    private val firestoreRepository: FirestoreRepository,
    private val locationRepository: LocationRepository,
    private val geofencingRepository: GeofencingRepository,
    private val coroutineScope: CoroutineScope
) {

    private val _livePosition: MutableStateFlow<LatLng> = MutableStateFlow(emptyPosition())
    val livePosition: StateFlow<LatLng> = _livePosition

    private val _initialPosition: MutableStateFlow<LatLng> = MutableStateFlow(emptyPosition())
    val initialPosition: StateFlow<LatLng> = _initialPosition

    private val _player: MutableStateFlow<Player> = MutableStateFlow(Player.emptyPlayer())
    val player: StateFlow<Player> = _player

    private val _game: MutableState<Game> = mutableStateOf(Game.initialGame(position = livePosition.value))
    val game: State<Game> = _game

    private val _initialScreen: MutableStateFlow<Screen> = MutableStateFlow(Screen.Menu)
    val initialScreen: StateFlow<Screen> = _initialScreen

    private val _stayInSplashScreen: MutableStateFlow<Boolean> = MutableStateFlow(true)
    val stayInSplashScreen: StateFlow<Boolean> = _stayInSplashScreen

    private val _enterBattleScreen: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val enterBattleScreen: StateFlow<Boolean> = _enterBattleScreen

    private val _showArFlagButton: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val showArFlagButton: StateFlow<Boolean> = _showArFlagButton

    private val _showBattleButton: MutableStateFlow<String> = MutableStateFlow(EMPTY)
    val showBattleButton: StateFlow<String> = _showBattleButton

    private val _battleState: MutableStateFlow<BattleState> = MutableStateFlow(BattleState.StandBy)
    val battleState: StateFlow<BattleState> = _battleState

    private val _battleWinner: MutableStateFlow<String> = MutableStateFlow(EMPTY)
    val battleWinner: StateFlow<String> = _battleWinner

    private val _battleWinnerID = MutableStateFlow(EMPTY)
    val battleWinnerID: StateFlow<String> = _battleWinnerID

    private val _battleRequestInProgress = MutableStateFlow(false)
    val battleRequestInProgress: StateFlow<Boolean> = _battleRequestInProgress

    private val _battleRequestFailed = MutableStateFlow(false)
    val battleRequestFailed: StateFlow<Boolean> = _battleRequestFailed

    private val battleRequestMutex = Mutex()

    fun clearBattleRequestFailure() {
        _battleRequestFailed.value = false
    }

    private val _isPlayerReadyToBattle: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isPlayerReadyToBattle: StateFlow<Boolean> = _isPlayerReadyToBattle

    private val _enterGameOverScreen: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val enterGameOverScreen: StateFlow<Boolean> = _enterGameOverScreen

    private val _isSafehouseDraggable: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isSafehouseDraggable: StateFlow<Boolean> = _isSafehouseDraggable

    private val _canPlaceFlag: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val canPlaceFlag: StateFlow<Boolean> = _canPlaceFlag

    private val _otherPlayers: MutableStateFlow<List<GamePlayer>> = MutableStateFlow(emptyList())
    val otherPlayers: StateFlow<List<GamePlayer>> = _otherPlayers

    private val _qrCodeBitmap: MutableStateFlow<Bitmap?> = MutableStateFlow(null)
    val qrCodeBitmap: StateFlow<Bitmap?> = _qrCodeBitmap

    private val _arMode: MutableStateFlow<ArMode> = MutableStateFlow(ArMode.Placer)
    val arMode: StateFlow<ArMode> = _arMode

    private val _enteredGeofenceId = mutableStateOf(EMPTY)
    private val _battleID = mutableStateOf(EMPTY)
    private var hasGeofenceListenerStarted = false
    private var gameObserverJob: Job? = null
    private var observedGameID: String? = null
    private var otherPlayersJob: Job? = null
    private var otherPlayersGameID: String? = null
    private var locationUpdatesJob: Job? = null
    private var lossCleanupJob: Deferred<Boolean>? = null
    private var lossCleanupGameID: String? = null
    private var lossCleanupPlayerID: String? = null

    fun observePlayer(): Job = firestoreRepository.observePlayer().onEach { updatedPlayer ->
        updatedPlayer.run {
            if (_player.value.userID != userID || _player.value.gameDetails?.gameID != gameDetails?.gameID) {
                cancelLossCleanup()
            }
            if (observedGameID != null && observedGameID != gameDetails?.gameID) {
                stopGameObservers()
                locationUpdatesJob?.cancel()
                locationUpdatesJob = null
            }
            _player.value = this
            reconcilePlayerLoss(_game.value)
            updateBattle()
            _game.value.battles.searchOpponent()
            foundOpponentToBattle(_otherPlayers.value)
            _stayInSplashScreen.value = false
            _initialScreen.value = getInitialScreen()
        }
    }.catch {
        Timber.e("[$LOGGER_TAG] Catch observe player error")
    }.launchIn(coroutineScope)

    fun observeGame(): Job {
        stopGameObservers()
        return coroutineScope.launch {
            val previousPlayer = _player.value
            firestoreRepository.getPlayer()?.let {
                if (_player.value.userID != previousPlayer.userID ||
                    _player.value.gameDetails?.gameID != previousPlayer.gameDetails?.gameID) return@launch
                if (_player.value.userID != it.userID || _player.value.gameDetails?.gameID != it.gameDetails?.gameID) {
                    cancelLossCleanup()
                }
                _player.value = it
                reconcilePlayerLoss(_game.value)
            }
            val id = _player.value.gameDetails?.gameID?.takeIf { it.isNotBlank() } ?: return@launch
            if (_game.value.gameID != id) {
                locationUpdatesJob?.cancel()
                locationUpdatesJob = null
                removeGeofencesListener()
            }
            observedGameID = id
            firestoreRepository.observeGame(id).onEach { game ->
                if (observedGameID != id || _player.value.gameDetails?.gameID != id) return@onEach
                _stayInSplashScreen.value = false
                publishBattleSnapshot(game)
                game.gameState.handleGameStateEvents()
            }.catch {
                Timber.e("[$LOGGER_TAG] Catch observe game error")
            }.collect()
        }.also { gameObserverJob = it }
    }

    private fun stopOtherPlayers() {
        otherPlayersJob?.cancel()
        otherPlayersJob = null
        otherPlayersGameID = null
        _otherPlayers.value = emptyList()
        _battleID.value = EMPTY
        _showBattleButton.value = EMPTY
    }

    private fun stopGameObservers() {
        gameObserverJob?.cancel()
        gameObserverJob = null
        observedGameID = null
        stopOtherPlayers()
        _enterBattleScreen.value = false
        _battleState.value = BattleState.StandBy
        _battleWinner.value = EMPTY
        _battleWinnerID.value = EMPTY
        _isPlayerReadyToBattle.value = false
    }

    private fun publishBattleSnapshot(game: Game) {
        if (_player.value.gameDetails?.gameID != game.gameID) return
        _game.value = game
        reconcilePlayerLoss(game)
        updateBattle()
        game.battles.searchOpponent()
        foundOpponentToBattle(_otherPlayers.value)
    }

    private fun cancelLossCleanup() {
        lossCleanupJob?.cancel()
        lossCleanupJob = null
        lossCleanupGameID = null
        lossCleanupPlayerID = null
    }

    private fun isCurrentGamePlayer(gameID: String, playerID: String): Boolean =
        gameID.isNotBlank() && playerID.isNotBlank() &&
            _game.value.gameID == gameID && _player.value.userID == playerID &&
            _player.value.gameDetails?.gameID == gameID

    private fun reconcilePlayerLoss(game: Game) {
        val currentPlayer = _player.value
        if (currentPlayer.status == Player.Status.Lost ||
            !isCurrentGamePlayer(game.gameID, currentPlayer.userID) ||
            !game.hasLost(currentPlayer.userID)) return
        startLossCleanup(game.gameID, currentPlayer.userID)
    }

    private fun Game.hasLost(playerID: String): Boolean =
        (redPlayers + greenPlayers).any { it.id == playerID && it.hasLost }

    private fun startLossCleanup(gameID: String, playerID: String): Deferred<Boolean> {
        lossCleanupJob?.takeIf {
            it.isActive && lossCleanupGameID == gameID && lossCleanupPlayerID == playerID
        }?.let { return it }
        cancelLossCleanup()
        lossCleanupGameID = gameID
        lossCleanupPlayerID = playerID
        return coroutineScope.async(start = CoroutineStart.LAZY) {
            if (!isCurrentGamePlayer(gameID, playerID)) return@async false
            val currentPlayer = _player.value
            val updated = currentPlayer.status == Player.Status.Lost ||
                firestoreRepository.updatePlayer(currentPlayer.copy(status = Player.Status.Lost))
            currentCoroutineContext().ensureActive()
            if (!isCurrentGamePlayer(gameID, playerID)) return@async false
            if (updated && _player.value.status != Player.Status.Lost) {
                _player.value = _player.value.copy(status = Player.Status.Lost)
            }
            // Always retry RTDB removal, including when the player is already Lost.
            val deleted = firestoreRepository.deleteGamePlayer(gameID, playerID)
            updated && deleted
        }.also {
            lossCleanupJob = it
            it.start()
        }
    }

    private fun updateBattle() {
        val playerBattle = findPlayerBattle()
        _battleState.value = playerBattle?.state ?: BattleState.StandBy
        _battleWinner.value = playerBattle?.winner ?: EMPTY
        _battleWinnerID.value = playerBattle?.winnerID ?: EMPTY
        _isPlayerReadyToBattle.value =
            playerBattle?.players?.firstOrNull { it.id == _player.value.userID }?.ready ?: false
    }

    suspend fun getGame(id: String): Game? = firestoreRepository.getGame(id)

    suspend fun getLastLocation(): Location = locationRepository.awaitLastLocation().apply {
        _initialPosition.value = toLatLng()
    }

    fun setEnteredGeofenceId(id: String) {
        _enteredGeofenceId.value = id
        setCaptureFlagButtonVisibility(id)
    }

    fun logout() = firestoreRepository.logout()

    suspend fun createNewGameWithRedCaptain(title: String, miniGame: BattleMiniGame) = coroutineScope.launch {
        val gameID = getRandomString(GAME_CODE_LENGTH)
        generateQrCode(gameID)

        cancelLossCleanup()
        _player.value = _player.value.copy(
            gameDetails = GameDetails(
                gameID = gameID,
                team = Team.Red,
                rank = GameDetails.Rank.Captain
            ),
            status = Player.Status.Connecting
        )

        firestoreRepository.updatePlayer(player = _player.value)
        firestoreRepository.createGame(
            id = gameID,
            title = title,
            miniGame = miniGame,
            position = _initialPosition.value,
            player = _player.value
        )

        observeGame()
    }

    suspend fun addPlayerToGame(gameID: String) = coroutineScope.launch {
        if (_player.value.gameDetails?.gameID != gameID) cancelLossCleanup()
        firestoreRepository.updatePlayer(
            player = _player.value.copy(
                gameDetails = GameDetails(
                    gameID = gameID,
                    team = Team.Unknown,
                    rank = GameDetails.Rank.Soldier
                ),
                status = Player.Status.Connecting
            )
        )

        observeGame()
    }

    fun changePlayerTeam(team: Team) {
        _player.value = _player.value.copy(gameDetails = _player.value.gameDetails?.copy(team = team))
    }

    suspend fun addPlayerToTeam() {
        val joiningPlayer = _player.value
        val gameDetails = joiningPlayer.gameDetails ?: return
        val gameID = gameDetails.gameID
        val team = gameDetails.team
        if (gameID.isBlank() || joiningPlayer.userID.isBlank() || team == Team.Unknown) return
        val updated = firestoreRepository.updateGame(gameID) { latest ->
            when (team) {
                Team.Red -> if (latest.redPlayers.any { it.id == joiningPlayer.userID }) latest else
                    latest.copy(redPlayers = latest.redPlayers + ActivePlayer(joiningPlayer.userID, false))
                Team.Green -> if (latest.greenPlayers.any { it.id == joiningPlayer.userID }) latest else
                    latest.copy(greenPlayers = latest.greenPlayers + ActivePlayer(joiningPlayer.userID, false))
                else -> latest
            }
        }
        if (!updated) return
        val joinedGame = getGame(gameID) ?: return
        val rank = if (team == Team.Green && joinedGame.greenPlayers.firstOrNull()?.id == joiningPlayer.userID)
            GameDetails.Rank.Leader else GameDetails.Rank.Soldier
        firestoreRepository.updatePlayer(
            joiningPlayer.copy(gameDetails = gameDetails.copy(rank = rank))
        )
        if (_player.value.gameDetails?.gameID == gameID) publishBattleSnapshot(joinedGame)
    }

    suspend fun startGame() = coroutineScope.launch {
        firestoreRepository.updatePlayer(_player.value.copy(status = Player.Status.Playing))
    }

    suspend fun updateSafehouseAndForwardGameState(position: LatLng, gameRadius: Float, flagRadius: Float) {
        val gameID = _game.value.gameID
        firestoreRepository.updateGame(gameID) { latest ->
            latest.copy(
                gameRadius = gameRadius,
                flagRadius = flagRadius,
                gameState = latest.gameState.copy(
                    state = ProgressState.SettingFlags,
                    safehouse = latest.gameState.safehouse.copy(position = position)
                )
            )
        }
    }

    suspend fun createBattle(): Boolean {
        val requestedGame = _game.value
        val requestingPlayer = _player.value
        val opponentID = _battleID.value
        if (!battleRequestMutex.tryLock()) return false
        _battleRequestInProgress.value = true
        clearBattleRequestFailure()
        try {
            val playerID = requestingPlayer.userID
            val ownTeam = when {
                requestedGame.redPlayers.any { it.id == playerID && !it.hasLost } -> Team.Red
                requestedGame.greenPlayers.any { it.id == playerID && !it.hasLost } -> Team.Green
                else -> Team.Unknown
            }
            val opponents = when (ownTeam) {
                Team.Red -> requestedGame.greenPlayers
                Team.Green -> requestedGame.redPlayers
                else -> emptyList()
            }
            val valid = requestedGame.gameID.isNotBlank() && playerID.isNotBlank() &&
                opponentID.isNotBlank() && opponentID != playerID &&
                requestingPlayer.gameDetails?.gameID == requestedGame.gameID &&
                requestingPlayer.status != Player.Status.Lost &&
                requestedGame.gameState.state == ProgressState.Started &&
                opponents.any { it.id == opponentID && !it.hasLost } &&
                requestedGame.battles.none { battle ->
                    battle.players.any { it.id == playerID || it.id == opponentID }
                }
            val accepted = valid && firestoreRepository.updateBattles(
                requestedGame.gameID,
                Battle(
                    battleID = UUID.randomUUID().toString(),
                    state = BattleState.StandBy,
                    winner = EMPTY,
                    players = listOf(BattlingPlayer(playerID, false), BattlingPlayer(opponentID, false))
                )
            )
            if (!accepted) {
                _battleRequestFailed.value = true
                if (requestedGame.gameID.isNotBlank()) {
                    firestoreRepository.getGame(requestedGame.gameID)?.let { refreshed ->
                        if (_game.value.gameID == requestedGame.gameID) publishBattleSnapshot(refreshed)
                    }
                }
            }
            return accepted
        } finally {
            _battleRequestInProgress.value = false
            battleRequestMutex.unlock()
        }
    }

    suspend fun readyToBattle(): Boolean {
        val currentGame = _game.value
        val currentPlayer = _player.value
        val battle = currentGame.battles.firstOrNull { battle ->
            battle.players.any { it.id == currentPlayer.userID }
        } ?: return false
        return firestoreRepository.updateReadyToBattle(currentGame.gameID, battle.battleID, currentPlayer.userID)
    }

    private fun findPlayerBattle(): Battle? {
        if (_player.value.gameDetails?.gameID != _game.value.gameID) return null
        return _game.value.battles.firstOrNull { battle ->
            battle.players.any { it.id == _player.value.userID }
        }
    }

    suspend fun onBattleWinnerFound(): Boolean {
        val currentGame = _game.value
        val currentPlayer = _player.value
        val battle = currentGame.battles.firstOrNull { battle ->
            battle.players.any { it.id == currentPlayer.userID }
        } ?: return false
        return firestoreRepository.finishBattle(
            currentGame.gameID, battle.battleID, currentPlayer.userID, currentPlayer.details.username
        )
    }

    suspend fun looseBattle(): Boolean {
        val currentGame = _game.value
        val currentPlayer = _player.value
        val gameID = currentGame.gameID
        val playerID = currentPlayer.userID
        if (!isCurrentGamePlayer(gameID, playerID)) return false
        val battle = currentGame.battles.firstOrNull { battle ->
            battle.players.any { it.id == playerID }
        }
        if (battle == null) {
            return currentGame.hasLost(playerID) && startLossCleanup(gameID, playerID).await()
        }
        if (!firestoreRepository.acknowledgeBattle(
                gameID, battle.battleID, playerID, currentPlayer.details.username
            )) return false
        if (!isCurrentGamePlayer(gameID, playerID)) return false
        val lossSnapshot = if (_game.value.hasLost(playerID)) _game.value else
            firestoreRepository.getGame(gameID) ?: return false
        // The live observer owns publication; a one-shot read can be older than its latest snapshot.
        if (!isCurrentGamePlayer(gameID, playerID)) return false
        if (!lossSnapshot.hasLost(playerID)) return true
        return startLossCleanup(gameID, playerID).await()
    }

    suspend fun removePlayer(onResult: (Boolean) -> Unit) {
        val leavingPlayer = _player.value
        val gameID = leavingPlayer.gameDetails?.gameID.orEmpty()
        val playerID = leavingPlayer.userID
        val left = playerID.isNotBlank() &&
            (gameID.isBlank() || firestoreRepository.leaveGame(gameID, playerID))
        if (!left) {
            if (gameID.isNotBlank() && _player.value.userID == playerID &&
                _player.value.gameDetails?.gameID == gameID &&
                (observedGameID != gameID || gameObserverJob?.isActive != true)) {
                observeGame()
            }
            onResult(false)
            return
        }
        // A delayed leave must never disconnect or overwrite a newly selected game/player.
        if (_player.value.userID != playerID || _player.value.gameDetails?.gameID.orEmpty() != gameID) {
            onResult(false)
            return
        }
        cancelLossCleanup()
        stopGameObservers()
        locationUpdatesJob?.cancel()
        locationUpdatesJob = null
        removeGeofencesListener()
        val departedPlayer = _player.value.copy(status = Player.Status.Online, gameDetails = null)
        val updated = firestoreRepository.updatePlayer(departedPlayer)
        currentCoroutineContext().ensureActive()
        if (!updated) {
            if (_player.value.userID == playerID && _player.value.gameDetails?.gameID.orEmpty() == gameID) {
                observeGame()
            }
            onResult(false)
            return
        }
        if (_player.value.userID == playerID && _player.value.gameDetails?.gameID.orEmpty() == gameID) {
            // Roster departure and player membership updates have both succeeded.
            _player.value = _player.value.copy(status = Player.Status.Online, gameDetails = null)
            _initialScreen.value = Screen.Menu
        }
        val deleted = gameID.isBlank() || firestoreRepository.deleteGamePlayer(gameID, playerID)
        if (gameID.isNotBlank()) checkForGameDeletion(gameID)
        onResult(updated && deleted)
    }

    private suspend fun checkForGameDeletion(gameID: String) {
        val latest = getGame(gameID) ?: return
        if (latest.redPlayers.isEmpty() && latest.greenPlayers.isEmpty()) {
            firestoreRepository.deleteFirebaseGame(gameID)
            firestoreRepository.deleteGame(gameID)
        }
    }

    suspend fun captureFlag(onResult: (Boolean) -> Unit) {
        val capturingPlayer = _player.value
        val gameID = _game.value.gameID
        val updated = firestoreRepository.updateGame(gameID) { latest ->
            when {
                latest.redPlayers.any { it.id == capturingPlayer.userID && !it.hasLost } ->
                    latest.copy(gameState = latest.gameState.copy(greenFlagCaptured = capturingPlayer.userID))
                latest.greenPlayers.any { it.id == capturingPlayer.userID && !it.hasLost } ->
                    latest.copy(gameState = latest.gameState.copy(redFlagCaptured = capturingPlayer.userID))
                else -> latest
            }
        }
        onResult(updated)
    }

    private suspend fun GameState.handleGameStateEvents(): Unit = when (state) {
        ProgressState.Created -> {
            _enterGameOverScreen.value = false
            _isSafehouseDraggable.value = _player.value.gameDetails?.rank == GameDetails.Rank.Captain
            removeGeofencesListener()
        }
        ProgressState.SettingFlags -> {
            // If player has left the game then don't observe SettingFlags event
            if (_player.value.gameDetails?.gameID != null && _player.value.gameDetails?.gameID!!.isNotEmpty()) {
                _enterGameOverScreen.value = false
                _isSafehouseDraggable.value = false
                connectPlayer()
                startLocationUpdates()
                _arMode.value = ArMode.Placer
                addGameOverByPlayersCountListener()
            } else Unit
        }
        ProgressState.Started -> {
            _enterGameOverScreen.value = false
            _isSafehouseDraggable.value = false
            _arMode.value = ArMode.Scanner
            hideCaptureFlagButton()
            observeOtherPlayers()
            startLocationUpdates()
            addGameOverByPlayersCountListener()
            startGeofencesListenerIfGameIsReady()
        }
        ProgressState.Ended -> {
            _enterGameOverScreen.value = true
            _isSafehouseDraggable.value = false
            removeGeofencesListener()
        }
        ProgressState.Idle -> {
            _enterGameOverScreen.value = false
            _isSafehouseDraggable.value = false
            removeGeofencesListener()
        }
        ProgressState.SettingGame -> {
            _enterGameOverScreen.value = false
            _isSafehouseDraggable.value = false
        }
    }

    private suspend fun addGameOverByPlayersCountListener() {
        val cached = _game.value
        if (cached.greenPlayers.any { !it.hasLost } && cached.redPlayers.any { !it.hasLost }) return
        firestoreRepository.updateGame(cached.gameID) { latest ->
            if (latest.gameState.state != ProgressState.SettingFlags &&
                latest.gameState.state != ProgressState.Started) latest
            else when {
                latest.greenPlayers.none { !it.hasLost } -> latest.copy(
                    gameState = latest.gameState.copy(state = ProgressState.Ended, winners = Team.Red)
                )
                latest.redPlayers.none { !it.hasLost } -> latest.copy(
                    gameState = latest.gameState.copy(state = ProgressState.Ended, winners = Team.Green)
                )
                else -> latest
            }
        }
    }

    private fun observeOtherPlayers() {
        val gameID = _game.value.gameID
        if (otherPlayersGameID == gameID && otherPlayersJob?.isActive == true) return
        stopOtherPlayers()
        otherPlayersGameID = gameID
        otherPlayersJob = coroutineScope.launch {
            firestoreRepository.observePlayersPosition(gameID).onEach { players ->
                if (_game.value.gameID == gameID) {
                    _otherPlayers.value = players
                    foundOpponentToBattle(players)
                }
            }.catch {
                Timber.e("[$LOGGER_TAG] Catch observe other players error")
            }.collect()
        }
    }

    fun startLocationUpdates(): Job {
        locationUpdatesJob?.takeIf { it.isActive }?.let { return it }
        return coroutineScope.launch {
            locationRepository.locationFlow().onEach { newLocation ->
                _livePosition.value = newLocation.toLatLng()
                _canPlaceFlag.value = _livePosition.value.isPlayerInsideGame()
                foundOpponentToBattle(_otherPlayers.value)
            }.catch {
                Timber.e("[$LOGGER_TAG] Catch start location updates error")
            }.collect()
        }.also { locationUpdatesJob = it }
    }

    private fun LatLng.isPlayerInsideGame(): Boolean {
        val safehousePosition = _game.value.gameState.safehouse.position
        val isNotInsideSafehouse = !isInRangeOf(safehousePosition, _game.value.flagRadius)
        val isInsideGame = isInRangeOf(safehousePosition, _game.value.gameRadius)

        return isNotInsideSafehouse && isInsideGame
    }

    private fun Player.getInitialScreen() = if (status == Player.Status.Playing || status == Player.Status.Lost)
        Screen.Map
    else
        Screen.Menu

    private fun List<Battle>.searchOpponent() {
        if (_player.value.gameDetails?.gameID != _game.value.gameID) {
            _enterBattleScreen.value = false
            return
        }
        if (isEmpty()) _enterBattleScreen.value = false

        var isInBattle = false
        for (battle in this) {
            if (battle.players.map { it.id }.contains(_player.value.userID)) {
                _enterBattleScreen.value = true
                isInBattle = true
                break
            }
        }
        if (!isInBattle) _enterBattleScreen.value = false
    }

    private fun setCaptureFlagButtonVisibility(id: String) = when (id.isNotEmpty()) {
        true -> {
            Timber.d("[$GEOFENCE_LOGGER_TAG] Entered to: ${_enteredGeofenceId.value}")
            _enteredGeofenceId.value.onEnteredGeofenceIdChanged()
        }
        false -> {
            Timber.d("[$GEOFENCE_LOGGER_TAG] Exited from geofence")
            _showArFlagButton.value = false
        }
    }


    private fun hideCaptureFlagButton() {
        if (_game.value.gameState.greenFlagCaptured != null && _player.value.gameDetails?.team == Team.Red) {
            _showArFlagButton.value = false
        }
        if (_game.value.gameState.redFlagCaptured != null && _player.value.gameDetails?.team == Team.Green) {
            _showArFlagButton.value = false
        }
    }

    private fun foundOpponentToBattle(players: List<GamePlayer>) {
        val currentGame = _game.value
        val currentPlayer = _player.value
        val playerID = currentPlayer.userID
        val busyPlayers = currentGame.battles.flatMap { it.players }.map { it.id }.toSet()
        val ownTeam = when {
            currentGame.redPlayers.any { it.id == playerID && !it.hasLost } -> Team.Red
            currentGame.greenPlayers.any { it.id == playerID && !it.hasLost } -> Team.Green
            else -> Team.Unknown
        }
        val opponents = when (ownTeam) {
            Team.Red -> currentGame.greenPlayers
            Team.Green -> currentGame.redPlayers
            else -> emptyList()
        }.filterNot { it.hasLost }.map { it.id }.toSet()
        val canBattle = currentGame.gameState.state == ProgressState.Started &&
            currentPlayer.gameDetails?.gameID == currentGame.gameID &&
            currentPlayer.status != Player.Status.Lost &&
            playerID.isNotBlank() && playerID !in busyPlayers && ownTeam != Team.Unknown &&
            _livePosition.value.isInBattleableGameZone()
        val opponent = if (canBattle) players.firstOrNull { candidate ->
            candidate.id.isNotBlank() && candidate.id != playerID && candidate.id in opponents &&
                candidate.id !in busyPlayers && candidate.position.isInBattleableGameZone() &&
                _livePosition.value.isInRangeOf(candidate.position, DEFAULT_BATTLE_RANGE)
        } else null
        _battleID.value = opponent?.id ?: EMPTY
        _showBattleButton.value = opponent?.username ?: EMPTY
    }

    private suspend fun connectPlayer() = coroutineScope.launch {
        firestoreRepository.updatePlayer(
            _player.value.copy(
                status = Player.Status.Playing
            )
        )
    }


    private fun generateQrCode(text: String): Bitmap? {
        try {
            val width = 300
            val height = 300
            val qrCodeWriter = QRCodeWriter()
            val bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, width, height)
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
                }
            }
            _qrCodeBitmap.value = bitmap
            return bitmap
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    private suspend fun GameState.startGeofencesListenerIfGameIsReady() {
        if (safehouse.isPlaced && redFlag.isPlaced && greenFlag.isPlaced) {
            withContext(Dispatchers.Main) {
                if (!hasGeofenceListenerStarted) {
                    hasGeofenceListenerStarted = true
                    startGeofencesListener()
                }
            }
        }
    }

    private fun GameState.startGeofencesListener() {
        geofencingRepository.addGeofence(safehouse.position, GAME_BOUNDARIES_GEOFENCE_ID, _game.value.gameRadius)
        geofencingRepository.addGeofence(safehouse.position, SAFEHOUSE_GEOFENCE_ID, _game.value.flagRadius)
        geofencingRepository.addGeofence(greenFlag.position, GREEN_FLAG_GEOFENCE_ID, _game.value.flagRadius)
        geofencingRepository.addGeofence(redFlag.position, RED_FLAG_GEOFENCE_ID, _game.value.flagRadius)
        geofencingRepository.addGeofences()
    }

    private fun removeGeofencesListener() {
        hasGeofenceListenerStarted = false
        geofencingRepository.removeGeofences()
    }

    // Game extensions
    private fun String.onEnteredGeofenceIdChanged() = when {
        contains(RED_FLAG_GEOFENCE_ID) -> {
            _showArFlagButton.value =
                _game.value.gameState.redFlagCaptured == null &&
                        _player.value.gameDetails?.team == Team.Green
        }
        contains(GREEN_FLAG_GEOFENCE_ID) -> {
            _showArFlagButton.value =
                _game.value.gameState.greenFlagCaptured == null &&
                        _player.value.gameDetails?.team == Team.Red
        }
        else -> Unit
    }

    // Position extensions
    private fun LatLng.isInBattleableGameZone() =
        isInsideGame() && !isInsideSafehouse() && !isInsideRedFlag() && !isInsideGreenFlag()

    private fun LatLng.isInsideGame() = isInRangeOf(_game.value.gameState.safehouse.position, _game.value.gameRadius)

    private fun LatLng.isInsideSafehouse() = isInRangeOf(_game.value.gameState.safehouse.position, _game.value.flagRadius)

    private fun LatLng.isInsideGreenFlag() = isInRangeOf(_game.value.gameState.greenFlag.position, _game.value.flagRadius)

    private fun LatLng.isInsideRedFlag() = isInRangeOf(_game.value.gameState.redFlag.position, _game.value.flagRadius)

    companion object {

        private const val GAME_BOUNDARIES_GEOFENCE_ID = "GameBoundariesGeofence"
        private const val SAFEHOUSE_GEOFENCE_ID = "SafehouseGeofence"
        const val GREEN_FLAG_GEOFENCE_ID = "GreenFlagGeofence"
        const val RED_FLAG_GEOFENCE_ID = "RedFlagGeofence"
        private const val GAME_CODE_LENGTH = 5
    }
}


