package com.lamti.capturetheflag.presentation.ui.components.composables.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import com.lamti.capturetheflag.R
import com.lamti.capturetheflag.utils.EMPTY

@Composable
fun PasswordTextField(
    modifier: Modifier = Modifier,
    text: String,
    error: String = EMPTY,
    label: String = stringResource(R.string.type_password),
    placeholder: String = stringResource(R.string.password),
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default.copy(
        imeAction = ImeAction.Done,
        keyboardType = KeyboardType.Password
    ),
    onValueChange: (String) -> Unit
) {
    val focusManager = LocalFocusManager.current
    var passwordVisibility by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = text,
            isError = error.isNotEmpty(),
            onValueChange = { onValueChange(it) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(label) },
            placeholder = { Text(placeholder) },
            keyboardOptions = keyboardOptions,
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) },
                onDone = { focusManager.clearFocus() }
            ),
            singleLine = true,
            visualTransformation = if (passwordVisibility) VisualTransformation.None else PasswordVisualTransformation(),
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null
                )
            },
            trailingIcon = {
                val iconId = if (passwordVisibility) R.drawable.ic_visibility else R.drawable.ic_visibility_off
                IconButton(
                    onClick = { passwordVisibility = !passwordVisibility }
                ) {
                    Icon(painter = painterResource(iconId), "")
                }
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
