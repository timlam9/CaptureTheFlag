package com.lamti.capturetheflag.presentation.ui.components.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateOffset
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import com.lamti.capturetheflag.R
import com.lamti.capturetheflag.domain.player.Team
import com.lamti.capturetheflag.presentation.ui.DatastoreHelper
import com.lamti.capturetheflag.presentation.ui.components.composables.common.ConfirmationDialog
import com.lamti.capturetheflag.presentation.ui.components.composables.common.DefaultButton
import com.lamti.capturetheflag.presentation.ui.components.composables.common.LoadingAnimation
import com.lamti.capturetheflag.presentation.ui.style.Black
import com.lamti.capturetheflag.presentation.ui.style.Green
import com.lamti.capturetheflag.presentation.ui.style.Red

@Composable
fun ChooseTeamScreen(
    dataStore: DatastoreHelper,
    onRedButtonClicked: () -> Unit,
    onGreenButtonClicked: () -> Unit,
    onOkButtonClicked: () -> Unit,
) {
    var hasChosenTeam by rememberSaveable { mutableStateOf(false) }
    var selectedTeam by rememberSaveable { mutableStateOf(Team.Unknown) }
    var showConfirmationDialog by rememberSaveable { mutableStateOf(false) }

    val lifecycleState: Lifecycle.State = LocalLifecycleOwner.current.lifecycle.currentState
    val isResumed: Boolean = lifecycleState.isAtLeast(Lifecycle.State.RESUMED)

    LaunchedEffect(key1 = lifecycleState) {
        if(isResumed) dataStore.saveHasGameFound(false)
    }

    when (hasChosenTeam) {
        true -> WaitingContent(selectedTeam = selectedTeam)
        false -> {
            SelectTeamContent(
                selectedTeam = selectedTeam,
                onRedButtonClicked = {
                    selectedTeam = Team.Red
                    onRedButtonClicked()
                },
                onGreenButtonClicked = {
                    selectedTeam = Team.Green
                    onGreenButtonClicked()
                },
                onOkButtonClicked = { showConfirmationDialog = true }
            )
            ConfirmationDialog(
                title = stringResource(id = R.string.join_game),
                description = stringResource(R.string.join_game_description),
                showDialog = showConfirmationDialog,
                onNegativeDialogClicked = { showConfirmationDialog = false },
                onPositiveButtonClicked = {
                    hasChosenTeam = true
                    showConfirmationDialog = false
                    onOkButtonClicked()
                }
            )
        }
    }
}

@Composable
fun WaitingContent(selectedTeam: Team) {
    val color: Color by remember(selectedTeam) {
        mutableStateOf(
            when (selectedTeam) {
                Team.Red -> Red
                Team.Green -> Green
                Team.Unknown -> Black
            }
        )
    }
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceAround
    ) {
        Text(
            text = stringResource(id = R.string.wait_captain),
            style = MaterialTheme.typography.h4.copy(fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        )
        LoadingAnimation(animatedCircleColor = color)
    }
}

@Composable
private fun SelectTeamContent(
    selectedTeam: Team,
    onRedButtonClicked: () -> Unit,
    onGreenButtonClicked: () -> Unit,
    onOkButtonClicked: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Header(
            text = stringResource(R.string.select_team),
            onRedButtonClicked = onRedButtonClicked,
            onGreenButtonClicked = onGreenButtonClicked
        )
        TeamSelectionImage(team = selectedTeam)
        DefaultButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            text = stringResource(R.string.ok),
            onclick = {
                if (selectedTeam == Team.Unknown) return@DefaultButton
                onOkButtonClicked()
            }
        )
    }
}

@Composable
private fun Header(
    text: String,
    onRedButtonClicked: () -> Unit,
    onGreenButtonClicked: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            modifier = Modifier.padding(30.dp),
            text = text,
            style = MaterialTheme.typography.h4.copy(fontWeight = FontWeight.Bold)
        )
        ButtonsRow(
            onRedButtonClicked = onRedButtonClicked,
            onGreenButtonClicked = onGreenButtonClicked
        )
    }
}

@Composable
private fun ButtonsRow(onRedButtonClicked: () -> Unit, onGreenButtonClicked: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        DefaultButton(
            modifier = Modifier.width(130.dp),
            text = stringResource(R.string.green),
            color = Green,
            onclick = onGreenButtonClicked
        )
        DefaultButton(
            modifier = Modifier.width(130.dp),
            text = stringResource(R.string.red),
            color = Red,
            onclick = onRedButtonClicked
        )
    }
}

@Composable
private fun TeamSelectionImage(team: Team) {
    val transition = updateTransition(targetState = team, label = "image transition")
    val imageOffset by transition.animateOffset(label = "image offset") { selectedTeam ->
        when (selectedTeam) {
            Team.Red -> Offset(100f, 0f)
            Team.Green -> Offset(-100f, 0f)
            Team.Unknown -> Offset.Zero
        }
    }
    val animatedColor: Color by animateColorAsState(
        when (team) {
            Team.Red -> Red
            Team.Green -> Green
            Team.Unknown -> Black
        }
    )
    val teamName: String by remember(team) {
        mutableStateOf(
            when (team) {
                Team.Red -> team.name
                Team.Green -> team.name
                Team.Unknown -> "No"
            }
        )
    }
    val annotatedString =
        buildAnnotatedString {
            withStyle(
                style = SpanStyle(
                    color = animatedColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp
                )
            ) {
                append(teamName)
            }
            append(" team selected")
        }


    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 20.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Image(
            modifier = Modifier
                .fillMaxWidth()
                .scale(2f)
                .offset(imageOffset.x.dp, imageOffset.y.dp),
            painter = painterResource(id = R.drawable.team_selection_image),
            contentDescription = "team selection image"
        )
        Text(
            modifier = Modifier.offset(y = (-30).dp),
            text = annotatedString,
            style = MaterialTheme.typography.h5
        )
    }
}
