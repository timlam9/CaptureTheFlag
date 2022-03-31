package com.lamti.capturetheflag.presentation.ui.components.composables.ar

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.lamti.capturetheflag.presentation.ui.components.composables.DefaultButton
import com.lamti.capturetheflag.presentation.ui.style.Blue

@Composable
fun ArCaptureFlagButton(
    text: String,
    showCaptureButton: Boolean,
    color: Color = Blue,
    onCaptureClicked: () -> Unit
) {
    if (showCaptureButton) {
        DefaultButton(
            modifier = Modifier.padding(bottom = 40.dp),
            text = text,
            color = color
        ) {
            onCaptureClicked()
        }
    }
}
