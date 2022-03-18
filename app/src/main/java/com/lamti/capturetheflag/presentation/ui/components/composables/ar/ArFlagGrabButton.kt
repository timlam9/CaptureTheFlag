package com.lamti.capturetheflag.presentation.ui.components.composables.ar

import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import com.lamti.capturetheflag.presentation.ui.components.composables.DefaultButton

@Composable
fun ArCaptureFlagButton(
    text: String,
    showCaptureButton: Boolean,
    onCaptureClicked: () -> Unit
) {
    if (showCaptureButton) {
        DefaultButton(
            text = text,
            color = MaterialTheme.colors.secondary
        ) {
            onCaptureClicked()
        }
    }
}
