package com.lamti.capturetheflag.presentation.ui.components.composables.map

import androidx.compose.runtime.Composable
import com.google.android.gms.maps.model.BitmapDescriptor
import com.lamti.capturetheflag.domain.game.GamePlayer
import com.lamti.capturetheflag.domain.player.Team

@Composable
fun PlayerMarkers(
    otherPlayers: List<GamePlayer>,
    greenPersonPin: BitmapDescriptor?,
    redPersonPin: BitmapDescriptor?,
    userID: String
) {
    otherPlayers.onEach {
        if (it.id != userID) {
            val icon = if (it.team == Team.Green) greenPersonPin else redPersonPin

            MapMarker(
                position = it.position,
                icon = icon,
                title = it.username
            )
        }
    }
}
