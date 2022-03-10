package com.lamti.capturetheflag.presentation.ui.components.composables.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.rememberMarkerDragState
import com.lamti.capturetheflag.presentation.ui.DEFAULT_GAME_BOUNDARIES_RADIUS
import com.lamti.capturetheflag.presentation.ui.DEFAULT_SAFEHOUSE_RADIUS

@Composable
 fun GameBoundariesGeofence(
    safehousePosition: LatLng,
    safeHouseIcon: BitmapDescriptor?,
    safeHouseTitle: String,
    isSafeHouseDraggable: Boolean,
    onMarkerClicked: (LatLng) -> Unit = {}
) {
    val dragState = rememberMarkerDragState()
    var finalPosition by remember { mutableStateOf(safehousePosition) }

    MapMarker(
        position = finalPosition,
        icon = safeHouseIcon,
        title = safeHouseTitle,
        hasGeofence = true,
        radius = DEFAULT_SAFEHOUSE_RADIUS.toDouble(),
        draggable = isSafeHouseDraggable,
        dragState = dragState,
    ) {
        finalPosition = it.position
        onMarkerClicked(finalPosition)
    }
    Circle(
        center = finalPosition,
        radius = DEFAULT_GAME_BOUNDARIES_RADIUS.toDouble(),
        strokeColor = Color.Blue,
        strokeWidth = 10f,
    )
}
