package com.lamti.capturetheflag.presentation.ui.components.map

import android.app.PendingIntent
import android.util.Log
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.lamti.capturetheflag.presentation.location.geofences.GeofenceBroadcastReceiver
import com.lamti.capturetheflag.presentation.ui.MapStyle
import com.lamti.capturetheflag.presentation.ui.fragments.maps.DEFAULT_FLAG_RADIUS
import com.lamti.capturetheflag.presentation.ui.fragments.maps.DEFAULT_GAME_BOUNDARIES_RADIUS
import com.lamti.capturetheflag.presentation.ui.fragments.maps.DEFAULT_SAFEHOUSE_RADIUS
import com.lamti.capturetheflag.presentation.ui.fragments.maps.GameUiState
import com.lamti.capturetheflag.presentation.ui.fragments.maps.MapViewModel
import com.lamti.capturetheflag.presentation.ui.fragments.maps.testGreenFlagPosition
import com.lamti.capturetheflag.presentation.ui.fragments.maps.testRedFlagPosition
import com.lamti.capturetheflag.presentation.ui.fragments.maps.testSafeHousePosition
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
@Composable
fun MapScreen(
    lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
    viewModel: MapViewModel = viewModel(),
    geofencePendingIntent: PendingIntent,
) {
    val gameState by viewModel.gameState
    val player by viewModel.player

    val (mapProperties, uiSettings) = setupMap(lifecycleOwner, viewModel, geofencePendingIntent)

    when (gameState) {
        is GameUiState.Started -> GameStartedUI(
            gameState = gameState,
            player = player,
            mapProperties = mapProperties,
            uiSettings = uiSettings
        )
    }
}

@ExperimentalCoroutinesApi
@Composable
private fun setupMap(
    lifecycleOwner: LifecycleOwner,
    viewModel: MapViewModel,
    geofencePendingIntent: PendingIntent,
    darkTheme: Boolean = isSystemInDarkTheme()
): Pair<MapProperties, MapUiSettings> {
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) {
                Log.i(GeofenceBroadcastReceiver.TAG, "On start")
                viewModel.addGeofence(testGreenFlagPosition, "GreenFlagGeofence", DEFAULT_FLAG_RADIUS)
                viewModel.addGeofence(testRedFlagPosition, "RedFlagGeofence", DEFAULT_FLAG_RADIUS)
                viewModel.addGeofence(testSafeHousePosition, "SafehouseGeofence", DEFAULT_SAFEHOUSE_RADIUS)
                viewModel.addGeofence(testSafeHousePosition, "GameBoundariesGeofence", DEFAULT_GAME_BOUNDARIES_RADIUS)
                viewModel.startGeofencesListener(geofencePendingIntent)
            } else if (event == Lifecycle.Event.ON_STOP) {
                Log.i(GeofenceBroadcastReceiver.TAG, "On stop")
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            Log.i(GeofenceBroadcastReceiver.TAG, "On dispose")
            viewModel.removeGeofencesListener(geofencePendingIntent)
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

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
                myLocationButtonEnabled = true,
                zoomControlsEnabled = false,
                mapToolbarEnabled = false
            )
        )
    }

    return Pair(mapProperties, uiSettings)
}
