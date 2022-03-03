package com.lamti.capturetheflag.presentation.ui.activity

import android.Manifest
import android.annotation.TargetApi
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.lamti.capturetheflag.R
import com.lamti.capturetheflag.databinding.ActivityMainBinding
import com.lamti.capturetheflag.presentation.arcore.helpers.FullScreenHelper
import com.lamti.capturetheflag.data.location.service.LocationServiceCommand
import com.lamti.capturetheflag.data.location.service.LocationServiceImpl
import com.lamti.capturetheflag.data.location.service.LocationServiceImpl.Companion.SERVICE_COMMAND
import com.lamti.capturetheflag.data.location.service.isLocationEnabledOrNot
import com.lamti.capturetheflag.data.location.service.showAlertLocation
import com.lamti.capturetheflag.presentation.ui.components.BottomNavigationView
import com.lamti.capturetheflag.presentation.ui.fragments.navigation.Screen
import com.lamti.capturetheflag.presentation.ui.fragments.navigation.navigateToScreen
import com.lamti.capturetheflag.presentation.ui.login.LoginActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@InternalCoroutinesApi
@ExperimentalCoroutinesApi
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        navigateToLoginIfNeededDuringSplashScreen()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        collectFlows()
        startLocationUpdates()
    }

    private fun navigateToLoginIfNeededDuringSplashScreen() {
        installSplashScreen().apply {
            var isEnterFirstTime = false
            setKeepOnScreenCondition {
                if (!viewModel.isUserLoggedIn && !isEnterFirstTime) {
                    isEnterFirstTime = true
                    startActivity(Intent(this@MainActivity, LoginActivity::class.java))
                    finish()
                }
                return@setKeepOnScreenCondition !viewModel.isUserLoggedIn
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        sendCommandToForegroundService(LocationServiceCommand.Stop)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        FullScreenHelper.setFullScreenOnWindowFocusChanged(this, hasFocus)
    }

    private fun setupUI() = with(binding) {
        logoutButton.setOnClickListener {
            viewModel.onLogoutClicked()
            startActivity(Intent(this@MainActivity, LoginActivity::class.java))
            finish()
        }
        bottomView.setContent {
            BottomNavigationView(
                onStatsClicked = { viewModel.onStatsClicked() },
                onMapClicked = { viewModel.onMapClicked() },
                onChatClicked = { viewModel.onChatClicked() }
            )
        }
    }

    private fun collectFlows() {
        lifecycleScope.launchWhenCreated {
            viewModel.currentScreen.onEach(::navigate).launchIn(lifecycleScope)
        }
    }

    private fun startLocationUpdates() {
        if (!isLocationEnabledOrNot(this)) {
            showAlertLocation(
                this,
                getString(R.string.gps_enable),
                getString(R.string.please_turn_on_gps),
                getString(R.string.ok)
            )
        }

        requestPermissionsSafely()
        sendCommandToForegroundService(LocationServiceCommand.Start)
    }

    @TargetApi(Build.VERSION_CODES.M)
    private fun requestPermissionsSafely(
        permissions: Array<String> = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
        requestCode: Int = PERMISSION_REQUEST_CODE
    ) {
        requestPermissions(permissions, requestCode)
    }

    private fun navigate(screen: Screen) {
        supportFragmentManager.navigateToScreen(screen)
    }

    private fun sendCommandToForegroundService(command: LocationServiceCommand) {
        ContextCompat.startForegroundService(this, getServiceIntent(command))
    }

    private fun getServiceIntent(command: LocationServiceCommand) =
        Intent(this, LocationServiceImpl::class.java).apply {
            putExtra(SERVICE_COMMAND, command as Parcelable)
        }

    companion object {

        private const val PERMISSION_REQUEST_CODE = 200
    }

}
