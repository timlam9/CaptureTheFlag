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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
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

    fun observePlayer(): Job = firestoreRepository.observePlayer().onEach { updatedPlayer ->
        updatedPlayer.run {
            _player.value = this
            _stayInSplashScreen.value = false
            _initialScreen.value = getInitialScreen()
        }
    }.catch {
        Timber.e("[$LOGGER_TAG] Catch observe player error")
    }.launchIn(coroutineScope)

    fun observeGame(): Job = coroutineScope.launch {
        firestoreRepository.getPlayer()?.let { _player.value = it }
        _player.value.gameDetails?.gameID?.let { id ->
            firestoreRepository.observeGame(id).onEach { game ->
                _stayInSplashScreen.value = false
                _game.value = game
                game.gameState.handleGameStateEvents()
                game.battles.searchOpponent()
                updateBattle()
            }.catch {
                Timber.e("[$LOGGER_TAG] Catch observe game error")
            }.launchIn(coroutineScope)
        }
    }

    private fun updateBattle() {
        val playerBattle = findPlayerBattle() ?: return
        _battleState.update { playerBattle.state }
        _battleWinner.update { playerBattle.winner }
        _isPlayerReadyToBattle.update {
            playerBattle.players.firstOrNull { it.id == _player.value.userID }?.ready ?: false
        }
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

    suspend fun addPlayerToTeam() = coroutineScope.launch {
        val team = _player.value.gameDetails?.team ?: Team.Unknown
        val gameDetails = _player.value.gameDetails
        val gameID = gameDetails?.gameID ?: return@launch
        val game = getGame(gameID) ?: return@launch
        _game.value = game

        val rank = when (team) {
            Team.Green -> {
                when (_game.value.greenPlayers.isEmpty()) {
                    true -> GameDetails.Rank.Leader
                    false -> GameDetails.Rank.Soldier
                }
            }
            else -> GameDetails.Rank.Soldier
        }

        firestoreRepository.updatePlayer(
            player = _player.value.copy(
                gameDetails = gameDetails.copy(
                    gameID = gameID,
                    team = team,
                    rank = rank
                )
            )
        )

        val updatedGame = when (team) {
            Team.Red -> {
                val newList = _game.value.redPlayers.toMutableList()
                newList.add(ActivePlayer(id = _player.value.userID, hasLost = false))
                _game.value.copy(redPlayers = newList)
            }
            Team.Green -> {
                val newList = _game.value.greenPlayers.toMutableList()
                newList.add(ActivePlayer(id = _player.value.userID, hasLost = false))
                _game.value.copy(greenPlayers = newList)
            }
            else -> _game.value
        }
        firestoreRepository.updateGame(updatedGame)
    }

    suspend fun startGame() = coroutineScope.launch {
        firestoreRepository.updatePlayer(_player.value.copy(status = Player.Status.Playing))
    }

    suspend fun updateSafehouseAndForwardGameState(position: LatLng, gameRadius: Float, flagRadius: Float) =
        coroutineScope.launch {
            firestoreRepository.updateGame(
                _game.value.copy(
                    gameRadius = gameRadius,
                    flagRadius = flagRadius,
                    gameState = _game.value.gameState.copy(
                        state = ProgressState.SettingFlags,
                        safehouse = _game.value.gameState.safehouse.copy(position = position)
                    )
                )
            )
        }

    suspend fun createBattle() = coroutineScope.launch {
        firestoreRepository.updateBattles(
            _game.value.gameID,
            Battle(
                battleID = _player.value.userID,
                state = BattleState.StandBy,
                winner = EMPTY,
                players = listOf(BattlingPlayer(_player.value.userID, false), BattlingPlayer(_battleID.value, false))
            )
        )
    }

    suspend fun readyToBattle() = coroutineScope.launch {
        firestoreRepository.updateReadyToBattle(_game.value.gameID, _player.value.userID)
    }

    private fun findPlayerBattle(): Battle? = _game.value.battles.firstOrNull { battle ->
        battle.players.map { it.id }.contains(_player.value.userID)
    }

    suspend fun onBattleWinnerFound() = coroutineScope.launch {
        val updatedBattles = _game.value.battles.map { battle ->
            if (battle.players.map { it.id }.contains(_player.value.userID))
                battle.copy(
                    winner = if (battle.winner == EMPTY) _player.value.details.username else battle.winner,
                    state = BattleState.Over
                )
            else battle
        }

        firestoreRepository.updateGame(
            _game.value.copy(
                battles = updatedBattles
            )
        )
    }

    suspend fun looseBattle() = if (_game.value.battleMiniGame == BattleMiniGame.None) withoutMiniGame() else withMiniGame()

    private suspend fun withMiniGame() = coroutineScope.launch {
        var updatedBattles: MutableList<Battle> = _game.value.battles.toMutableList()

        updatedBattles = updatedBattles.map { battle ->
            val updatedPlayers = battle.players.toMutableList()
            updatedPlayers.removeIf { player -> player.id == _player.value.userID }

            val updatedBattle = battle.copy(players = updatedPlayers)
            updatedBattle
        }.toMutableList()

        updatedBattles.removeIf { battle -> battle.players.isEmpty() }

        val updatedGameState = when (_player.value.userID) {
            _game.value.gameState.redFlagCaptured -> _game.value.gameState.copy(redFlagCaptured = null)
            _game.value.gameState.greenFlagCaptured -> _game.value.gameState.copy(greenFlagCaptured = null)
            else -> _game.value.gameState
        }

        val (redPlayers, greenPlayers) = when (_player.value.gameDetails?.team) {
            Team.Red -> {
                val redPlayers = _game.value.redPlayers.map {
                    if (it.id == _player.value.userID && _battleWinner.value != _player.value.details.username) it.copy(
                        hasLost = true
                    ) else it
                }
                Pair(redPlayers, _game.value.greenPlayers)
            }
            Team.Green -> {
                val greenPlayers = _game.value.greenPlayers.map {
                    if (it.id == _player.value.userID && _battleWinner.value != _player.value.details.username) it.copy(
                        hasLost = true
                    ) else it
                }
                Pair(_game.value.redPlayers, greenPlayers)
            }
            else -> Pair(_game.value.redPlayers, _game.value.greenPlayers)
        }

        firestoreRepository.updateGame(
            game = _game.value.copy(
                battles = updatedBattles,
                gameState = updatedGameState,
                redPlayers = redPlayers,
                greenPlayers = greenPlayers
            )
        )
        if (_battleWinner.value != _player.value.details.username)
            firestoreRepository.updatePlayer(player = _player.value.copy(status = Player.Status.Lost))
        firestoreRepository.deleteGamePlayer(gameID = _game.value.gameID, playerID = _player.value.userID)
    }

    private suspend fun withoutMiniGame() = coroutineScope.launch {
        val updatedBattles: MutableList<Battle> = _game.value.battles.toMutableList()
        updatedBattles.removeIf { battle -> battle.players.map { it.id }.contains(_player.value.userID) }

        val updatedGameState = when (_player.value.userID) {
            _game.value.gameState.redFlagCaptured -> _game.value.gameState.copy(redFlagCaptured = null)
            _game.value.gameState.greenFlagCaptured -> _game.value.gameState.copy(greenFlagCaptured = null)
            else -> _game.value.gameState
        }

        val (redPlayers, greenPlayers) = when (_player.value.gameDetails?.team) {
            Team.Red -> {
                val redPlayers = _game.value.redPlayers.map {
                    if (it.id == _player.value.userID) it.copy(hasLost = true) else it
                }
                Pair(redPlayers, _game.value.greenPlayers)
            }
            Team.Green -> {
                val greenPlayers = _game.value.greenPlayers.map {
                    if (it.id == _player.value.userID) it.copy(hasLost = true) else it
                }
                Pair(_game.value.redPlayers, greenPlayers)
            }
            else -> Pair(_game.value.redPlayers, _game.value.greenPlayers)
        }

        firestoreRepository.updateGame(
            game = _game.value.copy(
                battles = updatedBattles,
                gameState = updatedGameState,
                redPlayers = redPlayers,
                greenPlayers = greenPlayers
            )
        )

        firestoreRepository.updatePlayer(player = _player.value.copy(status = Player.Status.Lost))
        firestoreRepository.deleteGamePlayer(gameID = _game.value.gameID, playerID = _player.value.userID)
    }

    suspend fun removePlayer(onResult: (Boolean) -> Unit) {
        val updateGame = when (_player.value.gameDetails?.team) {
            Team.Red -> {
                val redPlayers = _game.value.redPlayers.toMutableList()
                redPlayers.removeIf {
                    it.id == _player.value.userID
                }
                firestoreRepository.updateGame(
                    game = _game.value.copy(
                        redPlayers = redPlayers
                    )
                )
            }
            Team.Green -> {
                val greenPlayers = _game.value.greenPlayers.toMutableList()
                greenPlayers.removeIf {
                    it.id == _player.value.userID
                }
                firestoreRepository.updateGame(
                    game = _game.value.copy(
                        greenPlayers = greenPlayers
                    )
                )
            }
            else -> true
        }

        val updatePlayer = firestoreRepository.updatePlayer(
            player = _player.value.copy(
                status = Player.Status.Online,
                gameDetails = null
            )
        )

        val deleteGamePlayer = firestoreRepository.deleteGamePlayer(_game.value.gameID, _player.value.userID)

        observeGame().cancel()
        checkForGameDeletion()

        onResult(updatePlayer && updateGame && deleteGamePlayer)
    }

    private suspend fun checkForGameDeletion() {
        if (_game.value.redPlayers.isEmpty() && _game.value.greenPlayers.isEmpty()) {
            firestoreRepository.deleteFirebaseGame(_game.value.gameID)
            firestoreRepository.deleteGame(_game.value.gameID)
        }
    }

    suspend fun captureFlag(onResult: (Boolean) -> Unit) = coroutineScope.launch {
        val game = when (_player.value.gameDetails?.team) {
            Team.Red -> _game.value.copy(gameState = _game.value.gameState.copy(greenFlagCaptured = _player.value.userID))
            Team.Green -> _game.value.copy(gameState = _game.value.gameState.copy(redFlagCaptured = _player.value.userID))
            else -> _game.value.copy(gameState = _game.value.gameState.copy())
        }
        onResult(firestoreRepository.updateGame(game))
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
        val updatedGame = when {
            _game.value.greenPlayers.filterNot { it.hasLost }.isEmpty() -> _game.value.copy(
                gameState = _game.value.gameState.copy(
                    state = ProgressState.Ended,
                    winners = Team.Red
                )
            )
            _game.value.redPlayers.filterNot { it.hasLost }.isEmpty() -> _game.value.copy(
                gameState = _game.value.gameState.copy(
                    state = ProgressState.Ended,
                    winners = Team.Green
                )
            )
            else -> _game.value
        }

        firestoreRepository.updateGame(updatedGame)
    }

    private fun observeOtherPlayers() = coroutineScope.launch {
        firestoreRepository.observePlayersPosition(_game.value.gameID).onEach { players ->
            _otherPlayers.value = players
            foundOpponentToBattle(players)
        }.catch {
            Timber.e("[$LOGGER_TAG] Catch observe other players error")
        }.launchIn(coroutineScope)
    }

    fun startLocationUpdates() = coroutineScope.launch {
        locationRepository.locationFlow().onEach { newLocation ->
            newLocation.toLatLng().run {
                _livePosition.value = this
                _canPlaceFlag.value = isPlayerInsideGame()
            }
        }.catch {
            Timber.e("[$LOGGER_TAG] Catch start location updates error")
        }.launchIn(coroutineScope)
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
        var foundOpponent = false
        for (player in players) {
            if (player.id != _player.value.userID &&
                !_game.value.battles.flatMap { it.players }.map { it.id }.contains(player.id) &&
                player.team != _player.value.gameDetails?.team &&
                player.position.isInBattleableGameZone() &&
                _livePosition.value.isInBattleableGameZone() &&
                _livePosition.value.isInRangeOf(player.position, DEFAULT_BATTLE_RANGE)
            ) {
                _battleID.value = player.id
                _showBattleButton.value = player.username
                foundOpponent = true
                break
            }
        }
        if (!foundOpponent) {
            _battleID.value = EMPTY
            _showBattleButton.value = EMPTY
        }
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


