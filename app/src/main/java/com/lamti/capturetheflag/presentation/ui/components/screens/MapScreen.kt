package com.lamti.capturetheflag.presentation.ui.components.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Card
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerDragState
import com.lamti.capturetheflag.R
import com.lamti.capturetheflag.domain.game.GeofenceObject
import com.lamti.capturetheflag.domain.game.ProgressState
import com.lamti.capturetheflag.domain.player.GameDetails
import com.lamti.capturetheflag.domain.player.Team
import com.lamti.capturetheflag.presentation.ui.DEFAULT_GAME_BOUNDARIES_RADIUS
import com.lamti.capturetheflag.presentation.ui.DEFAULT_SAFEHOUSE_RADIUS
import com.lamti.capturetheflag.presentation.ui.MapStyle
import com.lamti.capturetheflag.presentation.ui.bitmapDescriptorFromVector
import com.lamti.capturetheflag.presentation.ui.components.composables.MapMarker
import com.lamti.capturetheflag.presentation.ui.fragments.maps.MapViewModel
import com.lamti.capturetheflag.presentation.ui.style.DarkBlueOpacity
import com.lamti.capturetheflag.presentation.ui.style.GreenOpacity
import com.lamti.capturetheflag.presentation.ui.style.RedOpacity
import com.lamti.capturetheflag.utils.EMPTY

@Composable
fun MapScreen(
    viewModel: MapViewModel,
    enteredGeofenceId: String,
    onSettingFlagsButtonClicked: () -> Unit,
    onArScannerButtonClicked: () -> Unit,
    onQuitButtonClicked: () -> Unit
) {
    val (mapProperties, uiSettings) = setupMap()

    MapScreen(
        mapProperties = mapProperties,
        uiSettings = uiSettings,
        viewModel = viewModel,
        enteredGeofenceId = enteredGeofenceId,
        onArScannerButtonClicked = onArScannerButtonClicked,
        onSettingFlagsButtonClicked = onSettingFlagsButtonClicked,
        onQuitButtonClicked = onQuitButtonClicked,
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

@Composable
fun MapScreen(
    mapProperties: MapProperties,
    uiSettings: MapUiSettings,
    viewModel: MapViewModel,
    enteredGeofenceId: String,
    onArScannerButtonClicked: () -> Unit,
    onSettingFlagsButtonClicked: () -> Unit,
    onQuitButtonClicked: () -> Unit
) {
    val gameState = viewModel.gameState.value.state
    val playerGameDetails = viewModel.player.value.gameDetails

    val instructions: String = when (gameState) {
        ProgressState.Created -> {
            if (playerGameDetails?.rank == GameDetails.Rank.Captain) {
                stringResource(R.string.instructions_set_safehouse)
            } else {
                stringResource(R.string.wait_the_captain)
            }
        }
        ProgressState.SettingFlags -> {
            if (
                playerGameDetails?.team == Team.Red &&
                viewModel.gameState.value.redFlag.isPlaced &&
                !viewModel.gameState.value.greenFlag.isPlaced
            )
                stringResource(R.string.wait_for_green_flag)
            else if (
                playerGameDetails?.team == Team.Green &&
                viewModel.gameState.value.greenFlag.isPlaced &&
                !viewModel.gameState.value.redFlag.isPlaced
            )
                stringResource(R.string.wait_for_red_flag)
            else
                stringResource(R.string.instructions_set_flags)
        }
        ProgressState.Ended -> stringResource(id = R.string.game_over)
        else -> EMPTY
    }

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize()
    ) {
        GoogleMapsView(
            mapProperties = mapProperties,
            uiSettings = uiSettings,
            viewModel = viewModel
        )
        if (gameState == ProgressState.SettingFlags) {
            if (playerGameDetails?.rank == GameDetails.Rank.Captain ||
                playerGameDetails?.rank == GameDetails.Rank.Leader
            ) {
                val showArButton = when (playerGameDetails.team) {
                    Team.Red -> !viewModel.gameState.value.redFlag.isPlaced
                    Team.Green -> !viewModel.gameState.value.greenFlag.isPlaced
                    Team.Unknown -> false
                }
                if (showArButton) {
                    FloatingActionButton(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 64.dp),
                        onClick = onSettingFlagsButtonClicked,
                        backgroundColor = MaterialTheme.colors.secondary,
                        contentColor = Color.White
                    ) {
                        Icon(painterResource(id = R.drawable.ic_flag), EMPTY)
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(color = DarkBlueOpacity)
                            .fillMaxSize()
                    )
                }
            }
        }
        if (gameState != ProgressState.Started)
            InstructionsCard(instructions)
        if (gameState == ProgressState.Created && playerGameDetails?.rank == GameDetails.Rank.Captain) {
            DefaultButton(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
                text = stringResource(id = R.string.ready)
            ) {
                viewModel.onSetFlagsClicked()
            }
        }

        if (playerGameDetails?.team == Team.Red && enteredGeofenceId.contains("Green") ||
            playerGameDetails?.team == Team.Green && enteredGeofenceId.contains("Red")
        ) {
            FloatingActionButton(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 64.dp),
                onClick = onArScannerButtonClicked,
                backgroundColor = MaterialTheme.colors.secondary,
                contentColor = Color.White
            ) {
                Icon(painterResource(id = R.drawable.ic_flag), EMPTY)
            }
        }
        if(gameState == ProgressState.Ended) {
            DefaultButton(text = stringResource(id = R.string.quit_game)) {
                viewModel.onQuitButtonClicked {
                    if (it) onQuitButtonClicked()
                }
            }
        }
    }
}

@Composable
fun InstructionsCard(instructions: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .clickable { },
        elevation = 10.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(text = instructions)
        }
    }
}

@Composable
fun GoogleMapsView(
    modifier: Modifier = Modifier,
    mapProperties: MapProperties,
    uiSettings: MapUiSettings,
    viewModel: MapViewModel,
    greenFlagMarkerTitle: String = "Green Flag",
    redFlagMarkerTitle: String = "Red Flag",
    safeHouseTitle: String = "Safe House"
) {
    val currentPosition by viewModel.currentPosition
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(currentPosition, 15f)
    }

    LaunchedEffect(viewModel) {
        snapshotFlow { currentPosition }.collect {
            cameraPositionState.position = CameraPosition.fromLatLngZoom(currentPosition, 15f)
        }
    }

    val context = LocalContext.current
    val greenFlagIcon = context.bitmapDescriptorFromVector(R.drawable.ic_flag, R.color.green)
    val redFlagIcon = context.bitmapDescriptorFromVector(R.drawable.ic_flag, R.color.red)
    val safeHouseIcon = context.bitmapDescriptorFromVector(R.drawable.ic_safety, R.color.blue)

    GoogleMap(
        modifier = modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState,
        properties = mapProperties,
        uiSettings = uiSettings,
    ) {
        GameBoundariesGeofence(
            safehousePosition = viewModel.gameState.value.safehouse.position,
            safeHouseIcon = safeHouseIcon,
            safeHouseTitle = safeHouseTitle,
            isSafeHouseDraggable = viewModel.isSafehouseDraggable.value
        ) {
            if (viewModel.isSafehouseDraggable.value) {
                viewModel.updateSafeHousePosition(it)
            }
        }
        ShowFlags(
            team = viewModel.player.value.gameDetails?.team ?: Team.Unknown,
            redFlag = viewModel.gameState.value.redFlag,
            greenFlag = viewModel.gameState.value.greenFlag,
            redFlagIcon = redFlagIcon,
            redFlagMarkerTitle = redFlagMarkerTitle,
            greenFlagIcon = greenFlagIcon,
            greenFlagMarkerTitle = greenFlagMarkerTitle
        )
    }
}

@Composable
private fun GameBoundariesGeofence(
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

@Composable
private fun ShowFlags(
    team: Team,
    redFlag: GeofenceObject,
    greenFlag: GeofenceObject,
    redFlagIcon: BitmapDescriptor?,
    redFlagMarkerTitle: String,
    greenFlagIcon: BitmapDescriptor?,
    greenFlagMarkerTitle: String
) {
    if (team == Team.Red || redFlag.isDiscovered) {
        MapMarker(
            position = redFlag.position,
            icon = redFlagIcon,
            title = redFlagMarkerTitle,
            hasGeofence = true,
            fillColor = RedOpacity,
            strokeColor = Color.Red,
        )
    }
    if (team == Team.Green || greenFlag.isDiscovered) {
        MapMarker(
            position = greenFlag.position,
            icon = greenFlagIcon,
            title = greenFlagMarkerTitle,
            hasGeofence = true,
            fillColor = GreenOpacity,
            strokeColor = Color.Green,
        )
    }
}
