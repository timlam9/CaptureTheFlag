package com.lamti.capturetheflag.presentation.ui.components.composables.map

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Share
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun BottomNavigationView(
    modifier: Modifier = Modifier,
    onStatsClicked: () -> Unit,
    onMapClicked: () -> Unit,
    onChatClicked: () -> Unit,
    color: Color = Color.Blue,
    iconsColor: Color = Color.White,
    cornerRadius: Dp = 20.dp
) {
    Row(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(cornerRadius).copy())
            .background(color),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onStatsClicked) {
            Icon(
                imageVector = Icons.Filled.Phone,
                contentDescription = null,
                tint = iconsColor
            )
        }
        IconButton(onClick = onMapClicked) {
            Icon(
                imageVector = Icons.Filled.Home,
                contentDescription = null,
                tint = iconsColor
            )
        }
        IconButton(onClick = onChatClicked) {
            Icon(
                imageVector = Icons.Filled.Share,
                contentDescription = null,
                tint = iconsColor
            )
        }
    }
}
