package com.lamti.capturetheflag.presentation.ui.activity

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
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
import com.lamti.capturetheflag.data.location.service.isAppInForegrounded
import com.lamti.capturetheflag.data.location.service.isMyServiceRunning
import com.lamti.capturetheflag.databinding.ActivityMainBinding
import com.lamti.capturetheflag.domain.GameEngine.Companion.GREEN_FLAG_GEOFENCE_ID
import com.lamti.capturetheflag.domain.GameEngine.Companion.RED_FLAG_GEOFENCE_ID
import com.lamti.capturetheflag.domain.player.Player
import com.lamti.capturetheflag.domain.player.Team
import com.lamti.capturetheflag.presentation.arcore.helpers.FullScreenHelper
import com.lamti.capturetheflag.presentation.ui.fragments.ar.AR_MODE_KEY
import com.lamti.capturetheflag.presentation.ui.fragments.ar.ArMode
import com.lamti.capturetheflag.presentation.ui.fragments.navigation.FragmentScreen
import com.lamti.capturetheflag.presentation.ui.fragments.navigation.navigateToScreen
import com.lamti.capturetheflag.presentation.ui.login.LoginActivity
import com.lamti.capturetheflag.presentation.ui.playSound
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

    private val flagFoundSound: Uri = Uri.parse("android.resource://com.lamti.capturetheflag/" + R.raw.flag_found)

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

            if ((greenPlayerEntersUncapturedRedFlag() || redPlayerEntersUncapturedGreenFlag()) && !isAppInForegrounded()) {
                notificationHelper.showEventNotification(sound = flagFoundSound)
            }
        }
    }

    private fun redPlayerEntersUncapturedGreenFlag() =
        viewModel.player.value.gameDetails?.team == Team.Red &&
                geofenceIdFLow.value == GREEN_FLAG_GEOFENCE_ID &&
                viewModel.game.value.gameState.greenFlagCaptured == null

    private fun greenPlayerEntersUncapturedRedFlag() =
        viewModel.player.value.gameDetails?.team == Team.Green &&
                geofenceIdFLow.value == RED_FLAG_GEOFENCE_ID &&
                viewModel.game.value.gameState.redFlagCaptured == null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        navigateToLoginIfNeededDuringSplashScreen()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        lifecycleScope.launchWhenResumed {
            geofenceIdFLow.onEach {
                if ((greenPlayerEntersUncapturedRedFlag() || redPlayerEntersUncapturedGreenFlag()) && isAppInForegrounded()) {
                    playSound(sound = flagFoundSound)
                }
            }.launchIn(lifecycleScope)
        }
    }

    override fun onResume() {
        super.onResume()
        Timber.d("[$GEOFENCE_LOGGER_TAG] Saved value: ${myAppPreferences[GEOFENCE_KEY, EMPTY]}")

        if (myAppPreferences[GEOFENCE_KEY, EMPTY].isNotEmpty()) {
            geofenceIdFLow.value = myAppPreferences[GEOFENCE_KEY, EMPTY]
        }
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
                        startActivity(Intent(this@MainActivity, LoginActivity::class.java))
                        finish()
                    } else {
                        Timber.d("[$LOGGER_TAG] Start observing player with id: ${viewModel.userID}")

                        viewModel.observePlayer()
                        viewModel.startLocationService.onEach {
                            if (it) {
                                Timber.d("[$LOGGER_TAG] Start location service for user: ${viewModel.player.value.userID}")

                                collectScreenFlow()
                                registerReceiver(broadcastReceiver, IntentFilter(GEOFENCE_BROADCAST_RECEIVER_FILTER))
                                sendCommandToForegroundService(LocationServiceCommand.Start)
                            }
                        }.launchIn(lifecycleScope)
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
        Timber.d("[$LOGGER_TAG] Navigate to fragment: $screen")
        supportFragmentManager.navigateToScreen(screen)
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

}
