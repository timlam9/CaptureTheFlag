package com.lamti.capturetheflag.data

import com.google.android.gms.maps.model.LatLng
import com.google.firebase.firestore.GeoPoint
import com.lamti.capturetheflag.data.GameStateRaw.Companion.toRaw
import com.lamti.capturetheflag.data.GeofenceObjectRaw.Companion.toRaw
import com.lamti.capturetheflag.domain.game.Game
import com.lamti.capturetheflag.domain.game.GameState
import com.lamti.capturetheflag.domain.game.GeofenceObject
import com.lamti.capturetheflag.domain.game.ProgressState
import com.lamti.capturetheflag.utils.EMPTY

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

data class GeofenceObjectRaw(
    val position: GeoPoint = emptyGeoPoint,
    val placed: Boolean = false,
    val discovered: Boolean = false
) {

    fun toGeofenceObject() = GeofenceObject(
        position = position.toLatLng(),
        isPlaced = placed,
        isDiscovered = discovered
    )

    companion object {

        fun GeofenceObject.toRaw(): GeofenceObjectRaw = GeofenceObjectRaw(
            position = position.toGeoPoint(),
            placed = isPlaced,
            discovered = isDiscovered
        )
    }
}

data class GameStateRaw(
    val safehouse: GeofenceObjectRaw = GeofenceObjectRaw(),
    val greenFlag: GeofenceObjectRaw = GeofenceObjectRaw(),
    val redFlag: GeofenceObjectRaw = GeofenceObjectRaw(),
    val state: String = "Waiting"
) {

    companion object {

        fun GameState.toRaw() = GameStateRaw(
            safehouse = safehouse.toRaw(),
            greenFlag = greenFlag.toRaw(),
            redFlag =  redFlag.toRaw(),
            state = state.name
        )
    }
}

private val emptyGeoPoint: GeoPoint = GeoPoint(0.0, 0.0)

private fun GeoPoint.toLatLng() = LatLng(latitude, longitude)

private fun LatLng.toGeoPoint() = GeoPoint(latitude, longitude)

private fun String.toState(): ProgressState = when (this) {
    "Waiting" -> ProgressState.Waiting
    "Initializing" -> ProgressState.Initializing
    "Started" -> ProgressState.Started
    "Ended" -> ProgressState.Ended
    else -> ProgressState.Waiting
}
