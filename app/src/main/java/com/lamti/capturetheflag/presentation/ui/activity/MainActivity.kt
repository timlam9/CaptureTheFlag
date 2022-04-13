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
import android.view.WindowManager
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.lamti.capturetheflag.R
import com.lamti.capturetheflag.data.location.geofences.GEOFENCE_BROADCAST_RECEIVER_FILTER
import com.lamti.capturetheflag.data.location.geofences.GEOFENCE_KEY
import com.lamti.capturetheflag.data.location.geofences.GeofenceBroadcastReceiver
import com.lamti.capturetheflag.data.location.service.LocationServiceCommand
import com.lamti.capturetheflag.data.location.service.LocationServiceImpl
import com.lamti.capturetheflag.data.location.service.LocationServiceImpl.Companion.SERVICE_COMMAND
import com.lamti.capturetheflag.data.location.service.NotificationHelper
import com.lamti.capturetheflag.data.location.service.checkBackgroundLocationPermissionAPI30
import com.lamti.capturetheflag.data.location.service.checkLocationPermissionAPI29
import com.lamti.capturetheflag.data.location.service.checkSinglePermission
import com.lamti.capturetheflag.data.location.service.isAppInForegrounded
import com.lamti.capturetheflag.data.location.service.isLocationEnabledOrNot
import com.lamti.capturetheflag.data.location.service.isMyServiceRunning
import com.lamti.capturetheflag.data.location.service.showAlertLocation
import com.lamti.capturetheflag.databinding.ActivityMainBinding
import com.lamti.capturetheflag.domain.player.Player
import com.lamti.capturetheflag.presentation.arcore.helpers.FullScreenHelper
import com.lamti.capturetheflag.presentation.ui.fragments.ar.AR_MODE_KEY
import com.lamti.capturetheflag.presentation.ui.fragments.ar.ArMode
import com.lamti.capturetheflag.presentation.ui.fragments.navigation.FragmentScreen
import com.lamti.capturetheflag.presentation.ui.fragments.navigation.navigateToScreen
import com.lamti.capturetheflag.presentation.ui.login.LoginActivity
import com.lamti.capturetheflag.utils.EMPTY
import com.lamti.capturetheflag.utils.GEOFENCE_LOGGER_TAG
import com.lamti.capturetheflag.utils.LOGGER_TAG
import com.lamti.capturetheflag.utils.get
import com.lamti.capturetheflag.utils.myAppPreferences
import com.lamti.capturetheflag.utils.set
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()

    @Inject
    lateinit var notificationHelper: NotificationHelper

    var geofenceIdFLow = MutableStateFlow(EMPTY)

    private var broadcastReceiver: GeofenceBroadcastReceiver = object : GeofenceBroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            showWhenLockedAndTurnScreenOn()
            super.onReceive(context, intent)
            Timber.d("[$GEOFENCE_LOGGER_TAG] Geofence intent received: ${intent?.getStringExtra(GEOFENCE_KEY)}")

            myAppPreferences[GEOFENCE_KEY] = intent?.getStringExtra(GEOFENCE_KEY) ?: EMPTY
            geofenceIdFLow.value = intent?.getStringExtra(GEOFENCE_KEY) ?: EMPTY

            if (geofenceIdFLow.value.isNotEmpty() && !isAppInForegrounded()) {
                notificationHelper.showFlagFoundNotification()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Timber.d("[$GEOFENCE_LOGGER_TAG] Saved value: ${myAppPreferences[GEOFENCE_KEY, EMPTY]}")

        if (myAppPreferences[GEOFENCE_KEY, EMPTY].isNotEmpty()) {
            geofenceIdFLow.value = myAppPreferences[GEOFENCE_KEY, EMPTY]
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        navigateToLoginIfNeededDuringSplashScreen()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    private fun showWhenLockedAndTurnScreenOn() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                        or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isMyServiceRunning(LocationServiceImpl::class.java) && viewModel.player.value.status != Player.Status.Playing) {
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
                    when {
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> checkBackgroundLocationPermissionAPI30(requestCode)
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> checkLocationPermissionAPI29(requestCode)
                        else -> {
                            collectScreenFlow()
                            registerReceiver(broadcastReceiver, IntentFilter(GEOFENCE_BROADCAST_RECEIVER_FILTER))
                            sendCommandToForegroundService(LocationServiceCommand.Start)
                        }
                    }
                }
                if (checkSinglePermission(ACCESS_BACKGROUND_LOCATION)) {
                    collectScreenFlow()
                    registerReceiver(broadcastReceiver, IntentFilter(GEOFENCE_BROADCAST_RECEIVER_FILTER))
                    sendCommandToForegroundService(LocationServiceCommand.Start)
                }
            } else {
                Timber.d("[$LOGGER_TAG] Permission denied: ${grantResults[0]}")
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
                    if (!viewModel.isUserLoggedIn()) {
                        startActivity(Intent(this@MainActivity, LoginActivity::class.java))
                        finish()
                    } else {
                        viewModel.observePlayer()
                        startLocationUpdates()
                    }
                }
                return@setKeepOnScreenCondition false
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
            showAlertLocation(getString(R.string.gps_enable), getString(R.string.please_turn_on_gps), getString(R.string.ok))
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

    fun onLogoutClicked() {
        startActivity(Intent(this@MainActivity, LoginActivity::class.java))
        finish()
    }

    companion object {

        private const val PERMISSION_REQUEST_CODE = 200
    }

}
