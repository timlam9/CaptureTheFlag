package com.lamti.capturetheflag.presentation.ui.login

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.lamti.capturetheflag.domain.FirestoreRepository
import com.lamti.capturetheflag.presentation.ui.activity.MainActivity
import com.lamti.capturetheflag.presentation.ui.style.CaptureTheFlagTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginData(
    val email: String,
    val password: String
)

data class RegisterData(
    val email: String,
    val password: String,
    val username: String,
    val fullName: String,
)

@ExperimentalCoroutinesApi
@InternalCoroutinesApi
@AndroidEntryPoint
class LoginActivity : ComponentActivity() {

    @Inject
    lateinit var firestoreRepository: FirestoreRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val scope  = rememberCoroutineScope()

            CaptureTheFlagTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colors.background) {
                    val navController = rememberNavController()
                    LoginAndRegistration(
                        navController = navController,
                        onLoginSuccess = { loginData ->
                            scope.launch {
                                val loginSuccessfully = firestoreRepository.loginUser(loginData.email, loginData.password)
                                if(loginSuccessfully) {
                                    startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                                    finish()
                                }
                            }
                        },
                        onRegisterSuccess = { registerData ->
                            with(registerData) {
                                scope.launch {
                                    firestoreRepository.registerUser(email, password, username, fullName) {
                                        navController.navigate("login_screen") {
                                            popUpTo(navController.graph.startDestinationId)
                                            launchSingleTop = true
                                        }
                                    }
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun LoginAndRegistration(navController: NavHostController,onLoginSuccess: (LoginData) -> Unit, onRegisterSuccess: (RegisterData) -> Unit) {
    NavHost(
        navController = navController,
        startDestination = "login_screen",
        builder = {
            composable(
                "login_screen",
                content = { LoginScreen(navController = navController, onLoginSuccess = onLoginSuccess) })
            composable(
                "register_screen",
                content = { RegistrationScreen(navController = navController, onRegisterSuccess = onRegisterSuccess) })
        }
    )
}

@Composable
fun LoginScreen(navController: NavController, onLoginSuccess: (LoginData) -> Unit) {
    val email = remember { mutableStateOf(TextFieldValue()) }
    val emailErrorState = remember { mutableStateOf(false) }
    val passwordErrorState = remember { mutableStateOf(false) }
    val password = remember { mutableStateOf(TextFieldValue()) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = buildAnnotatedString {
            withStyle(style = SpanStyle(color = MaterialTheme.colors.secondary)) {
                append("S")
            }
            withStyle(style = SpanStyle(color = MaterialTheme.colors.primary)) {
                append("ign")
            }

            withStyle(style = SpanStyle(color = MaterialTheme.colors.secondary)) {
                append(" I")
            }
            withStyle(style = SpanStyle(color = MaterialTheme.colors.primary)) {
                append("n")
            }
        }, fontSize = 30.sp)
        Spacer(Modifier.size(16.dp))
        OutlinedTextField(
            value = email.value,
            onValueChange = {
                if (emailErrorState.value) {
                    emailErrorState.value = false
                }
                email.value = it
            },
            isError = emailErrorState.value,
            modifier = Modifier.fillMaxWidth(),
            label = {
                Text(text = "Enter Email*")
            },
        )
        if (emailErrorState.value) {
            Text(text = "Required", color = MaterialTheme.colors.primary)
        }
        Spacer(Modifier.size(16.dp))
        val passwordVisibility = remember { mutableStateOf(true) }
        OutlinedTextField(
            value = password.value,
            onValueChange = {
                if (passwordErrorState.value) {
                    passwordErrorState.value = false
                }
                password.value = it
            },
            isError = passwordErrorState.value,
            modifier = Modifier.fillMaxWidth(),
            label = {
                Text(text = "Enter Password*")
            },
            trailingIcon = {
                IconButton(onClick = {
                    passwordVisibility.value = !passwordVisibility.value
                }) {
                    Icon(
                        imageVector = if (passwordVisibility.value) Icons.Default.ThumbUp else Icons.Default.Warning,
                        contentDescription = "visibility",
                        tint = MaterialTheme.colors.primary
                    )
                }
            },
            visualTransformation = if (passwordVisibility.value) PasswordVisualTransformation() else VisualTransformation.None
        )
        if (passwordErrorState.value) {
            Text(text = "Required", color = MaterialTheme.colors.primary)
        }
        Spacer(Modifier.size(16.dp))
        Button(
            onClick = {
                when {
                    email.value.text.isEmpty() -> {
                        emailErrorState.value = true
                    }
                    password.value.text.isEmpty() -> {
                        passwordErrorState.value = true
                    }
                    else -> {
                        passwordErrorState.value = false
                        emailErrorState.value = false
                        onLoginSuccess(
                            LoginData(
                                email = email.value.text,
                                password = password.value.text
                            )
                        )
                    }
                }

            },
            content = {
                Text(text = "Login", color = MaterialTheme.colors.secondary)
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.primary)
        )
        Spacer(Modifier.size(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            TextButton(onClick = {
                navController.navigate("register_screen") {
                    popUpTo(navController.graph.startDestinationId)
                    launchSingleTop = true
                }
            }) {
                Text(text = "Register ?", color = MaterialTheme.colors.primary)
            }
        }
    }
}


@Composable
fun RegistrationScreen(navController: NavController, onRegisterSuccess: (RegisterData) -> Unit) {
    val username = remember { mutableStateOf(TextFieldValue()) }
    val email = remember { mutableStateOf(TextFieldValue()) }
    val firstName = remember { mutableStateOf(TextFieldValue()) }
    val lastName = remember { mutableStateOf(TextFieldValue()) }
    val password = remember { mutableStateOf(TextFieldValue()) }
    val confirmPassword = remember { mutableStateOf(TextFieldValue()) }

    val nameErrorState = remember { mutableStateOf(false) }
    val emailErrorState = remember { mutableStateOf(false) }
    val passwordErrorState = remember { mutableStateOf(false) }
    val confirmPasswordErrorState = remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.Center,
    ) {

        Text(text = buildAnnotatedString {
            withStyle(style = SpanStyle(color = MaterialTheme.colors.secondary)) {
                append("R")
            }
            withStyle(style = SpanStyle(color = MaterialTheme.colors.primary)) {
                append("egistration")
            }
        }, fontSize = 30.sp)
        Spacer(Modifier.size(16.dp))
        OutlinedTextField(
            value = username.value,
            onValueChange = {
                if (nameErrorState.value) {
                    nameErrorState.value = false
                }
                username.value = it
            },

            modifier = Modifier.fillMaxWidth(),
            isError = nameErrorState.value,
            label = {
                Text(text = "Username*")
            },
        )
        if (nameErrorState.value) {
            Text(text = "Required", color = MaterialTheme.colors.primary)
        }
        Spacer(Modifier.size(16.dp))

        OutlinedTextField(
            value = email.value,
            onValueChange = {
                if (emailErrorState.value) {
                    emailErrorState.value = false
                }
                email.value = it
            },

            modifier = Modifier.fillMaxWidth(),
            isError = emailErrorState.value,
            label = {
                Text(text = "Email*")
            },
        )
        if (emailErrorState.value) {
            Text(text = "Required", color = MaterialTheme.colors.primary)
        }
        Spacer(modifier = Modifier.size(16.dp))
        Row {
            OutlinedTextField(
                value = firstName.value,
                onValueChange = {

                    firstName.value = it
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    autoCorrect = false
                ),
                modifier = Modifier.fillMaxWidth(0.5f),
                label = {
                    Text(text = "First Name")
                },
            )
            Spacer(modifier = Modifier.size(16.dp))
            OutlinedTextField(
                value = lastName.value,
                onValueChange = {

                    lastName.value = it
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Phone,
                    autoCorrect = false
                ),
                label = {
                    Text(text = "Last Name")
                },
            )
        }

        Spacer(Modifier.size(16.dp))

        val passwordVisibility = remember { mutableStateOf(true) }

        OutlinedTextField(
            value = password.value,
            onValueChange = {
                if (passwordErrorState.value) {
                    passwordErrorState.value = false
                }
                password.value = it
            },
            modifier = Modifier.fillMaxWidth(),
            label = {
                Text(text = "Password*")
            },
            isError = passwordErrorState.value,
            trailingIcon = {
                IconButton(onClick = {
                    passwordVisibility.value = !passwordVisibility.value
                }) {
                    Icon(
                        imageVector = if (passwordVisibility.value) Icons.Default.ThumbUp else Icons.Default.Warning,
                        contentDescription = "visibility",
                        tint = MaterialTheme.colors.primary
                    )
                }
            },
            visualTransformation = if (passwordVisibility.value) PasswordVisualTransformation() else VisualTransformation.None
        )
        if (passwordErrorState.value) {
            Text(text = "Required", color = MaterialTheme.colors.primary)
        }

        Spacer(Modifier.size(16.dp))
        val cPasswordVisibility = remember { mutableStateOf(true) }
        OutlinedTextField(
            value = confirmPassword.value,
            onValueChange = {
                if (confirmPasswordErrorState.value) {
                    confirmPasswordErrorState.value = false
                }
                confirmPassword.value = it
            },
            modifier = Modifier.fillMaxWidth(),
            isError = confirmPasswordErrorState.value,
            label = {
                Text(text = "Confirm Password*")
            },
            trailingIcon = {
                IconButton(onClick = {
                    cPasswordVisibility.value = !cPasswordVisibility.value
                }) {
                    Icon(
                        imageVector = if (cPasswordVisibility.value) Icons.Default.ThumbUp else Icons.Default.Warning,
                        contentDescription = "visibility",
                        tint = MaterialTheme.colors.primary
                    )
                }
            },
            visualTransformation = if (cPasswordVisibility.value) PasswordVisualTransformation() else VisualTransformation.None
        )
        if (confirmPasswordErrorState.value) {
            val msg = when {
                confirmPassword.value.text.isEmpty() -> "Required"
                confirmPassword.value.text != password.value.text -> "Password not matching"
                else -> ""
            }
            Text(text = msg, color = MaterialTheme.colors.primary)
        }
        Spacer(Modifier.size(16.dp))
        Button(
            onClick = {
                when {
                    username.value.text.isEmpty() -> {
                        nameErrorState.value = true
                    }
                    email.value.text.isEmpty() -> {
                        emailErrorState.value = true
                    }
                    password.value.text.isEmpty() -> {
                        passwordErrorState.value = true
                    }
                    confirmPassword.value.text.isEmpty() -> {
                        confirmPasswordErrorState.value = true
                    }
                    confirmPassword.value.text != password.value.text -> {
                        confirmPasswordErrorState.value = true
                    }
                    else -> onRegisterSuccess(
                        RegisterData(
                            email = email.value.text,
                            password = password.value.text,
                            username = username.value.text,
                            fullName = "${lastName.value.text} ${firstName.value.text}"
                        )
                    )
                }
            },
            content = {
                Text(text = "Register", color = MaterialTheme.colors.secondary)
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.primary)
        )
        Spacer(Modifier.size(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            TextButton(onClick = {
                navController.navigate("login_screen") {
                    popUpTo(navController.graph.startDestinationId)
                    launchSingleTop = true
                }
            }) {
                Text(text = "Login", color = MaterialTheme.colors.primary)
            }
        }
    }
}
