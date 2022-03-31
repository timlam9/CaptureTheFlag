package com.lamti.capturetheflag.presentation.ui.components.composables.ar

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.clipPath
import com.lamti.capturetheflag.presentation.ui.style.Black

@Composable
fun TransparentBackgroundCircle(color: Color = Black) {
    Canvas(
        modifier = Modifier.fillMaxSize(),
        onDraw = {
            val circlePath = Path().apply {
                addOval(Rect(center, size.minDimension / 2))
            }
            clipPath(circlePath, clipOp = ClipOp.Difference) {
                drawRect(SolidColor(color.copy(alpha = 0.8f)))
            }
        }
    )
}
