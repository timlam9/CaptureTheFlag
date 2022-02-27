package com.lamti.capturetheflag.presentation.ui.fragments.maps

import android.app.PendingIntent
import androidx.lifecycle.ViewModel
import com.google.android.gms.maps.model.LatLng
import com.lamti.capturetheflag.presentation.location.geofences.GeofencingHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class MapViewModel @Inject constructor(
    private val geofencingHelper: GeofencingHelper
) : ViewModel() {

    private val _gameState = MutableStateFlow(GameState.Started())
    val gameState: StateFlow<GameState> = MutableStateFlow(GameState.Started())

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
