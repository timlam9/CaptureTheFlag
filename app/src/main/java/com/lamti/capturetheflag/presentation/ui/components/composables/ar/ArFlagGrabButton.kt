package com.lamti.capturetheflag.presentation.ui.components.composables.ar

import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import com.lamti.capturetheflag.presentation.ui.components.composables.DefaultButton

@Composable
fun ArFlagGrabButton(
    text: String,
    showGrabButton: Boolean,
    onGrabClicked: () -> Unit
) {
    if (showGrabButton) {
        DefaultButton(
            text = text,
            color = MaterialTheme.colors.secondary
        ) {
            onGrabClicked()
        }
    }
}
