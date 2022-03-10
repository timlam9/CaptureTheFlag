package com.lamti.capturetheflag.presentation.ui.components.composables

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun InstructionsCard(instructions: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .clickable { },
        elevation = 10.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(text = instructions)
        }
    }
}
