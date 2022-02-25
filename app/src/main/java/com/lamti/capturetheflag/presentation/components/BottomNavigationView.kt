package com.lamti.capturetheflag.presentation.components

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
import androidx.compose.ui.unit.dp

@Composable
fun BottomNavigationView(
    modifier: Modifier = Modifier,
    onStatsClicked: () -> Unit,
    onMapClicked: () -> Unit,
    onChatClicked: () -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(20.dp).copy())
            .background(Color.Magenta),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onStatsClicked) {
            Icon(Icons.Filled.Phone, null)
        }
        IconButton(onClick = onMapClicked) {
            Icon(Icons.Filled.Home, null)
        }
        IconButton(onClick = onChatClicked) {
            Icon(Icons.Filled.Share, null)
        }
    }
}
