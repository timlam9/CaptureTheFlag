package com.lamti.capturetheflag.presentation.ui.components.composables.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.Color
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.rememberMarkerState
import com.lamti.capturetheflag.domain.game.GeofenceObject
import com.lamti.capturetheflag.domain.player.Team
import com.lamti.capturetheflag.presentation.ui.style.GreenOpacity
import com.lamti.capturetheflag.presentation.ui.style.RedOpacity

@Composable
fun FlagMarkers(
    team: Team,
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
        redFlagMarkerState.position != LatLng(0.0, 0.0) &&
        (team == Team.Red || redFlag.isDiscovered) &&
        redFlagPlayer == null
    ) {
        MapMarker(
            markerState = redFlagMarkerState,
            icon = redFlagIcon,
            title = redFlagMarkerTitle,
            hasGeofence = true,
            fillColor = RedOpacity,
            strokeColor = Color.Red,
        )
    }
    if (
        greenFlagMarkerState.position != LatLng(0.0, 0.0) &&
        (team == Team.Green || greenFlag.isDiscovered) &&
        greenFlagPlayer == null
    ) {
        MapMarker(
            markerState = greenFlagMarkerState,
            icon = greenFlagIcon,
            title = greenFlagMarkerTitle,
            hasGeofence = true,
            fillColor = GreenOpacity,
            strokeColor = Color.Green,
        )
    }
}
