package com.lamti.capturetheflag.presentation.ui.activity

import android.Manifest
import android.annotation.TargetApi
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.lamti.capturetheflag.R
import com.lamti.capturetheflag.data.location.geofences.ENTER_GEOFENCE_KEY
import com.lamti.capturetheflag.data.location.geofences.GEOFENCE_BROADCAST_RECEIVER_FILTER
import com.lamti.capturetheflag.data.location.geofences.GeofenceBroadcastReceiver
import com.lamti.capturetheflag.data.location.service.LocationServiceCommand
import com.lamti.capturetheflag.data.location.service.LocationServiceImpl
import com.lamti.capturetheflag.data.location.service.LocationServiceImpl.Companion.SERVICE_COMMAND
import com.lamti.capturetheflag.data.location.service.isLocationEnabledOrNot
import com.lamti.capturetheflag.data.location.service.showAlertLocation
import com.lamti.capturetheflag.databinding.ActivityMainBinding
import com.lamti.capturetheflag.presentation.arcore.helpers.FullScreenHelper
import com.lamti.capturetheflag.presentation.ui.fragments.ar.AR_MODE_KEY
import com.lamti.capturetheflag.presentation.ui.fragments.ar.ArMode
import com.lamti.capturetheflag.presentation.ui.fragments.navigation.FragmentScreen
import com.lamti.capturetheflag.presentation.ui.fragments.navigation.navigateToScreen
import com.lamti.capturetheflag.presentation.ui.login.LoginActivity
import com.lamti.capturetheflag.utils.EMPTY
import com.lamti.capturetheflag.utils.myAppPreferences
import com.lamti.capturetheflag.utils.set
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()

    var geofenceIdFLow = MutableStateFlow(EMPTY)
    private var broadcastReceiver: GeofenceBroadcastReceiver = object : GeofenceBroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            super.onReceive(context, intent)

            geofenceIdFLow.value = intent?.getStringExtra(ENTER_GEOFENCE_KEY) ?: return
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        navigateToLoginIfNeededDuringSplashScreen()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        collectScreenFlow()
        startLocationUpdates()
        registerReceiver(broadcastReceiver, IntentFilter(GEOFENCE_BROADCAST_RECEIVER_FILTER))
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
        when (viewModel.currentScreen.value) {
            FragmentScreen.Map -> super.onBackPressed()
            FragmentScreen.Ar -> viewModel.onArBackPressed()
        }
    }

    fun onSettingFlagsClicked() {
        viewModel.onSettingFlagsClicked()
    }

    fun onArScannerButtonClicked() {
        myAppPreferences[AR_MODE_KEY] = ArMode.Scanner
        viewModel.onArScannerButtonClicked()
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
