package com.lamti.capturetheflag.presentation.ui.components.composables

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lamti.capturetheflag.presentation.ui.style.Green
import com.lamti.capturetheflag.presentation.ui.style.White

@Composable
fun DefaultButton(
    modifier: Modifier = Modifier,
    text: String,
    fontSize: TextUnit = 20.sp,
    textColor: Color = White,
    color: Color = MaterialTheme.colors.primary,
    cornerSize: CornerSize = CornerSize(60),
    shape: Shape = MaterialTheme.shapes.small.copy(cornerSize),
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

@Composable
fun IconButton(
    modifier: Modifier = Modifier,
    size: Dp = 42.dp,
    cornerRadius: Int = 20,
    shape: Shape = RoundedCornerShape(cornerRadius),
    tint: Color = MaterialTheme.colors.onBackground,
    backgroundColor: Color = MaterialTheme.colors.background,
    icon: Int,
    onclick: () -> Unit
) {
    Button(
        modifier = modifier.size(size),
        onClick = onclick,
        shape = shape,
        contentPadding = PaddingValues(0.dp),
        colors = ButtonDefaults.buttonColors(backgroundColor = backgroundColor)
    ) {
        Icon(
            painter = painterResource(id = icon),
            tint = tint,
            contentDescription = "icon button"
        )
    }
}

@Composable
fun BallScaleIndicator(
    modifier: Modifier = Modifier,
    color: Color = Green
) {
    val infiniteTransition = rememberInfiniteTransition()
    val alphaAnimationProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 850)
        )
    )
    val scaleAnimationProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 7f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 850)
        )
    )
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(
            modifier = Modifier
                .size(180.dp)
                .scale(scaleAnimationProgress)
                .alpha(1 - alphaAnimationProgress)
                .clip(CircleShape)
                .background(color = color)
        )
    }
}
