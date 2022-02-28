package com.lamti.capturetheflag.presentation.ui.components.map

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.CameraPosition
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.rememberCameraPositionState
import com.lamti.capturetheflag.R
import com.lamti.capturetheflag.domain.player.Player
import com.lamti.capturetheflag.domain.player.Team
import com.lamti.capturetheflag.presentation.ui.bitmapDescriptorFromVector
import com.lamti.capturetheflag.presentation.ui.fragments.maps.DEFAULT_GAME_BOUNDARIES_RADIUS
import com.lamti.capturetheflag.presentation.ui.fragments.maps.GameState
import com.lamti.capturetheflag.presentation.ui.fragments.maps.DEFAULT_SAFEHOUSE_RADIUS
import com.lamti.capturetheflag.presentation.ui.style.GreenOpacity
import com.lamti.capturetheflag.presentation.ui.style.RedOpacity

@Composable
fun GameStartedUI(
    gameState: GameState,
    player: Player,
    mapProperties: MapProperties,
    uiSettings: MapUiSettings,
    greenFlagMarkerTitle: String = "Green Flag",
    redFlagMarkerTitle: String = "Red Flag",
    safeHouseTitle: String = "Safe House"
) {
    val startedGameState = gameState as GameState.Started
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(startedGameState.safeHousePosition, 15f)
    }

    val context = LocalContext.current
    val greenFlagIcon = context.bitmapDescriptorFromVector(R.drawable.ic_flag, R.color.green)
    val redFlagIcon = context.bitmapDescriptorFromVector(R.drawable.ic_flag, R.color.red)
    val safeHouseIcon = context.bitmapDescriptorFromVector(R.drawable.ic_safety, R.color.blue)

    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState,
        properties = mapProperties,
        uiSettings = uiSettings
    ) {
        GameBoundariesGeofence(startedGameState, safeHouseIcon, safeHouseTitle)
        ShowFlags(
            player = player,
            startedGameState = startedGameState,
            redFlagIcon = redFlagIcon,
            redFlagMarkerTitle = redFlagMarkerTitle,
            greenFlagIcon = greenFlagIcon,
            greenFlagMarkerTitle = greenFlagMarkerTitle
        )
    }
}

@Composable
private fun GameBoundariesGeofence(
    startedGameState: GameState.Started,
    safeHouseIcon: BitmapDescriptor?,
    safeHouseTitle: String
) {
    MapMarker(
        position = startedGameState.safeHousePosition,
        icon = safeHouseIcon,
        title = safeHouseTitle,
        hasGeofence = true,
        radius = DEFAULT_SAFEHOUSE_RADIUS.toDouble(),
    )
    Circle(
        center = startedGameState.safeHousePosition,
        radius = DEFAULT_GAME_BOUNDARIES_RADIUS.toDouble(),
        strokeColor = Color.Blue,
        strokeWidth = 10f,
    )
}

@Composable
private fun ShowFlags(
    player: Player,
    startedGameState: GameState.Started,
    redFlagIcon: BitmapDescriptor?,
    redFlagMarkerTitle: String,
    greenFlagIcon: BitmapDescriptor?,
    greenFlagMarkerTitle: String
) {
    when (player.team) {
        Team.Red -> {
            MapMarker(
                position = startedGameState.redFlagPosition,
                icon = redFlagIcon,
                title = redFlagMarkerTitle,
                hasGeofence = true,
                fillColor = RedOpacity,
                strokeColor = Color.Red,
            )
        }
        Team.Green -> {
            MapMarker(
                position = startedGameState.greenFlagPosition,
                icon = greenFlagIcon,
                title = greenFlagMarkerTitle,
                hasGeofence = true,
                fillColor = GreenOpacity,
                strokeColor = Color.Green,
            )
        }
        Team.Unknown -> Unit
    }
}
