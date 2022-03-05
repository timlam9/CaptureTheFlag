package com.lamti.capturetheflag.presentation.ui.components.map

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import com.lamti.capturetheflag.presentation.ui.bitmapDescriptorFromVector
import com.lamti.capturetheflag.presentation.ui.components.DefaultButton
import com.lamti.capturetheflag.presentation.ui.fragments.maps.MapViewModel
import com.lamti.capturetheflag.presentation.ui.style.DarkBlueOpacity
import com.lamti.capturetheflag.presentation.ui.style.GreenOpacity
import com.lamti.capturetheflag.presentation.ui.style.RedOpacity
import com.lamti.capturetheflag.utils.EMPTY
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect

@ExperimentalCoroutinesApi
@Composable
fun GameStartedUI(
    mapProperties: MapProperties,
    uiSettings: MapUiSettings,
    viewModel: MapViewModel,
    onSettingFlagsButtonClicked: () -> Unit
) {
    val instructions: String = when (viewModel.gameState.value.state) {
        ProgressState.Created -> stringResource(R.string.instructions_set_safehouse)
        ProgressState.SettingFlags -> {
            if (
                viewModel.player.value.gameDetails?.team == Team.Red &&
                viewModel.gameState.value.redFlag.isPlaced &&
                !viewModel.gameState.value.greenFlag.isPlaced
            )
                stringResource(R.string.wait_for_green_flag)
            else if (
                viewModel.player.value.gameDetails?.team == Team.Green &&
                viewModel.gameState.value.greenFlag.isPlaced &&
                !viewModel.gameState.value.redFlag.isPlaced
            )
                stringResource(R.string.wait_for_red_flag)
            else
                stringResource(R.string.instructions_set_flags)
        }
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
        if (viewModel.gameState.value.state == ProgressState.SettingFlags) {
            if (viewModel.player.value.gameDetails?.rank == GameDetails.Rank.Captain ||
                viewModel.player.value.gameDetails?.rank == GameDetails.Rank.Leader
            ) {
                val showArButton = when (viewModel.player.value.gameDetails?.team) {
                    Team.Red -> !viewModel.gameState.value.redFlag.isPlaced
                    Team.Green -> !viewModel.gameState.value.greenFlag.isPlaced
                    Team.Unknown -> false
                    null -> false
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
        if (viewModel.gameState.value.state != ProgressState.Started)
            InstructionsCard(instructions)
        if (viewModel.gameState.value.state == ProgressState.Created) {
            DefaultButton(
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
                text = stringResource(id = R.string.ready)
            ) {
                viewModel.onSetFlagsClicked()
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

@ExperimentalCoroutinesApi
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
