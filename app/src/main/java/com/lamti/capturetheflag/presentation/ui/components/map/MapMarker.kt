package com.lamti.capturetheflag.presentation.ui.components.map

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerDragState
import com.lamti.capturetheflag.presentation.ui.DEFAULT_FLAG_RADIUS
import com.lamti.capturetheflag.presentation.ui.style.BlueOpacity

@Composable
fun MapMarker(
    position: LatLng,
    icon: BitmapDescriptor?,
    title: String,
    hasGeofence: Boolean = false,
    fillColor: Color = BlueOpacity,
    strokeColor: Color = Color.Blue,
    radius: Double = DEFAULT_FLAG_RADIUS.toDouble(),
    strokeWidth: Float = 4f,
    draggable: Boolean = false,
    dragState: MarkerDragState? = null,
    onMarkerClicked: (Marker) -> Unit = {}
) {
    Marker(
        position = position,
        title = title,
        icon = icon,
        draggable = draggable,
        markerDragState = dragState,
        onClick = {
            if (draggable) onMarkerClicked(it)
            return@Marker draggable
        }
    )
    if (hasGeofence) {
        Circle(
            center = position,
            radius = radius,
            fillColor = fillColor,
            strokeColor = strokeColor,
            strokeWidth = strokeWidth,
        )
    }
}
