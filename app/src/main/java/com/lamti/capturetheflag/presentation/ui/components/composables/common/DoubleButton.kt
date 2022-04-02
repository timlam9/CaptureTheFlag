package com.lamti.capturetheflag.presentation.ui.components.composables.common

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Composable
fun DoubleButton(
    modifier: Modifier = Modifier,
    startButtonColor: Color = MaterialTheme.colors.primary,
    endButtonColor: Color = MaterialTheme.colors.surface,
    startButtonTextColor: Color = MaterialTheme.colors.background,
    endButtonTextColor: Color = MaterialTheme.colors.onSurface,
    startButtonText: String,
    endButtonText: String,
    onStartButtonClicked: () -> Unit,
    onEndButtonClicked: () -> Unit,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DefaultButton(
            modifier = Modifier.weight(1f),
            text = startButtonText,
            textColor = startButtonTextColor,
            color = startButtonColor,
            cornerSize = CornerSize(20),
            shape = RoundedCornerShape(topStart = 20f, topEnd = 0f, bottomEnd = 0f, bottomStart = 20f),
            onclick = onStartButtonClicked
        )
        DefaultButton(
            modifier = Modifier.weight(1f),
            text = endButtonText,
            textColor = endButtonTextColor,
            color = endButtonColor,
            cornerSize = CornerSize(20),
            shape = RoundedCornerShape(topStart = 0f, topEnd = 20f, bottomEnd = 20f, bottomStart = 0f),
            onclick = onEndButtonClicked
        )
    }
}
