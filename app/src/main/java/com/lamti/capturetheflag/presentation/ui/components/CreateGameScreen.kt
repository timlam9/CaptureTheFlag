package com.lamti.capturetheflag.presentation.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.lamti.capturetheflag.R
import com.lamti.capturetheflag.domain.game.ProgressState
import com.lamti.capturetheflag.presentation.ui.fragments.maps.MapViewModel
import com.lamti.capturetheflag.utils.EMPTY
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
@Composable
fun CreateGameScreen(viewModel: MapViewModel, onSetGameClicked: () -> Unit) {
    var gameName by remember { mutableStateOf(EMPTY) }
    var isGameCreated by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf(EMPTY) }
    var buttonName by remember { mutableStateOf(EMPTY) }
    val icon = BitmapFactory.decodeResource(LocalContext.current.resources, R.drawable.ic_logo)

    when (viewModel.gameState.value.state) {
        ProgressState.Created -> {
            isGameCreated = true
            title = stringResource(R.string.game_created)
            buttonName = stringResource(R.string.set_game)
        }
        else -> {
            title = stringResource(R.string.create_game)
            buttonName = stringResource(R.string.create_game)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        Spacer(modifier = Modifier.weight(0.25f))
        Text(
            text = stringResource(R.string.capture_the_flag),
            style = MaterialTheme.typography.h4.copy(
                color = MaterialTheme.colors.primaryVariant,
                fontWeight = FontWeight.Bold
            )
        )
        Text(
            text = title,
            style = MaterialTheme.typography.h5.copy(color = MaterialTheme.colors.onBackground)
        )
        Spacer(modifier = Modifier.weight(0.1f))
        if (isGameCreated) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .weight(3f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Image(
                    modifier = Modifier.size(200.dp),
                    bitmap = viewModel.generateQrCode(viewModel.player.value.gameDetails?.gameID ?: EMPTY)?.asImageBitmap()
                        ?: icon.asImageBitmap(),
                    contentDescription = stringResource(R.string.gr_code)
                )
                Text(text = "Or share code: ${viewModel.player.value.gameDetails?.gameID}")
            }
            Spacer(modifier = Modifier.weight(0.2f))
        } else {
            InfoTextField(
                text = gameName,
                label = stringResource(id = R.string.type_title),
                keyboardOptions = KeyboardOptions.Default.copy(
                    imeAction = ImeAction.Done,
                    keyboardType = KeyboardType.Text,
                    capitalization = KeyboardCapitalization.Words
                )
            ) {
                gameName = it
            }
            Spacer(modifier = Modifier.weight(3f))
        }
        DefaultButton(
            modifier = Modifier.padding(20.dp).fillMaxWidth(),
            text = buttonName,
            color = MaterialTheme.colors.primaryVariant
        ) {
            when {
                isGameCreated -> {
                    viewModel.onSetGameClicked()
                    onSetGameClicked()
                }
                else ->
                    viewModel.onCreateGameClicked(gameName)
            }
        }
    }
}
