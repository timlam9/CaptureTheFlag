package com.lamti.capturetheflag.presentation.ui.components.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.lamti.capturetheflag.R
import com.lamti.capturetheflag.presentation.ui.components.composables.DefaultButton

@Composable
fun ConnectingToGameScreen(
    hasChosenTeam: Boolean,
    onRedButtonClicked: () -> Unit,
    onGreenButtonClicked: () -> Unit
) {
    val text = if (hasChosenTeam) stringResource(R.string.wait_captain) else stringResource(R.string.select_team)

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Text(
            modifier = Modifier.padding(32.dp),
            text = text
        )
        if (!hasChosenTeam) {
            DefaultButton(
                modifier = Modifier
                    .padding(20.dp)
                    .wrapContentHeight(align = Alignment.Bottom)
                    .fillMaxWidth(),
                text = stringResource(R.string.red_team),
                color = Color.Red
            ) {
                onRedButtonClicked()
            }
            DefaultButton(
                modifier = Modifier
                    .padding(20.dp)
                    .wrapContentHeight(align = Alignment.Bottom)
                    .fillMaxWidth(),
                text = stringResource(R.string.green_team),
                color = Color.Green
            ) {
                onGreenButtonClicked()
            }
        }
    }
}
