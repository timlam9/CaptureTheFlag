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
import com.lamti.capturetheflag.data.location.service.LocationServiceCommand
import com.lamti.capturetheflag.data.location.service.LocationServiceImpl
import com.lamti.capturetheflag.data.location.service.LocationServiceImpl.Companion.SERVICE_COMMAND
import com.lamti.capturetheflag.data.location.service.isLocationEnabledOrNot
import com.lamti.capturetheflag.data.location.service.showAlertLocation
import com.lamti.capturetheflag.databinding.ActivityMainBinding
import com.lamti.capturetheflag.presentation.arcore.helpers.FullScreenHelper
import com.lamti.capturetheflag.presentation.ui.fragments.navigation.FragmentScreen
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

        collectScreenFlow()
        startLocationUpdates()
    }

    override fun onDestroy() {
        super.onDestroy()
        sendCommandToForegroundService(LocationServiceCommand.Stop)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        FullScreenHelper.setFullScreenOnWindowFocusChanged(this, hasFocus)
    }

    override fun onBackPressed() {
        when(viewModel.currentScreen.value) {
            FragmentScreen.Map -> {
                super.onBackPressed()

            }
            FragmentScreen.Ar -> {
                viewModel.onArBackPressed()
            }
        }
    }

    fun onSettingFlagsClicked() {
        viewModel.onSettingFlagsClicked()
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

    private fun collectScreenFlow() {
        lifecycleScope.launchWhenCreated {
            viewModel.currentScreen.onEach(::navigate).launchIn(lifecycleScope)
        }
    }

    private fun navigate(screen: FragmentScreen) {
        supportFragmentManager.navigateToScreen(screen)
    }

    @TargetApi(Build.VERSION_CODES.M)
    private fun requestPermissionsSafely(
        permissions: Array<String> = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
        requestCode: Int = PERMISSION_REQUEST_CODE
    ) {
        requestPermissions(permissions, requestCode)
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

    private fun getServiceIntent(command: LocationServiceCommand) =
        Intent(this, LocationServiceImpl::class.java).apply {
            putExtra(SERVICE_COMMAND, command as Parcelable)
        }

    private fun sendCommandToForegroundService(command: LocationServiceCommand) {
        ContextCompat.startForegroundService(this, getServiceIntent(command))
    }

    companion object {

        private const val PERMISSION_REQUEST_CODE = 200
    }

}
