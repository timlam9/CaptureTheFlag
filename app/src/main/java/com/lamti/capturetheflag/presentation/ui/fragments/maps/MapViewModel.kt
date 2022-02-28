package com.lamti.capturetheflag.presentation.ui.fragments.maps

import android.app.PendingIntent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.lamti.capturetheflag.domain.FirestoreRepository
import com.lamti.capturetheflag.domain.player.Player
import com.lamti.capturetheflag.presentation.location.geofences.GeofencingHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MapViewModel @Inject constructor(
    private val geofencingHelper: GeofencingHelper,
    private val firestoreRepository: FirestoreRepository
) : ViewModel() {

    private val _gameState = MutableStateFlow(GameState.Started())
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    private val _player = MutableStateFlow(Player.emptyPlayer())
    val player: StateFlow<Player> = _player.asStateFlow()

    init {
        viewModelScope.launch {
            _player.update { firestoreRepository.getPlayer("LYHQT7MYtzPfg7hKNhqM") }
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
