package com.lamti.capturetheflag.domain.game

import com.google.android.gms.maps.model.LatLng


data class Game(
    val gameID: String,
    val title: String,
    val gameState: GameState
)

data class GameState(
    val safehouse: GeofenceObject,
    val greenFlag: GeofenceObject,
    val redFlag: GeofenceObject,
    val state: ProgressState
)

data class GeofenceObject(
    val position: LatLng,
    val isPlaced: Boolean,
    val isDiscovered: Boolean
)

enum class ProgressState {

    Waiting,
    Initializing,
    Started,
    Ended
}
