package com.lamti.capturetheflag.presentation.ui.components.composables.common

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.lamti.capturetheflag.R

@Composable
fun PasswordTextField(
    modifier: Modifier = Modifier,
    text: String,
    label: String = stringResource(R.string.type_password),
    placeholder: String = stringResource(R.string.password),
    onValueChange: (String) -> Unit
) {
    val focusManager = LocalFocusManager.current
    var passwordVisibility by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = text,
        onValueChange = { onValueChange(it) },
        modifier = modifier
            .padding(top = 16.dp)
            .fillMaxWidth(),
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done,
        ),
        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
        singleLine = true,
        visualTransformation = if (passwordVisibility) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            val iconId = if (passwordVisibility) R.drawable.ic_visibility else R.drawable.ic_visibility_off
            IconButton(
                onClick = { passwordVisibility = !passwordVisibility }
            ) {
                Icon(painter = painterResource(iconId), "")
            }
        }
    )
}
