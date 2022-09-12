package com.lamti.capturetheflag.presentation.ui.components.screens

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.lamti.capturetheflag.R
import com.lamti.capturetheflag.presentation.ui.components.composables.common.DefaultButton
import com.lamti.capturetheflag.presentation.ui.components.composables.common.PulseInteractor
import com.lamti.capturetheflag.presentation.ui.playSound
import com.lamti.capturetheflag.presentation.ui.style.Red
import com.lamti.capturetheflag.utils.EMPTY
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

private val punchSound: Uri = Uri.parse("android.resource://com.lamti.capturetheflag/" + R.raw.punch)

@Composable
fun BattleGameScreen(
    modifier: Modifier = Modifier,
    color: Color = Red,
    winner: String = EMPTY,
    isPlayerReady: Boolean = false,
    battleStarted: Boolean = false,
    onReadyClicked: () -> Unit,
    onWinnerFound: () -> Unit,
) {
    LaunchedEffect(winner) { if (winner != EMPTY) onWinnerFound() }
    TapTheFlag(
        modifier = modifier,
        color = color,
        isPlayerReady = isPlayerReady,
        battleStarted = battleStarted,
        onReadyClicked = onReadyClicked,
        onWinnerFound = onWinnerFound
    )
}

@Composable
fun TapTheFlag(
    modifier: Modifier = Modifier,
    animationTime: Int = 300,
    animationDelayTime: Int = 0,
    winnerTaps: Int = 5,
    color: Color,
    isPlayerReady: Boolean,
    battleStarted: Boolean,
    onReadyClicked: () -> Unit,
    onWinnerFound: () -> Unit
) {
    val context = LocalContext.current
    var timesTaped by remember { mutableStateOf(0) }
    var visible by remember { mutableStateOf(false) }
    var startPulseAnimation by remember { mutableStateOf(false) }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        var initialOffset by remember { mutableStateOf(Offset(maxWidth.value / 2 - 10, maxHeight.value / 2 - 10)) }
        var randomOffset by remember { mutableStateOf(Offset(maxWidth.value / 2 - 10, maxHeight.value / 2 - 10)) }

        val iconLocation by animateOffsetAsState(
            targetValue = if (visible) initialOffset else randomOffset,
            animationSpec = tween(animationTime, animationDelayTime, easing = LinearOutSlowInEasing)
        )

        LaunchedEffect(visible) {
            if (!battleStarted) return@LaunchedEffect
            delay(500)
            visible = !visible
            initialOffset = getRandomOffset(maxWidth.value, maxHeight.value)
            randomOffset = getRandomOffset(maxWidth.value, maxHeight.value)
        }

        LaunchedEffect(battleStarted) {
            if (battleStarted) {
                // Start flag's animation
                visible = !visible
            }
        }

        PulseInteractor(
            modifier = Modifier.offset(iconLocation.x.dp - maxWidth / 5, iconLocation.y.dp - maxHeight / 10),
            color = color,
            startAnimation = startPulseAnimation,
            onAnimationEnd = {
                startPulseAnimation = false
            }
        )

        FlagIcon(
            modifier = Modifier.offset(iconLocation.x.dp, iconLocation.y.dp),
            tint = color,
            isClickable = battleStarted,
            onIconClicked = {
                timesTaped += 1
                context.playSound(punchSound)
                startPulseAnimation = true
                if (timesTaped == winnerTaps) onWinnerFound()
            }
        )
        Text(
            modifier = Modifier.padding(20.dp),
            text = buildAnnotatedString {
                append("Tap on the flag 10 times to win the battle: ")
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = color)) {
                    append(timesTaped.toString())
                }
            }
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AnimatedVisibility(visible = !isPlayerReady) {
                DefaultButton(text = "Ready") {
                    onReadyClicked()
                }
            }
        }
    }
}

private fun getRandomOffset(screenWidth: Float, screenHeight: Float): Offset {
    val randomX: Float = (0..screenWidth.roundToInt()).random().toFloat()
    val randomY: Float = (0..screenHeight.roundToInt()).random().toFloat()
    return Offset(randomX, randomY)
}

@Composable
private fun FlagIcon(
    modifier: Modifier = Modifier,
    isClickable: Boolean = false,
    icon: Int = R.drawable.ic_flag,
    tint: Color,
    onIconClicked: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    Icon(
        modifier = modifier.clickable(
            enabled = isClickable,
            interactionSource = interactionSource,
            indication = null
        ) {
            onIconClicked()
        },
        painter = painterResource(id = icon),
        tint = tint,
        contentDescription = "flag icon"
    )
}

