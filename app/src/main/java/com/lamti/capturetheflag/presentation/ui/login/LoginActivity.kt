package com.lamti.capturetheflag.presentation.ui.login

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.lamti.capturetheflag.domain.FirestoreRepository
import com.lamti.capturetheflag.presentation.ui.activity.MainActivity
import com.lamti.capturetheflag.presentation.ui.login.components.LoginAndRegistration
import com.lamti.capturetheflag.presentation.ui.login.components.navigateToScreen
import com.lamti.capturetheflag.presentation.ui.style.CaptureTheFlagTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class LoginActivity : ComponentActivity() {

    @Inject
    lateinit var firestoreRepository: FirestoreRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val scope = rememberCoroutineScope()
            var isLoading by remember { mutableStateOf(false) }

            CaptureTheFlagTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    val navController = rememberNavController()
                    LoginAndRegistration(
                        navController = navController,
                        isLoading = isLoading,
                        onLogoClicked = {
                            Toast.makeText(
                                this@LoginActivity,
                                "Not available yet",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        onLoginSuccess = { loginData ->
                            scope.launch {
                                isLoading = true
                                val loginSuccessfully = firestoreRepository.loginUser(loginData.email, loginData.password)
                                if (loginSuccessfully) {
                                    startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                                    finish()
                                } else {
                                    Toast.makeText(
                                        this@LoginActivity,
                                        "Email and password doesn't match",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                isLoading = false
                            }
                        },
                        onRegisterSuccess = { registerData ->
                            isLoading = true
                            with(registerData) {
                                scope.launch {
                                    val registerSuccessfully = firestoreRepository.registerUser(email, password, username)
                                    if (registerSuccessfully) {
                                        navController.navigateToScreen("login_screen")
                                    } else {
                                        Toast.makeText(
                                            this@LoginActivity,
                                            "Please fill correctly your data",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                    isLoading = false
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}
