package com.lamti.capturetheflag.presentation.ui.components.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lamti.capturetheflag.R
import com.lamti.capturetheflag.domain.game.Game
import com.lamti.capturetheflag.domain.player.Team
import com.lamti.capturetheflag.presentation.ui.components.composables.common.DefaultButton
import com.lamti.capturetheflag.presentation.ui.components.composables.common.PositiveAndNegativeAlertDialog
import com.lamti.capturetheflag.presentation.ui.style.Blue
import com.lamti.capturetheflag.presentation.ui.style.Green
import com.lamti.capturetheflag.presentation.ui.style.Red
import com.lamti.capturetheflag.presentation.ui.style.White

@Composable
fun StartingGameScreen(
    game: Game,
    qrCodeImage: ImageBitmap?,
    onStartGameClicked: () -> Unit
) {
    var showConfirmationDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        GameCard(
            gameTitle = game.title,
            greenPlayers = game.greenPlayers.size,
            redPlayers = game.redPlayers.size
        )
        GameContent(
            qrCodeImage = qrCodeImage,
            gameID = game.gameID,
            onStartGameClicked = { showConfirmationDialog = true }
        )
        PositiveAndNegativeAlertDialog(
            title = stringResource(id = R.string.start_game),
            description = stringResource(R.string.start_game_description),
            showDialog = showConfirmationDialog,
            onNegativeDialogClicked = { showConfirmationDialog = false },
            onPositiveButtonClicked = onStartGameClicked
        )
    }
}

@Composable
fun GameContent(
    qrCodeImage: ImageBitmap?,
    gameID: String,
    onStartGameClicked: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 40.dp, bottom = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        val annotatedString = remember(gameID) {
            buildAnnotatedString {
                append("Or share code: ")
                withStyle(
                    style = SpanStyle(
                        color = Blue,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp
                    )
                ) {
                    append(gameID)
                }
            }
        }

        Text(
            text = "Waiting for players...",
            style = MaterialTheme.typography.h5
        )
        if (qrCodeImage != null) {
            Image(
                modifier = Modifier
                    .border(
                        border = BorderStroke(width = 4.dp, color = MaterialTheme.colors.onBackground),
                        shape = RoundedCornerShape(20)
                    )
                    .size(240.dp),
                bitmap = qrCodeImage,
                contentDescription = stringResource(R.string.gr_code)
            )
        }
        SelectionContainer {
            Text(text = annotatedString)
        }
        DefaultButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp),
            text = stringResource(id = R.string.start),
            onclick = onStartGameClicked
        )
    }
}

@Composable
fun GameCard(
    gameTitle: String,
    greenPlayers: Int,
    redPlayers: Int
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        backgroundColor = MaterialTheme.colors.onBackground,
        shape = RoundedCornerShape(bottomStart = 50.dp, bottomEnd = 50.dp),
        elevation = 12.dp
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceAround,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Game: $gameTitle",
                style = MaterialTheme.typography.h4.copy(
                    color = MaterialTheme.colors.background,
                    fontWeight = FontWeight.Bold
                )
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp),
            ) {
                TeamCounter(
                    modifier = Modifier.weight(1f),
                    playersCount = greenPlayers,
                    team = Team.Green
                )
                TeamCounter(
                    modifier = Modifier.weight(1f),
                    playersCount = redPlayers,
                    team = Team.Red
                )
            }
        }
    }
}

@Composable
fun TeamCounter(
    modifier: Modifier,
    playersCount: Int,
    team: Team,
) {
    val color = remember { if (team == Team.Red) Red else Green }
    val paddingStart = remember { if (team == Team.Red) 20.dp else 6.dp }
    val paddingEnd = remember { if (team == Team.Red) 6.dp else 20.dp }
    val shape = remember {
        if (team == Team.Red) RoundedCornerShape(
            topEnd = 40.dp,
            bottomEnd = 40.dp
        )
        else RoundedCornerShape(
            topStart = 40.dp,
            bottomStart = 40.dp
        )
    }

    Row(
        modifier = modifier
            .height(72.dp)
            .clip(shape)
            .background(color = color)
            .padding(start = paddingStart, end = paddingEnd),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (team == Team.Red) {
            TeamName(team.name)
            CircleCounter(counter = playersCount.toString(), color = color)
        } else {
            CircleCounter(counter = playersCount.toString(), color = color)
            TeamName(team.name)
        }
    }
}

@Composable
fun CircleCounter(counter: String, color: Color) {
    Box(
        modifier = Modifier
            .size(62.dp)
            .clip(CircleShape)
            .background(White),
        contentAlignment = Alignment.Center
    ) {
        Text(
            modifier = Modifier,
            text = counter,
            style = MaterialTheme.typography.h5.copy(fontWeight = FontWeight.Bold, color = color)
        )
    }
}

@Composable
fun TeamName(name: String) {
    Text(
        text = name,
        style = MaterialTheme.typography.h5.copy(color = MaterialTheme.colors.background)
    )
}
