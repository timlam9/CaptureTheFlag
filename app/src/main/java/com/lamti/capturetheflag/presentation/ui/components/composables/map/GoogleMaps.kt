package com.lamti.capturetheflag.presentation.ui.components.composables.map

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.rememberCameraPositionState
import com.lamti.capturetheflag.R
import com.lamti.capturetheflag.domain.game.GamePlayer
import com.lamti.capturetheflag.domain.game.GeofenceObject
import com.lamti.capturetheflag.domain.player.Team
import com.lamti.capturetheflag.presentation.ui.MapStyle
import com.lamti.capturetheflag.presentation.ui.bitmapDescriptorFromVector

@Composable
fun GoogleMapsView(
    currentPosition: LatLng,
    safehousePosition: LatLng,
    isSafeHouseDraggable: Boolean,
    team: Team,
    userID: String,
    redFlag: GeofenceObject,
    greenFlag: GeofenceObject,
    otherPlayers: List<GamePlayer>,
    onSafehouseMarkerClicked: (LatLng) -> Unit,
) {
    val (mapProperties, uiSettings) = setupMap()

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(currentPosition, 15f)
    }

    LaunchedEffect(currentPosition) {
        snapshotFlow { currentPosition }.collect {
            cameraPositionState.position = CameraPosition.fromLatLngZoom(currentPosition, 15f)
        }
    }

    GoogleMapsView(
        mapProperties = mapProperties,
        uiSettings = uiSettings,
        cameraPositionState = cameraPositionState,
        safehousePosition = safehousePosition,
        isSafeHouseDraggable = isSafeHouseDraggable,
        onSafehouseMarkerClicked = onSafehouseMarkerClicked,
        team = team,
        userID = userID,
        redFlag = redFlag,
        greenFlag = greenFlag,
        otherPlayers = otherPlayers
    )
}

@Composable
fun GoogleMapsView(
    modifier: Modifier = Modifier,
    mapProperties: MapProperties,
    uiSettings: MapUiSettings,
    greenFlagMarkerTitle: String = stringResource(R.string.green_flag),
    redFlagMarkerTitle: String = stringResource(R.string.red_flag),
    safeHouseTitle: String = stringResource(R.string.safehouse),
    cameraPositionState: CameraPositionState,
    safehousePosition: LatLng,
    isSafeHouseDraggable: Boolean,
    onSafehouseMarkerClicked: (LatLng) -> Unit,
    team: Team,
    userID: String,
    redFlag: GeofenceObject,
    greenFlag: GeofenceObject,
    otherPlayers: List<GamePlayer>
) {
    val context = LocalContext.current
    val greenPersonPin = remember { context.bitmapDescriptorFromVector(R.drawable.ic_person_pin, R.color.green) }
    val redPersonPin = remember { context.bitmapDescriptorFromVector(R.drawable.ic_person_pin, R.color.red) }
    val greenFlagIcon = remember { context.bitmapDescriptorFromVector(R.drawable.ic_flag, R.color.green) }
    val redFlagIcon = remember { context.bitmapDescriptorFromVector(R.drawable.ic_flag, R.color.red) }
    val safeHouseIcon = remember { context.bitmapDescriptorFromVector(R.drawable.ic_safety, R.color.blue) }

    GoogleMap(
        modifier = modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState,
        properties = mapProperties,
        uiSettings = uiSettings,
    ) {
        GameBoundariesGeofence(
            safehousePosition = safehousePosition,
            safeHouseIcon = safeHouseIcon,
            safeHouseTitle = safeHouseTitle,
            isSafeHouseDraggable = isSafeHouseDraggable
        ) {
            if (isSafeHouseDraggable) {
                onSafehouseMarkerClicked(it)
            }
        }
        FlagMarkers(
            team = team,
            redFlag = redFlag,
            greenFlag = greenFlag,
            redFlagIcon = redFlagIcon,
            redFlagMarkerTitle = redFlagMarkerTitle,
            greenFlagIcon = greenFlagIcon,
            greenFlagMarkerTitle = greenFlagMarkerTitle
        )
        PlayerMarkers(
            otherPlayers = otherPlayers,
            greenPersonPin = greenPersonPin,
            redPersonPin = redPersonPin,
            userID = userID
        )
    }
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
