package com.lamti.capturetheflag.presentation.ui.components.composables.ar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lamti.capturetheflag.presentation.ui.components.composables.common.InstructionsCard
import com.lamti.capturetheflag.presentation.ui.fragments.ar.ArMode
import com.lamti.capturetheflag.presentation.ui.style.White

@Composable
fun ArComponents(
    instructions: String,
    time: String,
    message: String,
    arModeState: ArMode,
    showPlacerButtons: Boolean,
    showCaptureButton: Boolean,
    okText: String,
    cancelText: String,
    captureText: String,
    teamColor: Color,
    onCancelClicked: () -> Unit,
    onOkClicked: () -> Unit,
    onCaptureClicked: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        TransparentBackgroundCircle()
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Instructions(
                instructions = instructions,
                message = message
            )
            ActionButtons(
                arModeState = arModeState,
                cancelText = cancelText,
                okText = okText,
                teamColor = teamColor,
                showPlacerButtons = showPlacerButtons,
                onCancelClicked = onCancelClicked,
                onOkClicked = onOkClicked,
                captureText = captureText,
                showCaptureButton = showCaptureButton,
                onCaptureClicked = onCaptureClicked
            )
        }
        Text(
            modifier = Modifier
                .padding(75.dp)
                .align(Alignment.BottomCenter),
            text = time,
            style = MaterialTheme.typography.h5.copy(
                color = White,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        )
    }
}

@Composable
private fun Instructions(instructions: String, message: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        Text(
            modifier = Modifier.padding(top = 20.dp, start = 20.dp, end = 20.dp),
            text = instructions,
            style = MaterialTheme.typography.h5.copy(
                color = White,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        )
        InstructionsCard(text = message)
    }
}

@Composable
private fun ActionButtons(
    arModeState: ArMode,
    cancelText: String,
    okText: String,
    teamColor: Color,
    showPlacerButtons: Boolean,
    onCancelClicked: () -> Unit,
    onOkClicked: () -> Unit,
    captureText: String,
    showCaptureButton: Boolean,
    onCaptureClicked: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        if (arModeState == ArMode.Placer) {
            ArPlacerButtons(
                cancelText = cancelText,
                okText = okText,
                color = teamColor,
                showPlacerButtons = showPlacerButtons,
                onCancelClicked = onCancelClicked,
                onOkClicked = onOkClicked,
            )
        } else {
            ArCaptureFlagButton(
                text = captureText,
                color = teamColor,
                showCaptureButton = showCaptureButton,
                onCaptureClicked = onCaptureClicked
            )
        }
    }
}
