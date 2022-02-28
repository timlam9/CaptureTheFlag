package com.lamti.capturetheflag.presentation.location.service

import android.content.Intent
import android.util.Log
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.lamti.capturetheflag.presentation.location.locationFlow
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@ExperimentalCoroutinesApi
@AndroidEntryPoint
class LocationServiceImpl @Inject constructor() : LifecycleService(), LocationService {

    @Inject lateinit var fusedLocationClient: FusedLocationProviderClient
    @Inject lateinit var notificationHelper: NotificationHelper

    private var isServiceRunning = false
    private var locationUpdates: Job? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        intent?.extras?.run {
            Log.d(TAG,"${getSerializable(SERVICE_COMMAND)} command is received")

            when (getSerializable(SERVICE_COMMAND) as LocationServiceCommand) {
                LocationServiceCommand.Start -> {
                    startFlagForegroundService()
                    startLocationUpdates()
                }
                LocationServiceCommand.Pause -> {
                    isServiceRunning = false
                    locationUpdates?.cancel()
                }
                LocationServiceCommand.Stop -> {
                    isServiceRunning = false
                    locationUpdates?.cancel()
                    stopSelf()
                    return START_NOT_STICKY
                }
            }
        }

        return START_STICKY
    }

    private fun startFlagForegroundService() {
        if (!isServiceRunning) startForeground(NotificationHelper.NOTIFICATION_ID, notificationHelper.getNotification())
        isServiceRunning = true
    }

    private fun startLocationUpdates() {
        locationUpdates = fusedLocationClient.locationFlow()
            .onEach { notificationHelper.updateNotification("Position: ${it.latitude}, ${it.longitude}") }
            .flowOn(Dispatchers.IO)
            .launchIn(lifecycleScope)
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG,"Service is created")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG,"Service is destroyed")
    }

    companion object {

        private const val TAG = "location_service" 
        const val SERVICE_COMMAND = "service_command"
    }

}
