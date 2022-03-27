package com.lamti.capturetheflag.presentation.ui.components.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.lamti.capturetheflag.R
import com.lamti.capturetheflag.presentation.ui.components.composables.DefaultButton
import com.lamti.capturetheflag.presentation.ui.components.composables.InfoTextField
import com.lamti.capturetheflag.utils.EMPTY

@Composable
fun CreateGameScreen(onCreateGameClicked: (String) -> Unit) {
    var gameTitle by remember { mutableStateOf(EMPTY) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = stringResource(R.string.create_game),
            style = MaterialTheme.typography.h4.copy(fontWeight = FontWeight.Bold)
        )
        InfoTextField(
            modifier = Modifier.padding(top = 20.dp),
            text = gameTitle,
            label = stringResource(id = R.string.type_title),
            keyboardOptions = KeyboardOptions.Default.copy(
                imeAction = ImeAction.Done,
                keyboardType = KeyboardType.Text,
                capitalization = KeyboardCapitalization.Words
            )
        ) {
            gameTitle = it
        }
        Image(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            painter = painterResource(id = R.drawable.rocket),
            contentDescription = "create game image"
        )
        DefaultButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp),
            text = stringResource(id = R.string.create),
            color = MaterialTheme.colors.onBackground
        ) {
            if (gameTitle.isEmpty()) return@DefaultButton
            onCreateGameClicked(gameTitle)
        }
    }
}
