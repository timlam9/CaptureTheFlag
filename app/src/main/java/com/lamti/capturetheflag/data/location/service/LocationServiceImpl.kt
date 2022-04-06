package com.lamti.capturetheflag.data.location.service

import android.content.Intent
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.lamti.capturetheflag.data.location.LocationRepository
import com.lamti.capturetheflag.domain.FirestoreRepository
import com.lamti.capturetheflag.presentation.ui.toLatLng
import com.lamti.capturetheflag.utils.SERVICE_LOCATION_LOGGER_TAG
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import timber.log.Timber
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
            Timber.d("[$SERVICE_LOCATION_LOGGER_TAG] ${getSerializable(SERVICE_COMMAND)} command is received")

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

    override fun onCreate() {
        super.onCreate()
        Timber.d("[$SERVICE_LOCATION_LOGGER_TAG] Location Service is created")
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.d("[$SERVICE_LOCATION_LOGGER_TAG] Location Service is destroyed")

        isServiceRunning = false
        locationUpdates?.cancel()
    }

    private fun startFlagForegroundService() {
        if (!isServiceRunning) startForeground(NotificationHelper.NOTIFICATION_ID, notificationHelper.getNotification())
        isServiceRunning = true
    }

    private fun startLocationUpdates() {
        locationUpdates = locationRepository.locationFlow()
            .onEach {
                firestoreRepository.uploadGamePlayer(it.toLatLng())
            }
            .flowOn(Dispatchers.IO)
            .launchIn(lifecycleScope)
    }

    companion object {

        const val SERVICE_COMMAND = "service_command"
    }

}
