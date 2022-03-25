package com.lamti.capturetheflag.presentation.ui.components.composables.map

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.google.android.gms.maps.model.BitmapDescriptor
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
    greenFlagPlayer: String?,
) {
    if ((team == Team.Red || redFlag.isDiscovered) && redFlagPlayer == null) {
        MapMarker(
            position = redFlag.position,
            icon = redFlagIcon,
            title = redFlagMarkerTitle,
            hasGeofence = true,
            fillColor = RedOpacity,
            strokeColor = Color.Red,
        )
    }
    if ((team == Team.Green || greenFlag.isDiscovered) && greenFlagPlayer == null) {
        MapMarker(
            position = greenFlag.position,
            icon = greenFlagIcon,
            title = greenFlagMarkerTitle,
            hasGeofence = true,
            fillColor = GreenOpacity,
            strokeColor = Color.Green,
        )
    }
}
