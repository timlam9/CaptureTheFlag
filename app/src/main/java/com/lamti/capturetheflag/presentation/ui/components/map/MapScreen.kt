package com.lamti.capturetheflag.presentation.ui.components.map

import android.app.PendingIntent
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.lamti.capturetheflag.presentation.location.geofences.GeofenceBroadcastReceiver
import com.lamti.capturetheflag.presentation.ui.fragments.maps.GameState
import com.lamti.capturetheflag.presentation.ui.fragments.maps.MapViewModel
import com.lamti.capturetheflag.presentation.ui.fragments.maps.testFlagPosition
import com.lamti.capturetheflag.presentation.ui.fragments.maps.testFlagRadius
import com.lamti.capturetheflag.presentation.ui.fragments.maps.testOpponentFlagPosition
import com.lamti.capturetheflag.presentation.ui.fragments.maps.testSafeHousePosition
import com.lamti.capturetheflag.presentation.ui.fragments.maps.testSafeHouseRadius

@Composable
fun MapScreen(
    lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
    viewModel: MapViewModel = viewModel(),
    geofencePendingIntent: PendingIntent,
) {
    val gameState by viewModel.gameState.collectAsState()
    val (mapProperties, uiSettings) = setupMap(lifecycleOwner, viewModel, geofencePendingIntent)

    when (gameState) {
        is GameState.Started -> GameStartedUI(
            gameState = gameState,
            mapProperties = mapProperties,
            uiSettings = uiSettings
        )
    }
}

@Composable
private fun setupMap(
    lifecycleOwner: LifecycleOwner,
    viewModel: MapViewModel,
    geofencePendingIntent: PendingIntent
): Pair<MapProperties, MapUiSettings> {
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) {
                Log.i(GeofenceBroadcastReceiver.TAG, "On start")
                viewModel.addGeofence(testFlagPosition, "MyFlagGeofence", testFlagRadius)
                viewModel.addGeofence(testOpponentFlagPosition, "OpponentFlagGeofence", testFlagRadius)
                viewModel.addGeofence(testSafeHousePosition, "SafeHouseGeofence", testSafeHouseRadius)
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
        mutableStateOf(MapProperties(isMyLocationEnabled = true))
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
