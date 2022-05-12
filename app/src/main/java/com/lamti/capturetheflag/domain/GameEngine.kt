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
import com.lamti.capturetheflag.domain.game.Game
import com.lamti.capturetheflag.domain.game.GamePlayer
import com.lamti.capturetheflag.domain.game.GameState
import com.lamti.capturetheflag.domain.game.ProgressState
import com.lamti.capturetheflag.domain.player.GameDetails
import com.lamti.capturetheflag.domain.player.Player
import com.lamti.capturetheflag.domain.player.Team
import com.lamti.capturetheflag.presentation.ui.DEFAULT_BATTLE_RANGE
import com.lamti.capturetheflag.presentation.ui.DEFAULT_FLAG_RADIUS
import com.lamti.capturetheflag.presentation.ui.DEFAULT_SAFEHOUSE_RADIUS
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
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

    private val _showBattleButton: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val showBattleButton: StateFlow<Boolean> = _showBattleButton

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

    fun observePlayer() = firestoreRepository.observePlayer().onEach { updatedPlayer ->
        updatedPlayer.run {
            _player.value = this
            _stayInSplashScreen.value = false
            _initialScreen.value = getInitialScreen()
        }
    }.catch {
        Timber.e("[$LOGGER_TAG] Catch observe player error")
    }.launchIn(coroutineScope)

    fun observeGame() = coroutineScope.launch {
        firestoreRepository.getPlayer()?.let { _player.value = it }
        _player.value.gameDetails?.gameID?.let { id ->
            firestoreRepository.observeGame(id).onEach { game ->
                _stayInSplashScreen.value = false
                _game.value = game
                game.gameState.handleGameStateEvents()
                game.battles.searchOpponent()
            }.catch {
                Timber.e("[$LOGGER_TAG] Catch observe game error")
            }.launchIn(coroutineScope)
        }
    }

    suspend fun getGame(id: String): Game? = firestoreRepository.getGame(id)

    suspend fun getLastLocation(): Location {
        _initialPosition.value = locationRepository.awaitLastLocation().toLatLng()
        return locationRepository.awaitLastLocation()
    }

    fun setEnteredGeofenceId(id: String) {
        _enteredGeofenceId.value = id
        setCaptureFlagButtonVisibility(id)
    }

    fun logout() = firestoreRepository.logout()

    suspend fun createNewGameWithRedCaptain(title: String) = coroutineScope.launch {
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

        firestoreRepository.updatePlayer(player = _player.value, clearCache = false)
        firestoreRepository.createGame(
            id = gameID,
            title = title,
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
            ),
            clearCache = false
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
            ),
            clearCache = false
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

        observeGame()
    }

    suspend fun startGame() = coroutineScope.launch {
        firestoreRepository.updatePlayer(_player.value.copy(status = Player.Status.Playing), false)
    }

    suspend fun updateSafehouseAndForwardGameState(position: LatLng, gameRadius: Float) = coroutineScope.launch {
        firestoreRepository.updateGame(
            _game.value.copy(
                gameRadius = gameRadius,
                gameState = _game.value.gameState.copy(
                    state = ProgressState.SettingFlags,
                    safehouse = _game.value.gameState.safehouse.copy(position = position)
                )
            )
        )
    }

    suspend fun createBattle() = coroutineScope.launch {
        firestoreRepository.updateGame(
            _game.value.copy(
                battles = _game.value.battles + Battle(
                    battleID = _player.value.userID,
                    playersIDs = listOf(_player.value.userID, _battleID.value)
                )
            )
        )
    }

    suspend fun looseBattle() = coroutineScope.launch {
        val updatedBattles: MutableList<Battle> = _game.value.battles.toMutableList()
        updatedBattles.removeIf { it.playersIDs.contains(_player.value.userID) }

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
        firestoreRepository.updatePlayer(player = _player.value.copy(status = Player.Status.Lost), clearCache = false)
        firestoreRepository.deleteGamePlayer(gameID = _game.value.gameID, playerID = _player.value.userID)
    }

    suspend fun gameOver(onResult: (Boolean) -> Unit) {
        onResult(
            firestoreRepository.updatePlayer(
                player = _player.value.copy(
                    status = Player.Status.Online,
                    gameDetails = null
                ),
                clearCache = true
            )
        )
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
            _enterGameOverScreen.value = false
            _isSafehouseDraggable.value = false
            connectPlayer()
            startLocationUpdates()
            _arMode.value = ArMode.Placer
        }
        ProgressState.Started -> {
            _enterGameOverScreen.value = false
            _isSafehouseDraggable.value = false
            _arMode.value = ArMode.Scanner
            hideCaptureFlagButton()
            observeOtherPlayers()
            startGeofencesListenerIfGameIsReady()
        }
        ProgressState.Ended -> {
            _enterGameOverScreen.value = true
            _isSafehouseDraggable.value = false
            firestoreRepository.clearCache()
            removeGeofencesListener()
        }
        ProgressState.Idle -> {
            _enterGameOverScreen.value = false
            _isSafehouseDraggable.value = false
        }
        ProgressState.SettingGame -> {
            _enterGameOverScreen.value = false
            _isSafehouseDraggable.value = false
        }
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
        val isNotInsideSafehouse = !isInRangeOf(safehousePosition, DEFAULT_SAFEHOUSE_RADIUS)
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
            if (battle.playersIDs.contains(_player.value.userID)) {
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
                !_game.value.battles.flatMap { it.playersIDs }.contains(player.id) &&
                player.team != _player.value.gameDetails?.team &&
                player.position.isInBattleableGameZone() &&
                _livePosition.value.isInBattleableGameZone() &&
                _livePosition.value.isInRangeOf(player.position, DEFAULT_BATTLE_RANGE)
            ) {
                _battleID.value = player.id
                _showBattleButton.value = true
                foundOpponent = true
                break
            }
        }
        if (!foundOpponent) {
            _battleID.value = EMPTY
            _showBattleButton.value = false
        }
    }

    private suspend fun connectPlayer() = coroutineScope.launch {
        firestoreRepository.updatePlayer(
            _player.value.copy(
                status = Player.Status.Playing
            ),
            false
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
        geofencingRepository.addGeofence(safehouse.position, SAFEHOUSE_GEOFENCE_ID, DEFAULT_SAFEHOUSE_RADIUS)
        geofencingRepository.addGeofence(greenFlag.position, GREEN_FLAG_GEOFENCE_ID, DEFAULT_FLAG_RADIUS)
        geofencingRepository.addGeofence(redFlag.position, RED_FLAG_GEOFENCE_ID, DEFAULT_FLAG_RADIUS)
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

    private fun LatLng.isInsideSafehouse() = isInRangeOf(_game.value.gameState.safehouse.position, DEFAULT_SAFEHOUSE_RADIUS)

    private fun LatLng.isInsideGreenFlag() = isInRangeOf(_game.value.gameState.greenFlag.position, DEFAULT_FLAG_RADIUS)

    private fun LatLng.isInsideRedFlag() = isInRangeOf(_game.value.gameState.redFlag.position, DEFAULT_FLAG_RADIUS)

    companion object {

        private const val GAME_BOUNDARIES_GEOFENCE_ID = "GameBoundariesGeofence"
        private const val SAFEHOUSE_GEOFENCE_ID = "SafehouseGeofence"
        const val GREEN_FLAG_GEOFENCE_ID = "GreenFlagGeofence"
        const val RED_FLAG_GEOFENCE_ID = "RedFlagGeofence"
        private const val GAME_CODE_LENGTH = 5
    }
}


