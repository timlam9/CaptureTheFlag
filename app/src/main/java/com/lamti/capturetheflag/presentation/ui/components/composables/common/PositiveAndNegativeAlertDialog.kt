package com.lamti.capturetheflag.presentation.ui.components.composables.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material.AlertDialog
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lamti.capturetheflag.R

@Composable
fun PositiveAndNegativeAlertDialog(
    modifier: Modifier = Modifier,
    title: String,
    description: String,
    positiveButtonText: String = stringResource(id = R.string.ok),
    negativeButtonText: String = stringResource(id = R.string.cancel),
    showDialog: Boolean,
    onNegativeDialogClicked: () -> Unit,
    onPositiveButtonClicked: () -> Unit
) {
    if (showDialog) {
        AlertDialog(
            modifier = modifier.fillMaxWidth(),
            backgroundColor = MaterialTheme.colors.background,
            onDismissRequest = onNegativeDialogClicked,
            title = {
                Text(
                    text = title,
                    style = MaterialTheme.typography.h5.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
            },
            text = {
                Text(
                    modifier = Modifier.padding(top = 12.dp),
                    text = description,
                    style = MaterialTheme.typography.body1
                )
            },
            buttons = {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(end = 12.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DefaultButton(
                        height = 40.dp,
                        fontSize = 16.sp,
                        cornerSize = CornerSize(20),
                        text = positiveButtonText,
                        color = MaterialTheme.colors.onBackground,
                        onclick = onPositiveButtonClicked
                    )
                    Text(
                        modifier = Modifier
                            .padding(20.dp)
                            .clickable { onNegativeDialogClicked() },
                        text = negativeButtonText,
                        style = MaterialTheme.typography.h6
                    )
                }
            }
        )
    }
}
