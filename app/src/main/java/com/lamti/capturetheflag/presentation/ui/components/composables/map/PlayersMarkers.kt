package com.lamti.capturetheflag.presentation.ui.components.composables.map

import androidx.compose.runtime.Composable
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.maps.android.compose.MarkerState
import com.lamti.capturetheflag.domain.game.GamePlayer
import com.lamti.capturetheflag.domain.player.Team

@Composable
fun PlayerMarkers(
    otherPlayers: List<GamePlayer>,
    greenPersonPin: BitmapDescriptor?,
    redPersonPin: BitmapDescriptor?,
    redFlagIcon: BitmapDescriptor?,
    greenFlagIcon: BitmapDescriptor?,
    userID: String,
    userTeam: Team,
    redFlagPlayer: String?,
    greenFlagPlayer: String?
) {
    otherPlayers.onEach {
        if (it.id != userID && it.team == userTeam) {
            val icon = if (it.team == Team.Green) greenPersonPin else redPersonPin
            MapMarker(
                markerState = MarkerState(position = it.position),
                icon = icon,
                title = it.username
            )
        }
        when {
            redFlagPlayer == it.id -> MapMarker(
                markerState = MarkerState(position = it.position),
                icon = redFlagIcon,
                title = it.username
            )
            greenFlagPlayer == it.id -> MapMarker(
                markerState = MarkerState(position = it.position),
                icon = greenFlagIcon,
                title = it.username
            )
        }

    }
}
