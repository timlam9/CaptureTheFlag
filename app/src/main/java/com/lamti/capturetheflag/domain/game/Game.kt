package com.lamti.capturetheflag.domain.game

import com.google.android.gms.maps.model.LatLng
import com.lamti.capturetheflag.domain.player.Team
import com.lamti.capturetheflag.presentation.ui.DEFAULT_FLAG_RADIUS
import com.lamti.capturetheflag.presentation.ui.DEFAULT_GAME_RADIUS
import com.lamti.capturetheflag.utils.EMPTY
import com.lamti.capturetheflag.utils.emptyPosition
import java.util.Date

data class Game(
    val gameID: String,
    val title: String,
    val flagRadius: Float,
    val gameRadius: Float,
    val gameState: GameState,
    val redPlayers: List<ActivePlayer>,
    val greenPlayers: List<ActivePlayer>,
    val battles: List<Battle>,
) {

    companion object {

        fun initialGame(
            position: LatLng = emptyPosition(),
            gameID: String = EMPTY,
            title: String = EMPTY,
            flagRadius: Float = DEFAULT_FLAG_RADIUS,
            gameRadius: Float = DEFAULT_GAME_RADIUS,
        ) = Game(
            gameID = gameID,
            title = title,
            flagRadius = flagRadius,
            gameRadius = gameRadius,
            gameState = GameState.initialGameState(position = position),
            redPlayers = emptyList(),
            greenPlayers = emptyList(),
            battles = emptyList()
        )
    }
}

data class ActivePlayer(
    val id: String,
    val hasLost: Boolean
)

data class Battle(
    val battleID: String,
    val playersIDs: List<String>
)

data class GameState(
    val safehouse: GeofenceObject,
    val greenFlag: GeofenceObject,
    val redFlag: GeofenceObject,
    val greenFlagCaptured: String?,
    val redFlagCaptured: String?,
    val state: ProgressState,
    val winners: Team
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
            greenFlagCaptured = null,
            redFlagCaptured = null,
            state = ProgressState.Idle,
            winners = Team.Unknown
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

data class GamePlayer(
    val id: String,
    val team: Team,
    val position: LatLng,
    val username: String,
)
