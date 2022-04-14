package com.lamti.capturetheflag.presentation.ui.components.composables.map

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.rememberMarkerState
import com.lamti.capturetheflag.R
import com.lamti.capturetheflag.domain.game.GamePlayer
import com.lamti.capturetheflag.domain.game.GeofenceObject
import com.lamti.capturetheflag.domain.game.ProgressState
import com.lamti.capturetheflag.domain.player.GameDetails
import com.lamti.capturetheflag.domain.player.Team
import com.lamti.capturetheflag.presentation.ui.MapStyle
import com.lamti.capturetheflag.presentation.ui.bitmapDescriptorFromVector
import com.lamti.capturetheflag.presentation.ui.components.composables.common.ConfirmationDialog

@Composable
fun GoogleMapsView(
    cameraPositionState: CameraPositionState,
    isSafeHouseDraggable: Boolean,
    safehousePosition: LatLng,
    team: Team,
    userID: String,
    gameState: ProgressState,
    gameDetails: GameDetails,
    redFlag: GeofenceObject,
    greenFlag: GeofenceObject,
    redFlagPlayer: String?,
    greenFlagPlayer: String?,
    otherPlayers: List<GamePlayer>,
    onReadyButtonClicked: (LatLng) -> Unit,
) {
    val (mapProperties, uiSettings) = setupMap()

    GoogleMapsView(
        mapProperties = mapProperties,
        uiSettings = uiSettings,
        safehousePosition = safehousePosition,
        cameraPositionState = cameraPositionState,
        isSafeHouseDraggable = isSafeHouseDraggable,
        onReadyButtonClicked = onReadyButtonClicked,
        team = team,
        gameState = gameState,
        gameDetails = gameDetails,
        userID = userID,
        redFlag = redFlag,
        greenFlag = greenFlag,
        redFlagPlayer = redFlagPlayer,
        greenFlagPlayer = greenFlagPlayer,
        otherPlayers = otherPlayers
    )
}

@Composable
fun GoogleMapsView(
    modifier: Modifier = Modifier,
    safehousePosition: LatLng,
    mapProperties: MapProperties,
    uiSettings: MapUiSettings,
    greenFlagMarkerTitle: String = stringResource(R.string.green_flag),
    redFlagMarkerTitle: String = stringResource(R.string.red_flag),
    safeHouseTitle: String = stringResource(R.string.safehouse),
    cameraPositionState: CameraPositionState,
    isSafeHouseDraggable: Boolean,
    onReadyButtonClicked: (LatLng) -> Unit,
    team: Team,
    gameState: ProgressState,
    gameDetails: GameDetails,
    userID: String,
    redFlag: GeofenceObject,
    greenFlag: GeofenceObject,
    redFlagPlayer: String?,
    greenFlagPlayer: String?,
    otherPlayers: List<GamePlayer>
) {
    val context = LocalContext.current
    val greenPersonPin = remember { context.bitmapDescriptorFromVector(R.drawable.ic_person_pin, R.color.green) }
    val redPersonPin = remember { context.bitmapDescriptorFromVector(R.drawable.ic_person_pin, R.color.red) }
    val greenFlagIcon = remember { context.bitmapDescriptorFromVector(R.drawable.ic_flag, R.color.green) }
    val redFlagIcon = remember { context.bitmapDescriptorFromVector(R.drawable.ic_flag, R.color.red) }
    val safeHouseIcon = remember { context.bitmapDescriptorFromVector(R.drawable.ic_safety, R.color.blue) }

    var showConfirmationDialog by remember { mutableStateOf(false) }
    val safehouseMarkerState = rememberMarkerState(position = safehousePosition)
    LaunchedEffect(key1 = safehousePosition) {
        safehouseMarkerState.position = safehousePosition
    }

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = mapProperties,
            uiSettings = uiSettings
        ) {
            GameBoundariesGeofence(
                safeHouseIcon = safeHouseIcon,
                safeHouseTitle = safeHouseTitle,
                markerState = safehouseMarkerState,
                isSafeHouseDraggable = isSafeHouseDraggable
            )
            FlagMarkers(
                team = team,
                redFlag = redFlag,
                greenFlag = greenFlag,
                redFlagIcon = redFlagIcon,
                redFlagMarkerTitle = redFlagMarkerTitle,
                greenFlagIcon = greenFlagIcon,
                greenFlagMarkerTitle = greenFlagMarkerTitle,
                redFlagPlayer = redFlagPlayer,
                greenFlagPlayer = greenFlagPlayer
            )
            PlayerMarkers(
                otherPlayers = otherPlayers,
                greenPersonPin = greenPersonPin,
                redPersonPin = redPersonPin,
                greenFlagIcon = greenFlagIcon,
                redFlagIcon = redFlagIcon,
                userID = userID,
                userTeam = team,
                redFlagPlayer = redFlagPlayer,
                greenFlagPlayer = greenFlagPlayer
            )
        }
        ReadyButton(
            modifier = Modifier
                .padding(20.dp)
                .align(Alignment.BottomCenter),
            gameState = gameState,
            playerGameDetails = gameDetails,
            onReadyButtonClicked = { showConfirmationDialog = true }
        )
        ConfirmationDialog(
            title = stringResource(id = R.string.start_game),
            description = stringResource(R.string.start_game_description),
            showDialog = showConfirmationDialog,
            onNegativeDialogClicked = { showConfirmationDialog = false },
            onPositiveButtonClicked = {
                showConfirmationDialog = false
                onReadyButtonClicked(safehouseMarkerState.position)
            }
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
                mapToolbarEnabled = false,
                compassEnabled = false
            )
        )
    }

    return Pair(mapProperties, uiSettings)
}
