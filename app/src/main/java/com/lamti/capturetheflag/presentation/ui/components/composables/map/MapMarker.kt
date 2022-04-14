package com.lamti.capturetheflag.presentation.ui.components.composables.map

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.lamti.capturetheflag.presentation.ui.DEFAULT_FLAG_RADIUS
import com.lamti.capturetheflag.presentation.ui.style.BlueOpacity

@Composable
fun MapMarker(
    icon: BitmapDescriptor?,
    title: String,
    fillColor: Color = BlueOpacity,
    strokeColor: Color = Color.Blue,
    radius: Double = DEFAULT_FLAG_RADIUS.toDouble(),
    strokeWidth: Float = 4f,
    showMarker: Boolean = true,
    hasGeofence: Boolean = false,
    draggable: Boolean = false,
    markerState: MarkerState,
) {
    if(showMarker) {
        Marker(
            title = title,
            icon = icon,
            draggable = draggable,
            state = markerState
        )
    }
    if (hasGeofence) {
        Circle(
            center = markerState.position,
            radius = radius,
            fillColor = fillColor,
            strokeColor = strokeColor,
            strokeWidth = strokeWidth,
        )
    }
}
