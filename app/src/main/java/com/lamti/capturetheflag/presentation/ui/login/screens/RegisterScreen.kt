package com.lamti.capturetheflag.presentation.ui.login.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lamti.capturetheflag.R
import com.lamti.capturetheflag.presentation.ui.components.composables.common.DefaultButton
import com.lamti.capturetheflag.presentation.ui.components.composables.common.InfoTextField
import com.lamti.capturetheflag.presentation.ui.components.composables.common.PasswordTextField
import com.lamti.capturetheflag.presentation.ui.components.composables.common.RoundedIcon
import com.lamti.capturetheflag.presentation.ui.login.components.RegisterData
import com.lamti.capturetheflag.utils.EMPTY

@Composable
fun RegisterScreen(
    isLoading: Boolean,
    onLogoClicked: () -> Unit,
    onSignUpClicked: (RegisterData) -> Unit
) {
    var username by remember { mutableStateOf(EMPTY) }
    var email by remember { mutableStateOf(EMPTY) }
    var password by remember { mutableStateOf(EMPTY) }
    var confirmPassword by remember { mutableStateOf(EMPTY) }

    var nameError by remember { mutableStateOf(EMPTY) }
    var emailError by remember { mutableStateOf(EMPTY) }
    var passwordError by remember { mutableStateOf(EMPTY) }
    var confirmPasswordError by remember { mutableStateOf(EMPTY) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Title()
        InfoTextField(
            modifier = Modifier.padding(top = 20.dp),
            text = username,
            label = stringResource(id = R.string.username),
            onValueChange = { username = it.trimEnd() },
            leadingIcon = Icons.Default.Person,
            error = nameError
        )
        InfoTextField(
            modifier = Modifier.padding(top = 12.dp),
            text = email,
            keyboardOptions = KeyboardOptions.Default.copy(
                imeAction = ImeAction.Next,
                keyboardType = KeyboardType.Email,
            ),
            label = stringResource(id = R.string.email),
            onValueChange = { email = it.trimEnd() },
            leadingIcon = Icons.Default.Email,
            error = emailError
        )
        PasswordTextField(
            modifier = Modifier.padding(top = 12.dp),
            text = password,
            keyboardOptions = KeyboardOptions.Default.copy(
                imeAction = ImeAction.Next,
                keyboardType = KeyboardType.Password
            ),
            label = stringResource(id = R.string.password),
            onValueChange = { password = it.trim() },
            error = passwordError
        )
        PasswordTextField(
            modifier = Modifier.padding(top = 12.dp),
            text = confirmPassword,
            label = stringResource(id = R.string.confirm_password),
            onValueChange = { confirmPassword = it.trim() },
            error = confirmPasswordError
        )
        SingUpButton(
            username = username,
            email = email,
            password = password,
            onSignUpClicked = {
                nameError = validateUsername(username)
                emailError = validateEmail(email)
                passwordError = validatePassword(password)
                confirmPasswordError = validateConfirmPassword(password, confirmPassword)

                if (nameError.isEmpty() && emailError.isEmpty() && passwordError.isEmpty() && confirmPasswordError.isEmpty())
                    onSignUpClicked(it)
            }
        )
        OrSignUpRow()
        LoginButtonIcons(onLogoClicked)
        Loading(isLoading)
    }
}

private fun validateUsername(name: String): String = if (name.isEmpty()) "Required filled" else EMPTY

private fun validateEmail(email: String): String = with(email) {
    when {
        isEmpty() -> "Required filled"
        !contains("@") || !contains(".") -> "Not valid email"
        else -> EMPTY
    }
}

private fun validatePassword(password: String): String = with(password) {
    when {
        isEmpty() -> "Required filled"
        length < 8 -> "Password must have at least 8 characters"
        else -> EMPTY
    }
}

private fun validateConfirmPassword(password: String, confirmPassword: String): String = when {
    confirmPassword.isEmpty() -> "Required filled"
    confirmPassword != password -> "Doesn't match with the password"
    else -> EMPTY
}


@Composable
private fun Title() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.welcome),
            style = MaterialTheme.typography.h4.copy(
                color = MaterialTheme.colors.onBackground,
                fontWeight = FontWeight.Bold
            )
        )
    }
}

@Composable
private fun SingUpButton(
    onSignUpClicked: (RegisterData) -> Unit,
    username: String,
    email: String,
    password: String
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    DefaultButton(
        modifier = Modifier
            .padding(top = 24.dp)
            .fillMaxWidth(),
        text = stringResource(id = R.string.sign_up),
        textColor = MaterialTheme.colors.background,
        color = MaterialTheme.colors.primary,
        cornerSize = CornerSize(20),
        onclick = {
            keyboardController?.hide()
            onSignUpClicked(
                RegisterData(
                    username = username,
                    email = email,
                    password = password
                )
            )
        },
    )
}

@Composable
private fun OrSignUpRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .height(3.dp)
                .padding(start = 16.dp, end = 16.dp)
                .weight(1f)
                .clip(RoundedCornerShape(20.dp))
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colors.background,
                            MaterialTheme.colors.primary
                        )
                    )
                )
        )
        Text(
            text = stringResource(R.string.or_sign_up_with),
            style = MaterialTheme.typography.body2.copy(color = MaterialTheme.colors.onSurface, fontSize = 12.sp)
        )
        Box(
            modifier = Modifier
                .height(3.dp)
                .padding(start = 16.dp, end = 16.dp)
                .weight(1f)
                .clip(RoundedCornerShape(20.dp))
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colors.primary,
                            MaterialTheme.colors.background
                        )
                    )
                )
        )
    }
}

@Composable
private fun LoginButtonIcons(onLogoClicked: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        RoundedIcon(
            icon = R.drawable.ic_google_logo,
            onClick = onLogoClicked
        )
        Spacer(modifier = Modifier.width(16.dp))
        RoundedIcon(
            icon = R.drawable.ic_facebook_logo,
            onClick = onLogoClicked
        )
        Spacer(modifier = Modifier.width(16.dp))
        RoundedIcon(
            icon = R.drawable.ic_apple_logo,
            onClick = onLogoClicked
        )
    }
}

@Composable
private fun ColumnScope.Loading(isLoading: Boolean) {
    AnimatedVisibility(visible = isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }
}
