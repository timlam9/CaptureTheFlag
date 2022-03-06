package com.lamti.capturetheflag.presentation.ui.fragments.maps

import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
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
import com.lamti.capturetheflag.domain.game.GameState
import com.lamti.capturetheflag.domain.game.ProgressState
import com.lamti.capturetheflag.domain.player.Player
import com.lamti.capturetheflag.presentation.ui.DEFAULT_FLAG_RADIUS
import com.lamti.capturetheflag.presentation.ui.DEFAULT_GAME_BOUNDARIES_RADIUS
import com.lamti.capturetheflag.presentation.ui.DEFAULT_SAFEHOUSE_RADIUS
import com.lamti.capturetheflag.presentation.ui.components.Screen
import com.lamti.capturetheflag.presentation.ui.fragments.ar.ArMode
import com.lamti.capturetheflag.presentation.ui.getRandomString
import com.lamti.capturetheflag.presentation.ui.toLatLng
import com.lamti.capturetheflag.utils.emptyPosition
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@ExperimentalCoroutinesApi
@HiltViewModel
class MapViewModel @Inject constructor(
    private val geofencingHelper: GeofencingRepository,
    private val firestoreRepository: FirestoreRepository,
    private val locationRepository: LocationRepository
) : ViewModel() {

    private val _stayInSplashScreen = mutableStateOf(true)
    val stayInSplashScreen: State<Boolean> = _stayInSplashScreen

    private val _initialScreen: MutableState<Screen> = mutableStateOf(Screen.Menu)
    val initialScreen: State<Screen> = _initialScreen

    private val _currentPosition: MutableState<LatLng> = mutableStateOf(emptyPosition())
    val currentPosition: State<LatLng> = _currentPosition

    private val _gameState: MutableState<GameState> = mutableStateOf(GameState.initialGameState(_currentPosition.value))
    val gameState: State<GameState> = _gameState

    private val _player = mutableStateOf(Player.emptyPlayer())
    val player: State<Player> = _player

    private val _isSafehouseDraggable = mutableStateOf(false)
    val isSafehouseDraggable: State<Boolean> = _isSafehouseDraggable

    private val _arMode = MutableStateFlow(ArMode.Placer)
    val arMode: StateFlow<ArMode> = _arMode.asStateFlow()

    fun getLastLocation() {
        viewModelScope.launch {
            _currentPosition.value = locationRepository.awaitLastLocation().toLatLng()
        }
    }

    fun observePlayer() {
        firestoreRepository.observePlayer().onEach { updatedPlayer ->
            _initialScreen.value = if (updatedPlayer.status == Player.Status.Playing) Screen.Map else Screen.Menu
            _player.value = updatedPlayer
            _stayInSplashScreen.value = false
        }.catch {
            Log.d("TAGARA", "Catch error")
        }.launchIn(viewModelScope)
    }

    fun onCreateGameClicked(title: String) {
        viewModelScope.launch {
            val gameID = getRandomString(5)
            firestoreRepository.createGame(gameID, title, _currentPosition.value)
            observeGameState(gameID)
        }
    }

    fun observeGameState(gameID: String) {
        viewModelScope.launch {
            firestoreRepository.observeGameState(gameID).onEach { gameState ->
                _gameState.value = gameState
                gameState.handleGameStateEvents()
            }.launchIn(viewModelScope)
        }
    }

    private fun GameState.handleGameStateEvents() = when (state) {
        ProgressState.Created -> _isSafehouseDraggable.value = true
        ProgressState.SettingFlags -> {
            _isSafehouseDraggable.value = false
            _arMode.value = ArMode.Placer
        }
        ProgressState.Started -> {
            _isSafehouseDraggable.value = false
            _arMode.value = ArMode.Scanner
            onGameStarted()
        }
        ProgressState.Ended -> {
            _isSafehouseDraggable.value = false
            removeGeofencesListener()
        }
        else -> _isSafehouseDraggable.value = false
    }

    private fun GameState.onGameStarted() {
        if (safehouse.isPlaced && redFlag.isPlaced && greenFlag.isPlaced)
            startGeofencesListener(this)
    }

    private fun startGeofencesListener(gameState: GameState) = with(gameState) {
        geofencingHelper.addGeofence(safehouse.position, GAME_BOUNDARIES_GEOFENCE_ID, DEFAULT_GAME_BOUNDARIES_RADIUS)
        geofencingHelper.addGeofence(safehouse.position, SAFEHOUSE_GEOFENCE_ID, DEFAULT_SAFEHOUSE_RADIUS)
        geofencingHelper.addGeofence(greenFlag.position, GREEN_FLAG_GEOFENCE_ID, DEFAULT_FLAG_RADIUS)
        geofencingHelper.addGeofence(redFlag.position, RED_FLAG_GEOFENCE_ID, DEFAULT_FLAG_RADIUS)
        geofencingHelper.addGeofences()
    }

    private fun removeGeofencesListener() {
        geofencingHelper.removeGeofences()
    }

    fun onSetGameClicked() {
        viewModelScope.launch {
            firestoreRepository.updatePlayerStatus(Player.Status.Playing)
        }
    }

    fun generateQrCode(text: String): Bitmap? {
        try {
            val qrCodeWriter = QRCodeWriter()
            val bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, 200, 200)
            val bitmap = Bitmap.createBitmap(200, 200, Bitmap.Config.RGB_565)
            for (x in 0..199) {
                for (y in 0..199) {
                    bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.DKGRAY else Color.WHITE)
                }
            }
            return bitmap
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    fun updateSafeHousePosition(position: LatLng) {
        val gameID = _player.value.gameDetails?.gameID ?: return
        viewModelScope.launch {
            firestoreRepository.updateSafehousePosition(gameID, position)
        }
    }

    fun onSetFlagsClicked() {
        val gameID = _player.value.gameDetails?.gameID ?: return
        viewModelScope.launch {
            firestoreRepository.updateGameStatus(gameID, ProgressState.SettingFlags)
        }
    }

    companion object {

        private const val GAME_BOUNDARIES_GEOFENCE_ID = "GameBoundariesGeofence"
        private const val SAFEHOUSE_GEOFENCE_ID = "SafehouseGeofence"
        private const val GREEN_FLAG_GEOFENCE_ID = "GreenFlagGeofence"
        private const val RED_FLAG_GEOFENCE_ID = "RedFlagGeofence"
    }
}
