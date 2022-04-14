package com.lamti.capturetheflag.presentation.ui.components.composables.common

import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun LoadingAnimation(
    modifier: Modifier = Modifier,
    animatedCircleColor: Color = MaterialTheme.colors.primary,
    circleColor: Color = MaterialTheme.colors.onBackground,
    arcColor: Color = MaterialTheme.colors.onBackground,
    durationMillis: Int = 1000,
    circleRadius: Float = 120f,
    size: Dp = 100.dp,
) {
    val infiniteTransition = rememberInfiniteTransition()
    val arcAngle1 by infiniteTransition.animateFloat(
        initialValue = 0F,
        targetValue = 180F,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = durationMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    val arcAngle2 by infiniteTransition.animateFloat(
        initialValue = 180F,
        targetValue = 360F,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = durationMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    val circleAnimation by infiniteTransition.animateFloat(
        initialValue = 50f,
        targetValue = 80f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = durationMillis, delayMillis = 100, easing = FastOutLinearInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Canvas(
        modifier = modifier
            .padding(12.dp)
            .size(size)
    ) {
        drawArc(
            color = arcColor,
            startAngle = arcAngle1,
            sweepAngle = 90f,
            useCenter = false,
            style = Stroke(width = 10f, cap = StrokeCap.Round),
        )

        drawArc(
            color = arcColor,
            startAngle = arcAngle2,
            sweepAngle = 90f,
            useCenter = false,
            style = Stroke(width = 10f, cap = StrokeCap.Round),
        )

        drawCircle(
            color = circleColor,
            radius = circleRadius,
        )

        drawCircle(
            color = animatedCircleColor,
            radius = circleAnimation,
        )
    }
}
