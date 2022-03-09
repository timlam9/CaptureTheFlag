package com.lamti.capturetheflag.presentation.ui.components.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun ConnectingToGameScreen(
    hasChosenTeam: Boolean,
    onRedButtonClicked: () -> Unit,
    onGreenButtonClicked: () -> Unit
) {
    val text = if (hasChosenTeam) "Wait for the captain to start the game" else "Select your team"

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Text(
            modifier = Modifier.weight(1f),
            text = text
        )
        DefaultButton(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            text = "Red Team",
            color = Color.Red
        ) {
            onRedButtonClicked()
        }
        DefaultButton(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            text = "Green Team",
            color = Color.Green
        ) {
            onGreenButtonClicked()
        }
    }
}
