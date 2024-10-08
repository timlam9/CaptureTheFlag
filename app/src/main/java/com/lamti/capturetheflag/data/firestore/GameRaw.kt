package com.lamti.capturetheflag.data.firestore

import com.google.android.gms.maps.model.LatLng
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.ServerTimestamp
import com.lamti.capturetheflag.data.authentication.toTeam
import com.lamti.capturetheflag.data.firestore.ActivePlayerRaw.Companion.toRaw
import com.lamti.capturetheflag.data.firestore.BattleRaw.Companion.toRaw
import com.lamti.capturetheflag.data.firestore.BattlingPlayerRaw.Companion.toRaw
import com.lamti.capturetheflag.data.firestore.GameStateRaw.Companion.toRaw
import com.lamti.capturetheflag.data.firestore.GeofenceObjectRaw.Companion.toRaw
import com.lamti.capturetheflag.domain.game.ActivePlayer
import com.lamti.capturetheflag.domain.game.Battle
import com.lamti.capturetheflag.domain.game.BattleMiniGame
import com.lamti.capturetheflag.domain.game.BattleState
import com.lamti.capturetheflag.domain.game.BattlingPlayer
import com.lamti.capturetheflag.domain.game.Game
import com.lamti.capturetheflag.domain.game.GamePlayer
import com.lamti.capturetheflag.domain.game.GameState
import com.lamti.capturetheflag.domain.game.GeofenceObject
import com.lamti.capturetheflag.domain.game.ProgressState
import com.lamti.capturetheflag.domain.player.Team
import com.lamti.capturetheflag.presentation.ui.DEFAULT_FLAG_RADIUS
import com.lamti.capturetheflag.presentation.ui.DEFAULT_GAME_RADIUS
import com.lamti.capturetheflag.utils.EMPTY
import java.util.Date

data class GameRaw(
    val gameID: String = EMPTY,
    val title: String = EMPTY,
    val flagRadius: Float = DEFAULT_FLAG_RADIUS,
    val gameRadius: Float = DEFAULT_GAME_RADIUS,
    val gameState: GameStateRaw = GameStateRaw(),
    val battleMiniGame: String = BattleMiniGame.None.name,
    val redPlayers: List<ActivePlayerRaw> = emptyList(),
    val greenPlayers: List<ActivePlayerRaw> = emptyList(),
    val battles: List<BattleRaw> = emptyList()
) {

    fun toGame() = Game(
        gameID = gameID,
        title = title,
        flagRadius = flagRadius,
        gameRadius = gameRadius,
        gameState = gameState.toGameState(),
        battleMiniGame = battleMiniGame.toMiniGame(),
        redPlayers = redPlayers.map { it.toActivePlayer() },
        greenPlayers = greenPlayers.map { it.toActivePlayer() },
        battles = battles.toBattles()
    )

    companion object {

        fun Game.toRaw() = GameRaw(
            gameID = gameID,
            title = title,
            flagRadius = flagRadius,
            gameRadius = gameRadius,
            battleMiniGame = battleMiniGame.name,
            gameState = gameState.toRaw(),
            redPlayers = redPlayers.map { it.toRaw() },
            greenPlayers = greenPlayers.map { it.toRaw() },
            battles = battles.toRaw()
        )
    }
}

data class ActivePlayerRaw(
    val id: String = EMPTY,
    val hasLost: Boolean = false
) {

    fun toActivePlayer() = ActivePlayer(
        id = id,
        hasLost = hasLost
    )

    companion object {

        fun ActivePlayer.toRaw() = ActivePlayerRaw(
            id = id,
            hasLost = hasLost
        )
    }
}

private fun List<Battle>.toRaw() = map { it.toRaw() }

private fun List<BattleRaw>.toBattles() = map { it.toBattle() }

data class BattleRaw(
    val battleID: String = EMPTY,
    val state: String = BattleState.StandBy.name,
    val winner: String = EMPTY,
    val players: List<BattlingPlayerRaw> = emptyList()
) {

    fun toBattle() = Battle(
        battleID = battleID,
        state = state.toBattleState(),
        winner = winner,
        players = players.map { it.toBattlingPlayer() }
    )

    companion object {

        fun Battle.toRaw() = BattleRaw(
            battleID = battleID,
            state = state.name,
            winner = winner,
            players = players.map { it.toRaw() }
        )
    }
}

data class BattlingPlayerRaw(
    val id: String = EMPTY,
    val ready: Boolean = false
) {

    fun toBattlingPlayer() = BattlingPlayer(id = id, ready = ready)

    companion object {

        fun BattlingPlayer.toRaw() = BattlingPlayerRaw(id = id, ready = ready)
    }
}

data class GameStateRaw(
    val safehouse: GeofenceObjectRaw = GeofenceObjectRaw(),
    val greenFlag: GeofenceObjectRaw = GeofenceObjectRaw(),
    val redFlag: GeofenceObjectRaw = GeofenceObjectRaw(),
    val redFlagCaptured: String? = null,
    val greenFlagCaptured: String? = null,
    val state: String = "Idle",
    val winners: String = Team.Unknown.name
) {

    fun toGameState() = GameState(
        safehouse = safehouse.toGeofenceObject(),
        greenFlag = greenFlag.toGeofenceObject(),
        redFlag = redFlag.toGeofenceObject(),
        greenFlagCaptured = greenFlagCaptured,
        redFlagCaptured = redFlagCaptured,
        state = state.toState(),
        winners = winners.toTeam()
    )

    companion object {

        fun GameState.toRaw() = GameStateRaw(
            safehouse = safehouse.toRaw(),
            greenFlag = greenFlag.toRaw(),
            redFlag = redFlag.toRaw(),
            state = state.name,
            redFlagCaptured = redFlagCaptured,
            greenFlagCaptured = greenFlagCaptured,
            winners = winners.name
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

private fun String.toBattleState(): BattleState = when (this) {
    "StandBy" -> BattleState.StandBy
    "Started" -> BattleState.Started
    "Over" -> BattleState.Over
    else -> BattleState.StandBy
}

private fun String.toMiniGame(): BattleMiniGame = when (this) {
    "None" -> BattleMiniGame.None
    "TapTheFlag" -> BattleMiniGame.TapTheFlag
    else -> BattleMiniGame.None
}
