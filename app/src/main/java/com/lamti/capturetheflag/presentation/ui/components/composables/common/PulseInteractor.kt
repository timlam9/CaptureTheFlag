package com.lamti.capturetheflag.presentation.ui.components.composables.common

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.lamti.capturetheflag.presentation.ui.style.Green

@Composable
fun PulseInteractor(
    modifier: Modifier = Modifier,
    color: Color = Green,
    startAnimation: Boolean = false,
    onAnimationEnd: (Float) -> Unit
) {
    val scale: State<Float> = animateFloatAsState(
        targetValue = if (startAnimation) 7f else 0f,
        animationSpec = tween(durationMillis = 350, delayMillis = 0, easing = FastOutSlowInEasing),
        finishedListener = onAnimationEnd
    )
    val alpha = animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 350, delayMillis = 0, easing = FastOutSlowInEasing),
    )

    Box(
        modifier = modifier
            .size(180.dp)
            .scale(scale.value)
            .alpha(1 - alpha.value)
            .clip(CircleShape)
            .background(color = color)
    )
}
