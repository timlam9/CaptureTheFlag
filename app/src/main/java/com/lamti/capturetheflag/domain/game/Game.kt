package com.lamti.capturetheflag.domain.game

import com.google.android.gms.maps.model.LatLng
import com.lamti.capturetheflag.utils.emptyPosition


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
) {

    companion object {

        fun initialGameState(position: LatLng = emptyPosition()) = GameState(
            safehouse = GeofenceObject(
                position = position,
                isPlaced = false,
                isDiscovered = false
            ), greenFlag = GeofenceObject(
                position = emptyPosition(),
                isPlaced = false,
                isDiscovered = false
            ), redFlag = GeofenceObject(
                position = emptyPosition(),
                isPlaced = false,
                isDiscovered = false
            ), state = ProgressState.Idle
        )
    }
}

data class GeofenceObject(
    val position: LatLng,
    val isPlaced: Boolean,
    val isDiscovered: Boolean
)

enum class ProgressState {

    Idle,
    Created,
    SettingGame,
    SettingFlags,
    Started,
    Ended
}
