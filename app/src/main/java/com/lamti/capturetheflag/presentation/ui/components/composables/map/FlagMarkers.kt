package com.lamti.capturetheflag.presentation.ui.components.composables.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.Color
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.maps.android.compose.rememberMarkerState
import com.lamti.capturetheflag.domain.game.GeofenceObject
import com.lamti.capturetheflag.domain.player.Team
import com.lamti.capturetheflag.presentation.ui.style.GreenOpacity
import com.lamti.capturetheflag.presentation.ui.style.RedOpacity
import com.lamti.capturetheflag.utils.emptyPosition

@Composable
fun FlagMarkers(
    team: Team,
    flagRadius: Float,
    redFlag: GeofenceObject,
    greenFlag: GeofenceObject,
    redFlagIcon: BitmapDescriptor?,
    redFlagMarkerTitle: String,
    greenFlagIcon: BitmapDescriptor?,
    greenFlagMarkerTitle: String,
    redFlagPlayer: String?,
    greenFlagPlayer: String?
) {
    val redFlagMarkerState = rememberMarkerState(position = redFlag.position)
    val greenFlagMarkerState = rememberMarkerState(position = greenFlag.position)

    LaunchedEffect(key1 = redFlag.position) {
        redFlagMarkerState.position = redFlag.position
    }
    LaunchedEffect(key1 = greenFlag.position) {
        greenFlagMarkerState.position = greenFlag.position
    }

    if (
        redFlagMarkerState.position != emptyPosition() &&
        (team == Team.Red || redFlag.isDiscovered)
    ) {
        MapMarker(
            markerState = redFlagMarkerState,
            icon = redFlagIcon,
            title = redFlagMarkerTitle,
            hasGeofence = true,
            radius = flagRadius.toDouble(),
            fillColor = RedOpacity,
            strokeColor = Color.Red,
            showMarker = redFlagPlayer == null
        )
    }
    if (
        greenFlagMarkerState.position != emptyPosition() &&
        (team == Team.Green || greenFlag.isDiscovered)
    ) {
        MapMarker(
            markerState = greenFlagMarkerState,
            icon = greenFlagIcon,
            title = greenFlagMarkerTitle,
            hasGeofence = true,
            radius = flagRadius.toDouble(),
            fillColor = GreenOpacity,
            strokeColor = Color.Green,
            showMarker = greenFlagPlayer == null
        )
    }
}
