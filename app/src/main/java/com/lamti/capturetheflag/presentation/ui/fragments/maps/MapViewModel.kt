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

    private val _initialScreen: MutableState<Screen> = mutableStateOf(Screen.Menu)
    val initialScreen: State<Screen> = _initialScreen

    private val _stayInSplashScreen = mutableStateOf(true)
    val stayInSplashScreen: State<Boolean> = _stayInSplashScreen

    private val _livePosition: MutableState<LatLng> = mutableStateOf(emptyPosition())
    val livePosition: State<LatLng> = _livePosition

    private val _initialPosition: MutableState<LatLng> = mutableStateOf(emptyPosition())
    val initialPosition: State<LatLng> = _initialPosition

    private val _player = mutableStateOf(Player.emptyPlayer())
    val player: State<Player> = _player

    private val _game: MutableState<Game> = mutableStateOf(Game.initialGame(_livePosition.value))
    val game: State<Game> = _game

    private val _qrCodeBitmap: MutableState<Bitmap?> = mutableStateOf(null)
    val qrCodeBitmap: State<Bitmap?> = _qrCodeBitmap

    private val _enteredGeofenceId = mutableStateOf(EMPTY)
    private val _battleID = mutableStateOf(EMPTY)

    private val _showBattleButton = mutableStateOf(false)
    val showBattleButton: State<Boolean> = _showBattleButton

    private val _enterBattleScreen = mutableStateOf(false)
    val enterBattleScreen: State<Boolean> = _enterBattleScreen

    private val _showArFlagButton = mutableStateOf(false)
    val showArFlagButton: State<Boolean> = _showArFlagButton

    private val _canPlaceFlag: MutableState<Boolean> = mutableStateOf(false)
    val canPlaceFlag: State<Boolean> = _canPlaceFlag

    private val _isSafehouseDraggable = mutableStateOf(false)
    val isSafehouseDraggable: State<Boolean> = _isSafehouseDraggable

    private val _enterGameOverScreen = mutableStateOf(false)
    val enterGameOverScreen: State<Boolean> = _enterGameOverScreen

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
        val gameID: String = if (_player.value.gameDetails != null) _player.value.gameDetails!!.gameID else return
        firestoreRepository.observeGame(gameID).onEach { game ->
            _game.value = game
            game.gameState.handleGameStateEvents()
            enterBattle(game.battles)
        }.launchIn(viewModelScope)
    }

    fun onCreateGameClicked(title: String) {
        viewModelScope.launch {
            val gameID = getRandomString(5)

            generateQrCode(gameID)
            _player.value = _player.value.copy(gameDetails = _player.value.gameDetails?.copy(gameID = gameID))

            firestoreRepository.createGame(
                id = gameID,
                title = title,
                position = _livePosition.value,
                player = _player.value
            )
            firestoreRepository.updatePlayer(
                player = _player.value
                    .copy(
                        gameDetails = GameDetails(
                            gameID = gameID,
                            team = Team.Red,
                            rank = GameDetails.Rank.Captain
                        ),
                        status = Player.Status.Connecting
                    )
            )
            observeGame()
        }
    }

    fun onReadyButtonClicked(position: LatLng) {
        viewModelScope.launch {
            // update safehouse position
            firestoreRepository.updateGame(
                _game.value.copy(
                    gameState = _game.value.gameState.copy(
                        state = ProgressState.SettingFlags,
                        safehouse = _game.value.gameState.safehouse.copy(position = position)
                    )
                )
            )
        }
    }

    fun onSetGameClicked() {
        viewModelScope.launch {
            firestoreRepository.updatePlayer(_player.value.copy(status = Player.Status.Playing))
        }
    }

    fun onGameCodeScanned(gameID: String) {
        viewModelScope.launch {
            // join player
            firestoreRepository.updatePlayer(
                _player.value.copy(
                    gameDetails = GameDetails(
                        gameID = gameID,
                        team = Team.Unknown,
                        rank = GameDetails.Rank.Soldier
                    ),
                    status = Player.Status.Connecting
                )
            )
        }
    }

    fun onTeamButtonClicked(team: Team) {
        _player.value = _player.value.copy(gameDetails = _player.value.gameDetails?.copy(team = team))
    }

    fun onTeamOkButtonClicked() {
        viewModelScope.launch {
            // set player's team
            val team = _player.value.gameDetails?.team ?: Team.Unknown
            val gameDetails = _player.value.gameDetails

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
                    gameDetails = gameDetails?.copy(
                        gameID = _game.value.gameID,
                        team = team,
                        rank = rank
                    )
                )
            )

            val updatedGame = when (_player.value.gameDetails?.team) {
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
    }

    fun onBattleButtonClicked() {
        viewModelScope.launch {
            // create battle
            firestoreRepository.updateGame(
                _game.value.copy(
                    battles = _game.value.battles + Battle(
                        battleID = _player.value.userID,
                        playersIDs = listOf(_player.value.userID, _battleID.value)
                    )
                )
            )
        }
    }

    fun onLostBattleButtonClicked() {
        viewModelScope.launch {
            // lost
            val updatedBattles: MutableList<Battle> = _game.value.battles.toMutableList()
            updatedBattles.removeIf { it.playersIDs.contains(_player.value.userID) }

            val updatedGameState = when (_player.value.userID) {
                _game.value.gameState.redFlagCaptured -> _game.value.gameState.copy(redFlagCaptured = null)
                _game.value.gameState.greenFlagCaptured -> _game.value.gameState.copy(greenFlagCaptured = null)
                else -> _game.value.gameState
            }

            val (redPlayers, greenPlayers) = when (_player.value.gameDetails?.team) {
                Team.Red -> {
                    val redPlayers =
                        _game.value.redPlayers.map { if (it.id == _player.value.userID) it.copy(hasLost = true) else it }
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
    }

    fun onGameOverOkClicked(onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            onResult(
                firestoreRepository.updatePlayer(
                    _player.value.copy(
                        status = Player.Status.Online,
                        gameDetails = null
                    )
                )
            )
        }
    }

    fun setEnteredGeofenceId(id: String) {
        _enteredGeofenceId.value = id
        if (id.isNotEmpty()) {
            _enteredGeofenceId.value.onEnteredGeofenceIdChanged()
            Timber.d("[$GEOFENCE_LOGGER_TAG] Entered to: ${_enteredGeofenceId.value}")
        } else {
            _showArFlagButton.value = false
            Timber.d("[$GEOFENCE_LOGGER_TAG] Exited from geofence")
        }
    }

    fun getLastLocation() {
        viewModelScope.launch {
            _initialPosition.value = locationRepository.awaitLastLocation().toLatLng()
        }
    }

    fun logout() {
        firestoreRepository.logout()
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

    private fun onConnect(onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            // connect player
            onResult(
                firestoreRepository.updatePlayer(
                    _player.value.copy(
                        status = Player.Status.Playing
                    )
                )
            )
        }
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

    private fun startLocationUpdates() {
        locationRepository.locationFlow().onEach { newLocation ->
            val safehousePosition = _game.value.gameState.safehouse.position
            val isNotInsideSafehouse = !newLocation.toLatLng().isInRangeOf(safehousePosition, DEFAULT_SAFEHOUSE_RADIUS)
            val isInsideGame = newLocation.toLatLng().isInRangeOf(safehousePosition, DEFAULT_GAME_BOUNDARIES_RADIUS)

            _canPlaceFlag.value = isNotInsideSafehouse && isInsideGame
            _livePosition.value = newLocation.toLatLng()
        }.launchIn(viewModelScope)
    }

    private fun removeGeofencesListener() {
        geofencingHelper.removeGeofences()
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

    private fun GameState.onGameStarted() {
        if (safehouse.isPlaced && redFlag.isPlaced && greenFlag.isPlaced) {
            startGeofencesListener()
            observeOtherPlayers()
        }
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
            if (_game.value.gameState.greenFlagCaptured != null && _player.value.gameDetails?.team == Team.Red) {
                _showArFlagButton.value = false
            }
            if (_game.value.gameState.redFlagCaptured != null && _player.value.gameDetails?.team == Team.Green) {
                _showArFlagButton.value = false
            }
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

    private fun GameState.startGeofencesListener() {
        geofencingHelper.addGeofence(safehouse.position, GAME_BOUNDARIES_GEOFENCE_ID, DEFAULT_GAME_BOUNDARIES_RADIUS)
        geofencingHelper.addGeofence(safehouse.position, SAFEHOUSE_GEOFENCE_ID, DEFAULT_SAFEHOUSE_RADIUS)
        geofencingHelper.addGeofence(greenFlag.position, GREEN_FLAG_GEOFENCE_ID, DEFAULT_FLAG_RADIUS)
        geofencingHelper.addGeofence(redFlag.position, RED_FLAG_GEOFENCE_ID, DEFAULT_FLAG_RADIUS)
        geofencingHelper.addGeofences()
    }

    // Position extensions
    private fun LatLng.isInBattleableGameZone() =
        isInsideGame() && !isInsideSafehouse() && !isInsideRedFlag() && !isInsideGreenFlag()

    private fun LatLng.isInsideGame() = isInRangeOf(_game.value.gameState.safehouse.position, DEFAULT_GAME_BOUNDARIES_RADIUS)

    private fun LatLng.isInsideSafehouse() = isInRangeOf(_game.value.gameState.safehouse.position, DEFAULT_SAFEHOUSE_RADIUS)

    private fun LatLng.isInsideGreenFlag() = isInRangeOf(_game.value.gameState.greenFlag.position, DEFAULT_FLAG_RADIUS)

    private fun LatLng.isInsideRedFlag() = isInRangeOf(_game.value.gameState.redFlag.position, DEFAULT_FLAG_RADIUS)


    companion object {

        private const val GAME_BOUNDARIES_GEOFENCE_ID = "GameBoundariesGeofence"
        private const val SAFEHOUSE_GEOFENCE_ID = "SafehouseGeofence"
        private const val GREEN_FLAG_GEOFENCE_ID = "GreenFlagGeofence"
        private const val RED_FLAG_GEOFENCE_ID = "RedFlagGeofence"
    }
}
