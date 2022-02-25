package com.lamti.capturetheflag.presentation.location

import android.annotation.SuppressLint
import android.location.Location
import android.os.Looper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resumeWithException

@ExperimentalCoroutinesApi
@SuppressLint("MissingPermission")
suspend fun FusedLocationProviderClient.awaitLastLocation(): Location =
    // Create a new coroutine that can be cancelled
    suspendCancellableCoroutine<Location> { continuation ->

        // Add listeners that will resume the execution of this coroutine
        lastLocation.addOnSuccessListener { location ->
            // Resume coroutine and return location
            continuation.resume(location) {

            }
        }.addOnFailureListener { e ->
            // Resume the coroutine by throwing an exception
            continuation.resumeWithException(e)
        }

        // End of the suspendCancellableCoroutine block. This suspends the
        // coroutine until one of the callbacks calls the continuation parameter.
    }

// Send location updates to the consumer
@ExperimentalCoroutinesApi
@SuppressLint("MissingPermission")
fun FusedLocationProviderClient.locationFlow() = callbackFlow<Location> {
    // A new Flow is created. This code executes in a coroutine!

    // 1. Create callback and add elements into the flow
    val callback = object : LocationCallback() {

        override fun onLocationResult(result: LocationResult) {
            result ?: return // Ignore null responses

            for (location in result.locations) {
                try {
                    trySend(location) // Send location to the flow
                } catch (t: Throwable) {
                    // Location couldn't be sent to the flow
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

    // 2. Register the callback to get location updates by calling requestLocationUpdates
    requestLocationUpdates(
        locationRequest,
        callback,
        Looper.getMainLooper()
    ).addOnFailureListener { e ->
        close(e) // in case of error, close the Flow
    }

    // 3. Wait for the consumer to cancel the coroutine and unregister
    // the callback. This suspends the coroutine until the Flow is closed.
    awaitClose {
        // Clean up code goes here
        removeLocationUpdates(callback)
    }
}.shareIn(
    // Make the flow follow the applicationScope
    scope = applicationScope,
    // Emit the last emitted element to new collectors
    replay = 1,
    // Keep the producer active while there are active subscribers
    started = SharingStarted.WhileSubscribed()
)

val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
