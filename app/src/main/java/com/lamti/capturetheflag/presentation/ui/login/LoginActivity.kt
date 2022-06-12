package com.lamti.capturetheflag.presentation.ui.login

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.lamti.capturetheflag.R
import com.lamti.capturetheflag.data.location.service.checkBackgroundLocationPermissionAPI30
import com.lamti.capturetheflag.data.location.service.checkLocationPermissionAPI29
import com.lamti.capturetheflag.data.location.service.checkSinglePermission
import com.lamti.capturetheflag.data.location.service.isLocationEnabledOrNot
import com.lamti.capturetheflag.data.location.service.showAlertLocation
import com.lamti.capturetheflag.domain.FirestoreRepository
import com.lamti.capturetheflag.presentation.ui.DatastoreHelper
import com.lamti.capturetheflag.presentation.ui.activity.MainActivity
import com.lamti.capturetheflag.presentation.ui.login.components.LoginAndRegistration
import com.lamti.capturetheflag.presentation.ui.login.components.navigateToScreen
import com.lamti.capturetheflag.presentation.ui.style.CaptureTheFlagTheme
import com.lamti.capturetheflag.utils.LOGGER_TAG
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class LoginActivity : ComponentActivity() {

    @Inject lateinit var firestoreRepository: FirestoreRepository
    @Inject lateinit var dataStore: DatastoreHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val scope = rememberCoroutineScope()
            val navController = rememberNavController()

            val initialScreen by dataStore.initialScreen.collectAsState(initial = "onboarding_screen")
            val isLoading by dataStore.isLoading.collectAsState(initial = false)
            var next by remember { mutableStateOf(0) }
            val hasPermissions by dataStore.hasPreferences.collectAsState(initial = false)

            CaptureTheFlagTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    LoginAndRegistration(
                        navController = navController,
                        initialScreen = initialScreen,
                        next = next,
                        hasPermissions = hasPermissions,
                        onOnboardingStartButtonClicked = {
                            if (hasPermissions) {
                                navController.popBackStack()
                                navController.navigateToScreen("intro_screen")
                                scope.launch { dataStore.saveInitialScreen("intro_screen") }
                            } else
                                showToast("Location permissions are required in order to continue.")
                        },
                        onPermissionsOkClicked = { if (hasPermissions) next++ else requestLocationPermissions() },
                        isLoading = isLoading,
                        onLogoClicked = { showToast("Not available yet") },
                        onLoginClicked = { loginData ->
                            scope.launch {
                                dataStore.saveIsLoading(true)
                                if (firestoreRepository.loginUser(loginData.email, loginData.password)) {
                                    dataStore.saveIsLoading(false)
                                    startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                                    finish()
                                } else {
                                    dataStore.saveIsLoading(false)
                                    showToast("Email and password doesn't match")
                                }
                            }
                        }
                    ) { registerData ->
                        with(registerData) {
                            scope.launch {
                                dataStore.saveIsLoading(true)
                                if (firestoreRepository.registerUser(email, password, username)) {
                                    dataStore.saveIsLoading(false)
                                    startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                                    finish()
                                } else {
                                    dataStore.saveIsLoading(false)
                                    showToast("Email and password doesn't match")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun showToast(text: String) = Toast.makeText(this@LoginActivity, text, Toast.LENGTH_SHORT).show()

    private fun requestLocationPermissions() {
        if (!isLocationEnabledOrNot(this)) {
            showAlertLocation(getString(R.string.gps_enable), getString(R.string.please_turn_on_gps), getString(R.string.ok))
        }
        requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), PERMISSION_REQUEST_CODE)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (checkSinglePermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
                    when {
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> checkBackgroundLocationPermissionAPI30(requestCode)
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> checkLocationPermissionAPI29(requestCode)
                        else -> {
                            lifecycleScope.launch {
                                dataStore.saveHasPreferences(true)
                            }
                        }
                    }
                }
                if (checkSinglePermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {
                    lifecycleScope.launch {
                        dataStore.saveHasPreferences(true)
                    }
                    showToast("Location permissions are granted!")
                }
            } else {
                Timber.d("[$LOGGER_TAG] Permission denied: ${grantResults[0]}")
            }
        }
    }

    companion object {

        private const val PERMISSION_REQUEST_CODE = 200
    }

}
