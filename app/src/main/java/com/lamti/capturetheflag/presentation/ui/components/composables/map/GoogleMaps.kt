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
import com.lamti.capturetheflag.presentation.ui.DEFAULT_FLAG_RADIUS
import com.lamti.capturetheflag.presentation.ui.DEFAULT_GAME_RADIUS
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
    flagRadius: Float,
    gameRadius: Float,
    gameState: ProgressState,
    gameDetails: GameDetails,
    redFlag: GeofenceObject,
    greenFlag: GeofenceObject,
    redFlagPlayer: String?,
    greenFlagPlayer: String?,
    otherPlayers: List<GamePlayer>,
    onReadyButtonClicked: (LatLng, Float, Float) -> Unit,
) {
    val (mapProperties, uiSettings) = setupMap()

    GoogleMapsView(
        safehousePosition = safehousePosition,
        mapProperties = mapProperties,
        uiSettings = uiSettings,
        cameraPositionState = cameraPositionState,
        isSafeHouseDraggable = isSafeHouseDraggable,
        onReadyButtonClicked = onReadyButtonClicked,
        team = team,
        flagRadius = flagRadius,
        gameRadius = gameRadius,
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
    onReadyButtonClicked: (LatLng, Float, Float) -> Unit,
    team: Team,
    flagRadius: Float,
    gameRadius: Float,
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

    var initialGameRadius: Float by remember {
        mutableStateOf(
            if (gameState == ProgressState.Created && gameDetails.rank == GameDetails.Rank.Captain) gameRadius else DEFAULT_GAME_RADIUS
        )
    }
    var initialFlagRadius: Float by remember {
        mutableStateOf(
            if (gameState == ProgressState.Created && gameDetails.rank == GameDetails.Rank.Captain) flagRadius else DEFAULT_FLAG_RADIUS
        )
    }

    LaunchedEffect(key1 = gameRadius) { initialGameRadius = gameRadius }
    LaunchedEffect(key1 = flagRadius) { initialFlagRadius = flagRadius }

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
                safeHouseRadius = initialFlagRadius,
                markerState = safehouseMarkerState,
                gameRadius = initialGameRadius,
                isSafeHouseDraggable = isSafeHouseDraggable
            )
            FlagMarkers(
                team = team,
                flagRadius = initialFlagRadius,
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
        GameConfiguration(
            modifier = Modifier
                .padding(20.dp)
                .align(Alignment.BottomCenter),
            gameState = gameState,
            playerRank = gameDetails.rank,
            onFlagRadiusValueChange = { initialFlagRadius = it },
            onGameRadiusValueChange = { initialGameRadius = it },
            onReadyButtonClicked = { showConfirmationDialog = true }
        )
        ConfirmationDialog(
            title = stringResource(id = R.string.start_game),
            description = stringResource(R.string.start_game_description),
            showDialog = showConfirmationDialog,
            onNegativeDialogClicked = { showConfirmationDialog = false },
            onPositiveButtonClicked = {
                showConfirmationDialog = false
                onReadyButtonClicked(safehouseMarkerState.position, initialGameRadius, initialFlagRadius)
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
