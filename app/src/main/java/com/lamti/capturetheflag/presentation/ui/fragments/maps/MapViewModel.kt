package com.lamti.capturetheflag.presentation.ui.fragments.maps

import android.app.PendingIntent
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.lamti.capturetheflag.data.gameID
import com.lamti.capturetheflag.data.playerID
import com.lamti.capturetheflag.domain.FirestoreRepository
import com.lamti.capturetheflag.domain.player.Player
import com.lamti.capturetheflag.presentation.location.geofences.GeofencingHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MapViewModel @Inject constructor(
    private val geofencingHelper: GeofencingHelper,
    private val firestoreRepository: FirestoreRepository
) : ViewModel() {

    private val _gameState = mutableStateOf(GameUiState.Started())
    val gameState: State<GameUiState> = _gameState

    private val _player = mutableStateOf(Player.emptyPlayer())
    val player: State<Player> = _player

    init {
        viewModelScope.launch {
            _player.value = firestoreRepository.getPlayer(playerID)
        }
        viewModelScope.launch {
            firestoreRepository.observeGameState(gameID).onEach {
                _gameState.value = _gameState.value.copy(
                    isGreenFlagFound = it.isGreenFlagDiscovered,
                    isRedFlagFound = it.isRedFlagDiscovered,
                )
            }.launchIn(viewModelScope)
        }
    }

    fun addGeofence(position: LatLng, id: String, radius: Float) {
        geofencingHelper.addGeofence(position, id, radius)
    }

    fun startGeofencesListener(geofencePendingIntent: PendingIntent) {
        geofencingHelper.addGeofences(geofencePendingIntent)
    }

    fun removeGeofencesListener(geofencePendingIntent: PendingIntent) {
        geofencingHelper.removeGeofences(geofencePendingIntent)
    }

}
