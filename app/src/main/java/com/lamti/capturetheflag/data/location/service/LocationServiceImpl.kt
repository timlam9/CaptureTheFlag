package com.lamti.capturetheflag.data.location.service

import android.content.Intent
import android.util.Log
import androidx.lifecycle.LifecycleService
import com.lamti.capturetheflag.data.location.LocationRepository
import com.lamti.capturetheflag.domain.FirestoreRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import javax.inject.Inject

@AndroidEntryPoint
class LocationServiceImpl @Inject constructor() : LifecycleService() {

    @Inject lateinit var locationRepository: LocationRepository
    @Inject lateinit var notificationHelper: NotificationHelper
    @Inject lateinit var firestoreRepository: FirestoreRepository

    private var isServiceRunning = false
    private var locationUpdates: Job? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        intent?.extras?.run {
            Log.d(TAG, "${getSerializable(SERVICE_COMMAND)} command is received")

            when (getSerializable(SERVICE_COMMAND) as LocationServiceCommand) {
                LocationServiceCommand.Start -> {
                    startFlagForegroundService()
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

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service is created")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service is destroyed")

        isServiceRunning = false
        locationUpdates?.cancel()
    }

    companion object {

        private const val TAG = "location_service"
        const val SERVICE_COMMAND = "service_command"
    }

}
