package com.lamti.capturetheflag.presentation.ui.activity

import android.Manifest.permission.ACCESS_BACKGROUND_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.util.Log
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
import com.lamti.capturetheflag.data.location.service.checkBackgroundLocationPermissionAPI30
import com.lamti.capturetheflag.data.location.service.checkLocationPermissionAPI29
import com.lamti.capturetheflag.data.location.service.checkSinglePermission
import com.lamti.capturetheflag.data.location.service.isLocationEnabledOrNot
import com.lamti.capturetheflag.data.location.service.isMyServiceRunning
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
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isMyServiceRunning(LocationServiceImpl::class.java)) {
            sendCommandToForegroundService(LocationServiceCommand.Stop)
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        FullScreenHelper.setFullScreenOnWindowFocusChanged(this, hasFocus)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (checkSinglePermission(ACCESS_FINE_LOCATION)) {
                    Log.d("TAGARA", "Fine location permission granted")
                    when {
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> checkBackgroundLocationPermissionAPI30(requestCode)
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> checkLocationPermissionAPI29(requestCode)
                    }
                }
                if (checkSinglePermission(ACCESS_BACKGROUND_LOCATION)) {
                    Log.d("TAGARA", "Background location permission granted")
                    collectScreenFlow()
                    registerReceiver(broadcastReceiver, IntentFilter(GEOFENCE_BROADCAST_RECEIVER_FILTER))
                    sendCommandToForegroundService(LocationServiceCommand.Start)
                }
            } else {
                Log.d("TAGARA", "permission denied: ${grantResults[0]}")
            }
        }
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
                if (!isEnterFirstTime) {
                    isEnterFirstTime = true
                    if (!viewModel.isUserLoggedIn) {
                        Log.d("TAGARA", "Go to login")
                        startActivity(Intent(this@MainActivity, LoginActivity::class.java))
                        finish()
                    } else {
                        Log.d("TAGARA", "Start location updates")
                        startLocationUpdates()
                    }
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

    private fun startLocationUpdates() {
        if (!isLocationEnabledOrNot(this)) {
            showAlertLocation(
                this,
                getString(R.string.gps_enable),
                getString(R.string.please_turn_on_gps),
                getString(R.string.ok)
            )
        }
        requestPermissions(arrayOf(ACCESS_FINE_LOCATION), PERMISSION_REQUEST_CODE)
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
