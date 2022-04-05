package com.lamti.capturetheflag.data.location

import android.annotation.SuppressLint
import android.location.Location
import android.os.Looper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber
import javax.inject.Inject
import kotlin.coroutines.resumeWithException

@SuppressLint("MissingPermission")
class LocationRepository @Inject constructor(
    private val client: FusedLocationProviderClient,
    private val scope: CoroutineScope
) {

    suspend fun awaitLastLocation(): Location = with(client) {
        suspendCancellableCoroutine { continuation ->
            lastLocation.addOnSuccessListener { location ->
                continuation.resume(location) { }
            }.addOnFailureListener { e ->
                continuation.resumeWithException(e)
            }
        }
    }

    fun locationFlow() = with(client) {
        callbackFlow<Location> {
            val callback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    result ?: return
                    for (location in result.locations) {
                        try {
                            trySend(location)
                        } catch (t: Throwable) {
                            Timber.e("Location error: ${t.message}")
                        }
                    }
                }
            }
            val locationRequest = LocationRequest.create().apply {
                interval = 100
                fastestInterval = 50
                priority = LocationRequest.PRIORITY_HIGH_ACCURACY
                maxWaitTime = 100
            }

            requestLocationUpdates(
                locationRequest,
                callback,
                Looper.getMainLooper()
            ).addOnFailureListener { e ->
                close(e)
            }

            awaitClose {
                removeLocationUpdates(callback)
            }
        }.shareIn(
            scope = scope,
            replay = 1,
            started = SharingStarted.WhileSubscribed()
        )
    }
}
