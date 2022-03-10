package com.lamti.capturetheflag.presentation.ui.components.composables.ar

import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import com.lamti.capturetheflag.presentation.ui.components.composables.DefaultButton

@Composable
fun ArPlacerButtons(
    cancelText: String,
    okText: String,
    showPlacerButtons: Boolean,
    onCancelClicked: () -> Unit,
    onOkClicked: () -> Unit
) {
    if (showPlacerButtons) {
        DefaultButton(text = cancelText) {
            onCancelClicked()
        }
        DefaultButton(
            text = okText,
            color = MaterialTheme.colors.secondary
        ) {
            onOkClicked()
        }
    }
}
