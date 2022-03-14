package com.lamti.capturetheflag.domain.game

import com.google.android.gms.maps.model.LatLng
import com.lamti.capturetheflag.utils.EMPTY
import com.lamti.capturetheflag.utils.emptyPosition
import java.util.Date

data class Game(
    val gameID: String,
    val title: String,
    val gameState: GameState,
    val redPlayers: List<String>,
    val greenPlayers: List<String>,
) {

    companion object {

        fun initialGame(position: LatLng = emptyPosition(), gameID: String = EMPTY, title: String = EMPTY) = Game(
            gameID = gameID,
            title = title,
            gameState = GameState.initialGameState(position = position),
            redPlayers = emptyList(),
            greenPlayers = emptyList()
        )
    }
}

data class GameState(
    val safehouse: GeofenceObject,
    val greenFlag: GeofenceObject,
    val redFlag: GeofenceObject,
    val greenFlagGrabbed: String?,
    val redFlagGrabbed: String?,
    val state: ProgressState
) {

    companion object {

        fun initialGameState(position: LatLng = emptyPosition()) = GameState(
            safehouse = GeofenceObject(
                position = position,
                isPlaced = false,
                isDiscovered = false,
                id = EMPTY,
                timestamp = Date()
            ),
            greenFlag = GeofenceObject(
                position = emptyPosition(),
                isPlaced = false,
                isDiscovered = false,
                id = EMPTY,
                timestamp = Date()
            ),
            redFlag = GeofenceObject(
                position = emptyPosition(),
                isPlaced = false,
                isDiscovered = false,
                id = EMPTY,
                timestamp = Date()
            ),
            greenFlagGrabbed = null,
            redFlagGrabbed = null,
            state = ProgressState.Idle
        )
    }
}

data class GeofenceObject(
    val position: LatLng,
    val isPlaced: Boolean,
    val isDiscovered: Boolean,
    val id: String,
    val timestamp: Date = Date()
)

enum class ProgressState {

    Idle,
    Created,
    SettingGame,
    SettingFlags,
    Started,
    Ended
}
