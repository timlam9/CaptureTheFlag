package com.lamti.capturetheflag.presentation.ui.components.composables.map

import androidx.compose.runtime.Composable
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.MarkerState
import com.lamti.capturetheflag.presentation.ui.style.Blue

@Composable
 fun GameBoundariesGeofence(
    safeHouseIcon: BitmapDescriptor?,
    safeHouseTitle: String,
    safeHouseRadius: Float,
    isSafeHouseDraggable: Boolean,
    gameRadius: Float,
    markerState: MarkerState
) {
    MapMarker(
        icon = safeHouseIcon,
        title = safeHouseTitle,
        hasGeofence = true,
        radius = safeHouseRadius.toDouble(),
        draggable = isSafeHouseDraggable,
        markerState = markerState,
    )
    Circle(
        center = markerState.position,
        radius = gameRadius.toDouble(),
        strokeColor = Blue,
        strokeWidth = 10f,
    )
}
