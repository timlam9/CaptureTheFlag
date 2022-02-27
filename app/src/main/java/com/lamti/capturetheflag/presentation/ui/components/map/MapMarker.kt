package com.lamti.capturetheflag.presentation.ui.components.map

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.Marker
import com.lamti.capturetheflag.presentation.ui.fragments.maps.testFlagRadius
import com.lamti.capturetheflag.presentation.ui.style.BlueOpacity

@Composable
fun MapMarker(
    position: LatLng,
    icon: BitmapDescriptor?,
    title: String,
    hasGeofence: Boolean = false,
    fillColor: Color = BlueOpacity,
    strokeColor: Color = Color.Blue,
    radius: Double = testFlagRadius.toDouble(),
    strokeWidth: Float = 4f
) {
    Marker(
        position = position,
        title = title,
        icon = icon,
        onClick = {
            return@Marker false
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
