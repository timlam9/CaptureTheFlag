package com.lamti.capturetheflag.presentation.ui.components.composables

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun DefaultButton(
    modifier: Modifier = Modifier,
    text: String,
    textColor: Color = Color.White,
    color: Color = MaterialTheme.colors.primaryVariant,
    shape: Shape = MaterialTheme.shapes.small.copy(CornerSize(20)),
    onclick: () -> Unit
) {
    Button(
        onClick = onclick,
        modifier = modifier
            .height(60.dp),
        shape = shape,
        colors = ButtonDefaults.buttonColors(backgroundColor = color)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.button.copy(
                color = textColor,
                fontWeight = FontWeight.Bold
            )
        )
    }
}
