package com.lamti.capturetheflag.presentation.ui.components.composables.ar

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lamti.capturetheflag.presentation.ui.components.composables.DefaultButton
import com.lamti.capturetheflag.presentation.ui.style.Blue
import com.lamti.capturetheflag.presentation.ui.style.White

@Composable
fun ArPlacerButtons(
    cancelText: String,
    okText: String,
    showPlacerButtons: Boolean,
    color: Color = Blue,
    onCancelClicked: () -> Unit,
    onOkClicked: () -> Unit
) {
    if (showPlacerButtons) {
        DefaultButton(
            modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
            text = okText,
            color = color,
            onclick = onOkClicked
        )
        Text(
            modifier = Modifier.clickable { onCancelClicked() },
            text = cancelText,
            style = MaterialTheme.typography.h6.copy(
                color = White,
                fontWeight = FontWeight.Normal
            )
        )
    }
}
