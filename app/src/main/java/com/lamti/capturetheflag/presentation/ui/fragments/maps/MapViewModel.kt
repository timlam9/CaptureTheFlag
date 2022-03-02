package com.lamti.capturetheflag.presentation.ui.fragments.maps

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.lamti.capturetheflag.data.firestore.gameID
import com.lamti.capturetheflag.data.firestore.playerID
import com.lamti.capturetheflag.data.location.LocationRepository
import com.lamti.capturetheflag.data.location.geofences.GeofencingRepository
import com.lamti.capturetheflag.domain.FirestoreRepository
import com.lamti.capturetheflag.domain.game.GameState
import com.lamti.capturetheflag.domain.game.ProgressState
import com.lamti.capturetheflag.domain.player.Player
import com.lamti.capturetheflag.utils.emptyPosition
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

    private val _currentPosition: MutableState<LatLng> = mutableStateOf(emptyPosition())
    val currentPosition: State<LatLng> = _currentPosition

    private val _gameState = mutableStateOf(GameUiState.Started())
    val gameState: State<GameUiState> = _gameState

    private val _player = mutableStateOf(Player.emptyPlayer())
    val player: State<Player> = _player

    init {
        getLastLocation()
        getPlayer()
        observeGameState()
    }

    private fun getLastLocation() {
        viewModelScope.launch {
            val location = locationRepository.awaitLastLocation()
            _currentPosition.value = LatLng(location.latitude, location.longitude)
        }
    }

    private fun getPlayer() {
        viewModelScope.launch {
            _player.value = firestoreRepository.getPlayer(playerID)
        }
    }

    private fun observeGameState() {
        viewModelScope.launch {
            firestoreRepository.observeGameState(gameID).onEach { gameState ->
                updateUiGameStateValue(gameState)
                handleGameStateEvents(gameState)
            }.launchIn(viewModelScope)
        }
    }

    private fun updateUiGameStateValue(gameState: GameState) = with(gameState) {
        _gameState.value = _gameState.value.copy(
            isGreenFlagFound = greenFlag.isDiscovered,
            isRedFlagFound = redFlag.isDiscovered,
            greenFlagPosition = greenFlag.position,
            redFlagPosition = redFlag.position,
            safeHousePosition = safehouse.position
        )
    }

    private fun handleGameStateEvents(gameState: GameState) = with(gameState) {
        when (state) {
            ProgressState.Waiting -> Unit
            ProgressState.Initializing -> Unit
            ProgressState.Started -> {
                if (safehouse.isPlaced && redFlag.isPlaced && greenFlag.isPlaced) {
                    startGeofencesListener(this)
                }
            }
            ProgressState.Ended -> removeGeofencesListener()
        }
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

    companion object {

        private const val GAME_BOUNDARIES_GEOFENCE_ID = "GameBoundariesGeofence"
        private const val SAFEHOUSE_GEOFENCE_ID = "SafehouseGeofence"
        private const val GREEN_FLAG_GEOFENCE_ID = "GreenFlagGeofence"
        private const val RED_FLAG_GEOFENCE_ID = "RedFlagGeofence"
    }
}
