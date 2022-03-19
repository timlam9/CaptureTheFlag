package com.lamti.capturetheflag.data.firestore

import com.google.android.gms.maps.model.LatLng
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.ServerTimestamp
import com.lamti.capturetheflag.data.authentication.toTeam
import com.lamti.capturetheflag.data.firestore.BattleRaw.Companion.toRaw
import com.lamti.capturetheflag.data.firestore.GameStateRaw.Companion.toRaw
import com.lamti.capturetheflag.data.firestore.GeofenceObjectRaw.Companion.toRaw
import com.lamti.capturetheflag.domain.game.Battle
import com.lamti.capturetheflag.domain.game.Game
import com.lamti.capturetheflag.domain.game.GamePlayer
import com.lamti.capturetheflag.domain.game.GameState
import com.lamti.capturetheflag.domain.game.GeofenceObject
import com.lamti.capturetheflag.domain.game.ProgressState
import com.lamti.capturetheflag.domain.player.Team
import com.lamti.capturetheflag.utils.EMPTY
import java.util.Date

data class GameRaw(
    val gameID: String = EMPTY,
    val title: String = EMPTY,
    val gameState: GameStateRaw = GameStateRaw(),
    val redPlayers: List<String> = emptyList(),
    val greenPlayers: List<String> = emptyList(),
    val battles: List<BattleRaw> = emptyList()
) {

    fun toGame() = Game(
        gameID = gameID,
        title = title,
        gameState = GameState(
            safehouse = gameState.safehouse.toGeofenceObject(),
            greenFlag = gameState.greenFlag.toGeofenceObject(),
            redFlag = gameState.redFlag.toGeofenceObject(),
            greenFlagCaptured = gameState.greenFlagCaptured,
            redFlagCaptured = gameState.redFlagCaptured,
            state = gameState.state.toState()
        ),
        redPlayers = redPlayers,
        greenPlayers = greenPlayers,
        battles = battles.toBattles()
    )

    companion object {

        fun Game.toRaw() = GameRaw(
            gameID = gameID,
            title = title,
            gameState = gameState.toRaw(),
            redPlayers = redPlayers,
            greenPlayers = greenPlayers,
            battles = battles.toRaw()
        )
    }
}

private fun List<Battle>.toRaw() = map { it.toRaw() }
private fun List<BattleRaw>.toBattles() = map { it.toBattle() }

data class BattleRaw(
    val battleID: String = EMPTY,
    val playersIDs: List<String> = emptyList()
) {

    fun toBattle() = Battle(
        battleID = battleID,
        playersIDs = playersIDs
    )

    companion object {

        fun Battle.toRaw() = BattleRaw(
            battleID = battleID,
            playersIDs = playersIDs
        )
    }
}

data class GameStateRaw(
    val safehouse: GeofenceObjectRaw = GeofenceObjectRaw(),
    val greenFlag: GeofenceObjectRaw = GeofenceObjectRaw(),
    val redFlag: GeofenceObjectRaw = GeofenceObjectRaw(),
    val redFlagCaptured: String? = null,
    val greenFlagCaptured: String? = null,
    val state: String = "Idle"
) {

    companion object {

        fun GameState.toRaw() = GameStateRaw(
            safehouse = safehouse.toRaw(),
            greenFlag = greenFlag.toRaw(),
            redFlag = redFlag.toRaw(),
            state = state.name,
            redFlagCaptured = redFlagCaptured,
            greenFlagCaptured = greenFlagCaptured
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

data class GamePlayerRaw(
    val id: String = EMPTY,
    val team: String = Team.Unknown.name,
    val position: GeoLocation = emptyGeoLocation,
    val username: String = EMPTY,
) {

    fun toGamePlayer() = GamePlayer(
        id = id,
        team = team.toTeam(),
        position = position.toLatLng(),
        username = username,
    )

    companion object {

        fun GamePlayer.toRaw() = GamePlayerRaw(
            id = id,
            team = team.name,
            position = position.toGeoLocation(),
            username = username,
        )
    }
}

data class GeoLocation(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
)

val emptyGeoPoint: GeoPoint = GeoPoint(0.0, 0.0)
val emptyGeoLocation: GeoLocation = GeoLocation(0.0, 0.0)

fun GeoPoint.toLatLng() = LatLng(latitude, longitude)

fun LatLng.toGeoPoint() = GeoPoint(latitude, longitude)

fun GeoLocation.toLatLng() = LatLng(latitude, longitude)

fun LatLng.toGeoLocation() = GeoLocation(latitude, longitude)

private fun String.toState(): ProgressState = when (this) {
    "Created" -> ProgressState.Created
    "SettingGame" -> ProgressState.SettingGame
    "SettingFlags" -> ProgressState.SettingFlags
    "Started" -> ProgressState.Started
    "Ended" -> ProgressState.Ended
    else -> ProgressState.Idle
}
