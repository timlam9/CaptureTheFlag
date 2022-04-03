package com.lamti.capturetheflag.presentation.ui.login.components

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.lamti.capturetheflag.presentation.ui.login.screens.IntroScreen
import com.lamti.capturetheflag.presentation.ui.login.screens.LoginScreen
import com.lamti.capturetheflag.presentation.ui.login.screens.RegisterScreen

@Composable
fun LoginAndRegistration(
    navController: NavHostController,
    isLoading: Boolean,
    onLogoClicked: () -> Unit,
    onLoginSuccess: (LoginData) -> Unit,
    onRegisterSuccess: (RegisterData) -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = "intro_screen",
        builder = {
            composable(
                route = "intro_screen",
                content = {
                    IntroScreen(
                        onSignInClicked = { navController.navigateToScreen("login_screen") },
                        onRegisterClicked = { navController.navigateToScreen("register_screen") }
                    )
                }
            )
            composable(
                route = "login_screen",
                content = {
                    LoginScreen(
                        isLoading = isLoading,
                        onLogoClicked = onLogoClicked,
                        onSignInClicked = onLoginSuccess
                    )
                }
            )
            composable(
                route = "register_screen",
                content = {
                    RegisterScreen(
                        isLoading = isLoading,
                        onLogoClicked = onLogoClicked,
                        onSignUpClicked = onRegisterSuccess
                    )
                }
            )
        }
    )
}

fun NavController.navigateToScreen(route: String) {
    navigate(route) {
        popUpTo(graph.startDestinationId)
        launchSingleTop = true
    }
}

data class LoginData(
    val email: String,
    val password: String,
    val isLoading: Boolean = false,
    val error: String? = null
)

data class RegisterData(
    val email: String,
    val password: String,
    val username: String
)
