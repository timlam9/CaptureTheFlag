package com.lamti.capturetheflag.presentation.ui.fragments.maps

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.lamti.capturetheflag.data.location.LocationRepository
import com.lamti.capturetheflag.data.location.geofences.GeofencingRepository
import com.lamti.capturetheflag.domain.FirestoreRepository
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
import com.lamti.capturetheflag.presentation.ui.DEFAULT_GAME_BOUNDARIES_RADIUS
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
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class MapViewModel @Inject constructor(
    private val geofencingHelper: GeofencingRepository,
    private val firestoreRepository: FirestoreRepository,
    private val locationRepository: LocationRepository
) : ViewModel() {

    private val _enteredGeofenceId = mutableStateOf(EMPTY)
    private val _battleID = mutableStateOf(EMPTY)

    private val _stayInSplashScreen = mutableStateOf(true)
    val stayInSplashScreen: State<Boolean> = _stayInSplashScreen

    private val _initialScreen: MutableState<Screen> = mutableStateOf(Screen.Menu)
    val initialScreen: State<Screen> = _initialScreen

    private val _livePosition: MutableState<LatLng> = mutableStateOf(emptyPosition())
    val livePosition: State<LatLng> = _livePosition

    private val _initialPosition: MutableState<LatLng> = mutableStateOf(emptyPosition())
    val initialPosition: State<LatLng> = _initialPosition

    private val _player = mutableStateOf(Player.emptyPlayer())
    val player: State<Player> = _player

    private val _game: MutableState<Game> = mutableStateOf(Game.initialGame(_livePosition.value))
    val game: State<Game> = _game

    private val _showBattleButton = mutableStateOf(false)
    val showBattleButton: State<Boolean> = _showBattleButton

    private val _enterBattleScreen = mutableStateOf(false)
    val enterBattleScreen: State<Boolean> = _enterBattleScreen

    private val _showArFlagButton = mutableStateOf(false)
    val showArFlagButton: State<Boolean> = _showArFlagButton

    private val _enterGameOverScreen = mutableStateOf(false)
    val enterGameOverScreen: State<Boolean> = _enterGameOverScreen

    private val _canPlaceFlag: MutableState<Boolean> = mutableStateOf(false)
    val canPlaceFlag: State<Boolean> = _canPlaceFlag

    private val _isSafehouseDraggable = mutableStateOf(false)
    val isSafehouseDraggable: State<Boolean> = _isSafehouseDraggable

    private val _qrCodeBitmap: MutableState<Bitmap?> = mutableStateOf(null)
    val qrCodeBitmap: State<Bitmap?> = _qrCodeBitmap

    private val _otherPlayers: MutableState<List<GamePlayer>> = mutableStateOf(emptyList())
    val otherPlayers: State<List<GamePlayer>> = _otherPlayers

    private val _arMode = MutableStateFlow(ArMode.Placer)
    val arMode: StateFlow<ArMode> = _arMode.asStateFlow()

    init {
        startLocationUpdates()
    }

    suspend fun getGame(id: String) = withContext(Dispatchers.IO) {
        firestoreRepository.getGame(id)
    }

    fun getLastLocation() {
        viewModelScope.launch {
            _initialPosition.value = locationRepository.awaitLastLocation().toLatLng()
        }
    }

    fun setEnteredGeofenceId(id: String) {
        _enteredGeofenceId.value = id
        _enteredGeofenceId.value.onEnteredGeofenceIdChanged()
        Timber.d("[$GEOFENCE_LOGGER_TAG] Entered to: ${_enteredGeofenceId.value}")
    }

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
        contains(SAFEHOUSE_GEOFENCE_ID) -> _showArFlagButton.value = false
        else -> _showArFlagButton.value = false
    }

    private fun observeOtherPlayers() {
        firestoreRepository.observePlayersPosition(_game.value.gameID).onEach { players ->
            _otherPlayers.value = players
            foundOpponentToBattle(players)
        }.catch {
            Timber.e("[$LOGGER_TAG] Catch observe other players error")
        }.launchIn(viewModelScope)
    }

    private fun foundOpponentToBattle(players: List<GamePlayer>) {
        var foundOpponent = false
        for (player in players) {
            if (player.id != _player.value.userID &&
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

    private fun LatLng.isInBattleableGameZone() = !isInsideSafehouse() && !isInsideRedFlag() && !isInsideGreenFlag()

    private fun LatLng.isInsideSafehouse() = isInRangeOf(_game.value.gameState.safehouse.position, DEFAULT_SAFEHOUSE_RADIUS)

    private fun LatLng.isInsideGreenFlag() = isInRangeOf(_game.value.gameState.greenFlag.position, DEFAULT_FLAG_RADIUS)

    private fun LatLng.isInsideRedFlag() = isInRangeOf(_game.value.gameState.redFlag.position, DEFAULT_FLAG_RADIUS)

    private fun enterBattle(battles: List<Battle>) {
        if (battles.isEmpty()) _enterBattleScreen.value = false

        var isInBattle = false
        for (battle in battles) {
            if (battle.playersIDs.contains(_player.value.userID)) {
                _enterBattleScreen.value = true
                isInBattle = true
                break
            }
        }
        if (!isInBattle) _enterBattleScreen.value = false
    }

    fun observePlayer() {
        firestoreRepository.observePlayer().onEach { updatedPlayer ->
            _initialScreen.value = if (updatedPlayer.status == Player.Status.Playing ||
                updatedPlayer.status == Player.Status.Lost
            ) Screen.Map else Screen.Menu
            _player.value = updatedPlayer
            _stayInSplashScreen.value = false
        }.catch {
            Timber.e("[$LOGGER_TAG] Catch observe player error")
        }.launchIn(viewModelScope)
    }

    fun observeGame() {
        viewModelScope.launch {
            firestoreRepository.observeGame().onEach { game ->
                _game.value = game
                game.gameState.handleGameStateEvents()
                enterBattle(game.battles)
            }.launchIn(viewModelScope)
        }
    }

    fun updateSafeHousePosition(position: LatLng) {
        val gameID = _player.value.gameDetails?.gameID ?: return
        viewModelScope.launch {
            firestoreRepository.updateSafehousePosition(gameID, position)
        }
    }

    fun onCreateGameClicked(title: String) {
        viewModelScope.launch {
            val gameID = getRandomString(5)
            generateQrCode(gameID)
            _player.value = _player.value.copy(gameDetails = _player.value.gameDetails?.copy(gameID = gameID))
            firestoreRepository.createGame(gameID, title, _livePosition.value)
            observeGame()
        }
    }

    fun onSetGameClicked() {
        viewModelScope.launch {
            firestoreRepository.updatePlayerStatus(Player.Status.Playing)
        }
    }

    fun onSetFlagsClicked() {
        val gameID = _player.value.gameDetails?.gameID ?: return
        viewModelScope.launch {
            firestoreRepository.updateGameStatus(gameID, ProgressState.SettingFlags)
        }
    }

    fun onJoinButtonClicked(gameID: String) {
        viewModelScope.launch {
            firestoreRepository.joinPlayer(gameID)
        }
    }

    fun onTeamButtonClicked(team: Team) {
        _player.value = _player.value.copy(gameDetails = _player.value.gameDetails?.copy(team = team))
    }

    fun onTeamOkButtonClicked() {
        viewModelScope.launch {
            firestoreRepository.setPlayerTeam(_player.value.gameDetails?.team ?: Team.Unknown)
            observeGame()
        }
    }

    private fun onConnect(onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = firestoreRepository.connectPlayer()
            onResult(result)
        }
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

    private fun GameState.startGeofencesListener() {
        geofencingHelper.addGeofence(safehouse.position, GAME_BOUNDARIES_GEOFENCE_ID, DEFAULT_GAME_BOUNDARIES_RADIUS)
        geofencingHelper.addGeofence(safehouse.position, SAFEHOUSE_GEOFENCE_ID, DEFAULT_SAFEHOUSE_RADIUS)
        geofencingHelper.addGeofence(greenFlag.position, GREEN_FLAG_GEOFENCE_ID, DEFAULT_FLAG_RADIUS)
        geofencingHelper.addGeofence(redFlag.position, RED_FLAG_GEOFENCE_ID, DEFAULT_FLAG_RADIUS)
        geofencingHelper.addGeofences()
    }

    private fun removeGeofencesListener() {
        geofencingHelper.removeGeofences()
    }

    private fun GameState.handleGameStateEvents() = when (state) {
        ProgressState.Created -> {
            _enterGameOverScreen.value = false
            _isSafehouseDraggable.value = _player.value.gameDetails?.rank == GameDetails.Rank.Captain
        }
        ProgressState.SettingFlags -> {
            _enterGameOverScreen.value = false
            _isSafehouseDraggable.value = false
            _arMode.value = ArMode.Placer
            onConnect { }
        }
        ProgressState.Started -> {
            _enterGameOverScreen.value = false
            _isSafehouseDraggable.value = false
            _arMode.value = ArMode.Scanner
            onGameStarted()
        }
        ProgressState.Ended -> {
            _enterGameOverScreen.value = true
            _isSafehouseDraggable.value = false
            removeGeofencesListener()
        }
        else -> {
            _enterGameOverScreen.value = false
            _isSafehouseDraggable.value = false
        }
    }

    private fun GameState.onGameStarted() {
        if (safehouse.isPlaced && redFlag.isPlaced && greenFlag.isPlaced) {
            startGeofencesListener()
            observeOtherPlayers()
        }
    }

    fun onBattleButtonClicked() {
        viewModelScope.launch {
            firestoreRepository.createBattle(_battleID.value)
        }
    }

    private fun startLocationUpdates() {
        locationRepository.locationFlow().onEach { newLocation ->
            val safehousePosition = _game.value.gameState.safehouse.position
            val isNotInsideSafehouse = !newLocation.toLatLng().isInRangeOf(safehousePosition, DEFAULT_SAFEHOUSE_RADIUS)
            val isInsideGame = newLocation.toLatLng().isInRangeOf(safehousePosition, DEFAULT_GAME_BOUNDARIES_RADIUS)

            _canPlaceFlag.value = isNotInsideSafehouse && isInsideGame
            _livePosition.value = newLocation.toLatLng()
        }.launchIn(viewModelScope)
    }

    fun onLostBattleButtonClicked() {
        viewModelScope.launch {
            firestoreRepository.lost()
        }
    }

    fun onGameOverOkClicked(onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = firestoreRepository.quitGame()
            onResult(result)
        }
    }

    fun logout() {
        firestoreRepository.logout()
    }

    companion object {

        private const val GAME_BOUNDARIES_GEOFENCE_ID = "GameBoundariesGeofence"
        private const val SAFEHOUSE_GEOFENCE_ID = "SafehouseGeofence"
        private const val GREEN_FLAG_GEOFENCE_ID = "GreenFlagGeofence"
        private const val RED_FLAG_GEOFENCE_ID = "RedFlagGeofence"
    }
}
