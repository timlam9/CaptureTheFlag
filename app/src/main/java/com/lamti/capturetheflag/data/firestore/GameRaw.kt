package com.lamti.capturetheflag.data.firestore

import com.google.android.gms.maps.model.LatLng
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.ServerTimestamp
import com.lamti.capturetheflag.data.firestore.GameStateRaw.Companion.toRaw
import com.lamti.capturetheflag.data.firestore.GeofenceObjectRaw.Companion.toRaw
import com.lamti.capturetheflag.domain.game.Game
import com.lamti.capturetheflag.domain.game.GameState
import com.lamti.capturetheflag.domain.game.GeofenceObject
import com.lamti.capturetheflag.domain.game.ProgressState
import com.lamti.capturetheflag.utils.EMPTY
import java.util.Date

data class GameRaw(
    val gameID: String = EMPTY,
    val title: String = EMPTY,
    val gameState: GameStateRaw = GameStateRaw()
) {

    fun toGame() = Game(
        gameID = gameID,
        title = title,
        gameState = GameState(
            safehouse = gameState.safehouse.toGeofenceObject(),
            greenFlag = gameState.greenFlag.toGeofenceObject(),
            redFlag = gameState.redFlag.toGeofenceObject(),
            state = gameState.state.toState()
        )
    )

    companion object {

        fun Game.toRaw() = GameRaw(
            gameID = gameID,
            title = title,
            gameState = gameState.toRaw()
        )
    }
}

data class GameStateRaw(
    val safehouse: GeofenceObjectRaw = GeofenceObjectRaw(),
    val greenFlag: GeofenceObjectRaw = GeofenceObjectRaw(),
    val redFlag: GeofenceObjectRaw = GeofenceObjectRaw(),
    val state: String = "Idle"
) {

    companion object {

        fun GameState.toRaw() = GameStateRaw(
            safehouse = safehouse.toRaw(),
            greenFlag = greenFlag.toRaw(),
            redFlag = redFlag.toRaw(),
            state = state.name
        )
    }
}


data class GeofenceObjectRaw(
    val position: GeoPoint = emptyGeoPoint,
    val placed: Boolean = false,
    val discovered: Boolean = false,
    val id: String = EMPTY,
    @ServerTimestamp
    val timestamp: Date = Date()
) {

    fun toGeofenceObject() = GeofenceObject(
        position = position.toLatLng(),
        isPlaced = placed,
        isDiscovered = discovered,
        id = id,
        timestamp = timestamp
    )


    companion object {
        fun GeofenceObject.toRaw(): GeofenceObjectRaw = GeofenceObjectRaw(
            position = position.toGeoPoint(),
            placed = isPlaced,
            discovered = isDiscovered,
            id = id,
            timestamp = timestamp
        )
    }
}

val emptyGeoPoint: GeoPoint = GeoPoint(0.0, 0.0)

fun GeoPoint.toLatLng() = LatLng(latitude, longitude)

fun LatLng.toGeoPoint() = GeoPoint(latitude, longitude)

private fun String.toState(): ProgressState = when (this) {
    "Created" -> ProgressState.Created
    "SettingGame" -> ProgressState.SettingGame
    "SettingFlags" -> ProgressState.SettingFlags
    "Started" -> ProgressState.Started
    "Ended" -> ProgressState.Ended
    else -> ProgressState.Idle
}
