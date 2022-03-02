package com.lamti.capturetheflag.data.location.service

import android.content.Intent
import android.util.Log
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.lamti.capturetheflag.data.location.LocationRepository
import com.lamti.capturetheflag.data.location.geofences.GeofencingRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@InternalCoroutinesApi
@ExperimentalCoroutinesApi
@AndroidEntryPoint
class LocationServiceImpl @Inject constructor() : LifecycleService() {

    @Inject lateinit var locationRepository: LocationRepository
    @Inject lateinit var notificationHelper: NotificationHelper
    @Inject lateinit var geofencingRepository: GeofencingRepository

    private var isServiceRunning = false
    private var locationUpdates: Job? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        intent?.extras?.run {
            Log.d(TAG, "${getSerializable(SERVICE_COMMAND)} command is received")

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
        locationUpdates = locationRepository.locationFlow()
            .onEach { notificationHelper.updateNotification("Position: ${it.latitude}, ${it.longitude}") }
            .flowOn(Dispatchers.IO)
            .launchIn(lifecycleScope)
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service is created")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service is destroyed")

        isServiceRunning = false
        locationUpdates?.cancel()
        removeGeofencesListener()
    }

    private fun removeGeofencesListener() {
        geofencingRepository.removeGeofences()
    }

    companion object {

        private const val TAG = "location_service"
        const val SERVICE_COMMAND = "service_command"
    }

}
