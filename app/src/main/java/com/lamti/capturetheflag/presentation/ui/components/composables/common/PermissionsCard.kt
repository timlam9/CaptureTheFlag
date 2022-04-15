package com.lamti.capturetheflag.presentation.ui.components.composables.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lamti.capturetheflag.R
import com.lamti.capturetheflag.presentation.ui.style.Blue
import com.lamti.capturetheflag.presentation.ui.style.TextColor

@Composable
fun PermissionsCard(modifier: Modifier = Modifier, title: String, description: String, onOkClicked: () -> Unit) {
    Card(
        modifier = modifier
            .height(280.dp)
            .fillMaxWidth()
            .padding(24.dp),
        elevation = 10.dp,
        backgroundColor = MaterialTheme.colors.background,
        shape = MaterialTheme.shapes.small.copy(CornerSize(6))
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceAround
        ) {
            Text(text = title, style = MaterialTheme.typography.h5.copy(color = Blue, fontWeight = FontWeight.Bold))
            Text(text = description, style = MaterialTheme.typography.subtitle1.copy(color = TextColor))
            DefaultButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                text = stringResource(id = R.string.ok),
                onclick = onOkClicked
            )
        }
    }
}
