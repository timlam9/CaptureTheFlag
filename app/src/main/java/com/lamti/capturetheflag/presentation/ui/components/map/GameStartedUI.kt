package com.lamti.capturetheflag.presentation.ui.components.map

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.maps.model.CameraPosition
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.rememberCameraPositionState
import com.lamti.capturetheflag.R
import com.lamti.capturetheflag.presentation.ui.bitmapDescriptorFromVector
import com.lamti.capturetheflag.presentation.ui.fragments.maps.GameState
import com.lamti.capturetheflag.presentation.ui.fragments.maps.testSafeHouseRadius
import com.lamti.capturetheflag.presentation.ui.style.GreenOpacity
import com.lamti.capturetheflag.presentation.ui.style.RedOpacity

@Composable
fun GameStartedUI(
    gameState: GameState,
    mapProperties: MapProperties,
    uiSettings: MapUiSettings,
    flagMarkerTitle: String = "Green Flag",
    opponentFlagMarkerTitle: String = "Red Flag",
    safeHouseTitle: String = "Safe House"
) {
    val startedGameState = gameState as GameState.Started
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(startedGameState.safeHousePosition, 15f)
    }

    val context = LocalContext.current
    val flagIcon = context.bitmapDescriptorFromVector(R.drawable.ic_flag, R.color.green)
    val opponentFlagIcon = context.bitmapDescriptorFromVector(R.drawable.ic_flag, R.color.red)
    val safeHouseIcon = context.bitmapDescriptorFromVector(R.drawable.ic_safety, R.color.blue)

    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState,
        properties = mapProperties,
        uiSettings = uiSettings
    ) {
        MapMarker(
            position = startedGameState.flagPosition,
            icon = flagIcon,
            title = flagMarkerTitle,
            hasGeofence = true,
            fillColor = GreenOpacity,
            strokeColor = Color.Green,
        )
        MapMarker(
            position = startedGameState.opponentFlagPosition,
            icon = opponentFlagIcon,
            title = opponentFlagMarkerTitle,
            hasGeofence = true,
            fillColor = RedOpacity,
            strokeColor = Color.Red,
        )
        MapMarker(
            position = startedGameState.safeHousePosition,
            icon = safeHouseIcon,
            title = safeHouseTitle,
            hasGeofence = true,
            radius = testSafeHouseRadius.toDouble(),
        )
        Circle(
            center = startedGameState.safeHousePosition,
            radius = 750.0,
            strokeColor = Color.Blue,
            strokeWidth = 10f,
        )
    }
}
