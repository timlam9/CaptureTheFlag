package com.lamti.capturetheflag.presentation.ui.components.composables.ar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.lamti.capturetheflag.presentation.ui.components.composables.InstructionsCard
import com.lamti.capturetheflag.presentation.ui.fragments.ar.ArMode

@Composable
fun ArComponents(
    instructions: String,
    message: String,
    arModeState: ArMode,
    showPlacerButtons: Boolean,
    showGrabButton: Boolean,
    okText: String,
    cancelText: String,
    grabText: String,
    onCancelClicked: ()-> Unit,
    onOkClicked: ()-> Unit,
    onCaptureClicked: ()-> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        TransparentBackgroundCircle()
        Column {
            InstructionsCard(instructions = instructions)
            InstructionsCard(instructions = message)
            Spacer(modifier = Modifier.weight(3.5f))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                if (arModeState == ArMode.Placer) {
                    ArPlacerButtons(
                        cancelText = cancelText,
                        okText = okText,
                        showPlacerButtons = showPlacerButtons,
                        onCancelClicked =  onCancelClicked,
                        onOkClicked = onOkClicked,
                    )
                } else {
                    ArFlagGrabButton(
                        text = grabText,
                        showGrabButton = showGrabButton,
                        onGrabClicked = onCaptureClicked
                    )
                }
            }
        }
    }
}
