package com.lamti.capturetheflag.presentation.ui.components.composables.map

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.MarkerState
import com.lamti.capturetheflag.presentation.ui.DEFAULT_SAFEHOUSE_RADIUS

@Composable
 fun GameBoundariesGeofence(
    safeHouseIcon: BitmapDescriptor?,
    safeHouseTitle: String,
    isSafeHouseDraggable: Boolean,
    gameRadius: Float,
    markerState: MarkerState
) {
    MapMarker(
        icon = safeHouseIcon,
        title = safeHouseTitle,
        hasGeofence = true,
        radius = DEFAULT_SAFEHOUSE_RADIUS.toDouble(),
        draggable = isSafeHouseDraggable,
        markerState = markerState,
    )
    Circle(
        center = markerState.position,
        radius = gameRadius.toDouble(),
        strokeColor = Color.Blue,
        strokeWidth = 10f,
    )
}
