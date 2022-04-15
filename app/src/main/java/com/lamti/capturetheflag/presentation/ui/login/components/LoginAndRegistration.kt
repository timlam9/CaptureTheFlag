package com.lamti.capturetheflag.presentation.ui.login.components

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.lamti.capturetheflag.presentation.ui.login.screens.IntroScreen
import com.lamti.capturetheflag.presentation.ui.login.screens.LoginScreen
import com.lamti.capturetheflag.presentation.ui.login.screens.OnboardingScreen
import com.lamti.capturetheflag.presentation.ui.login.screens.RegisterScreen

const val INITIAL_SCREEN = "initial_screen"

@Composable
fun LoginAndRegistration(
    navController: NavHostController,
    initialScreen: String,
    onOnboardingStartButtonClicked: () -> Unit,
    isLoading: Boolean,
    onLogoClicked: () -> Unit,
    onLoginSuccess: (LoginData) -> Unit,
    onRegisterSuccess: (RegisterData) -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = initialScreen,
        builder = {
            composable(
                route = "onboarding_screen",
                content = {
                    OnboardingScreen(
                        onStartButtonClicked = {
                            navController.popBackStack()
                            navController.navigateToScreen("intro_screen")
                            onOnboardingStartButtonClicked()
                        }
                    )
                }
            )
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
