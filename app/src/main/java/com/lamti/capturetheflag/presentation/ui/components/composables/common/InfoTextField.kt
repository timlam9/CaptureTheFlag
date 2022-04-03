package com.lamti.capturetheflag.presentation.ui.components.composables.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import com.lamti.capturetheflag.utils.EMPTY

@Composable
fun InfoTextField(
    modifier: Modifier = Modifier,
    text: String,
    label: String,
    error: String = EMPTY,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default.copy(
        imeAction = ImeAction.Next,
        keyboardType = KeyboardType.Text,
        capitalization = KeyboardCapitalization.Words
    ),
    leadingIcon: ImageVector = Icons.Default.Info,
    onValueChange: (String) -> Unit
) {
    val focusManager = LocalFocusManager.current

    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = text,
            isError = error.isNotEmpty(),
            onValueChange = { onValueChange(it) },
            label = { Text(label) },
            colors = TextFieldDefaults.outlinedTextFieldColors(),
            maxLines = 1,
            textStyle = MaterialTheme.typography.body1.copy(textAlign = TextAlign.Start),
            singleLine = true,
            keyboardOptions = keyboardOptions,
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) },
                onDone = { focusManager.clearFocus() }
            ),
            leadingIcon = {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null
                )
            }
        )
        AnimatedVisibility(visible = error.isNotEmpty()) {
            Text(
                text = error,
                style = MaterialTheme.typography.body2.copy(color = MaterialTheme.colors.error)
            )
        }
    }
}
