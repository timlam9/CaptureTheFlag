package com.lamti.capturetheflag.presentation.ui.components.map

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.lamti.capturetheflag.presentation.ui.MapStyle
import com.lamti.capturetheflag.presentation.ui.fragments.maps.MapViewModel

@Composable
fun MapScreen(
    viewModel: MapViewModel,
    enteredGeofenceId: String,
    onSettingFlagsButtonClicked: () -> Unit,
    onArScannerButtonClicked: () -> Unit
) {
    val (mapProperties, uiSettings) = setupMap()

    GameStartedUI(
        mapProperties = mapProperties,
        uiSettings = uiSettings,
        viewModel = viewModel,
        enteredGeofenceId = enteredGeofenceId,
        onSettingFlagsButtonClicked = onSettingFlagsButtonClicked,
        onArScannerButtonClicked = onArScannerButtonClicked,
    )
}

@Composable
private fun setupMap(darkTheme: Boolean = isSystemInDarkTheme()): Pair<MapProperties, MapUiSettings> {
    val mapProperties by remember {
        mutableStateOf(
            MapProperties(
                isMyLocationEnabled = true,
                mapStyleOptions = if (darkTheme) MapStyleOptions(MapStyle.sinCity) else MapStyleOptions(MapStyle.cleanGrey)
            )
        )
    }
    val uiSettings by remember {
        mutableStateOf(
            MapUiSettings(
                myLocationButtonEnabled = false,
                zoomControlsEnabled = false,
                mapToolbarEnabled = false
            )
        )
    }

    return Pair(mapProperties, uiSettings)
}
