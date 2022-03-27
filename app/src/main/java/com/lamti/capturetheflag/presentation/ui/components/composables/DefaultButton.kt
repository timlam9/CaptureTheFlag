package com.lamti.capturetheflag.presentation.ui.components.composables

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lamti.capturetheflag.presentation.ui.style.White

@Composable
fun DefaultButton(
    modifier: Modifier = Modifier,
    text: String,
    fontSize: TextUnit = 20.sp,
    textColor: Color = White,
    color: Color = MaterialTheme.colors.primary,
    shape: Shape = MaterialTheme.shapes.small.copy(CornerSize(60)),
    onclick: () -> Unit
) {
    Button(
        onClick = onclick,
        modifier = modifier.height(60.dp),
        shape = shape,
        colors = ButtonDefaults.buttonColors(backgroundColor = color)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.button.copy(
                color = textColor,
                fontWeight = FontWeight.Bold,
                fontSize = fontSize
            )
        )
    }
}

@Composable
fun OutlinedButton(
    modifier: Modifier = Modifier,
    text: String,
    fontSize: TextUnit = 20.sp,
    textColor: Color = MaterialTheme.colors.primary,
    color: Color = MaterialTheme.colors.primary,
    shape: Shape = MaterialTheme.shapes.small.copy(CornerSize(60)),
    stroke: Dp = 2.dp,
    onclick: () -> Unit
) {
    OutlinedButton(
        onClick = onclick,
        modifier = modifier.height(60.dp),
        border = BorderStroke(stroke, color),
        shape = shape,
        colors = ButtonDefaults.outlinedButtonColors(contentColor = color)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.button.copy(
                color = textColor,
                fontWeight = FontWeight.Bold,
                fontSize = fontSize
            )
        )
    }
}
