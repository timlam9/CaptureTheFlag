package com.lamti.capturetheflag.data.location.geofences

import android.annotation.SuppressLint
import android.app.PendingIntent
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.maps.model.LatLng
import timber.log.Timber
import javax.inject.Inject

class GeofencingRepository @Inject constructor(
    private val geofencingClient: GeofencingClient,
    private val geofencePendingIntent: PendingIntent
) {

    private val geofenceList: MutableList<Geofence> = mutableListOf()

    fun addGeofence(position: LatLng, id: String, radius: Float) {
        geofenceList.add(
            Geofence.Builder()
                .setRequestId(id)
                .setCircularRegion(
                    position.latitude,
                    position.longitude,
                    radius
                )
                .setExpirationDuration(GEOFENCE_EXPIRATION_IN_MILLISECONDS)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT)
                .build()
        )
    }

    @SuppressLint("MissingPermission")
    fun addGeofences() {
        geofencingClient.addGeofences(getGeofencingRequest(), geofencePendingIntent).run {
            addOnSuccessListener {
                Timber.d("Geofence is added")
            }
            addOnFailureListener {
                Timber.e("Geofence failed to be added: ${it.message}")
            }
        }
    }

    fun removeGeofences() {
        geofencingClient.removeGeofences(geofencePendingIntent).run {
            addOnSuccessListener {
                Timber.d("Geofence is removed")
            }
            addOnFailureListener {
                Timber.e("Geofence failed to be removed: ${it.message}")
            }
        }
    }

    private fun getGeofencingRequest(): GeofencingRequest {
        return GeofencingRequest.Builder().apply {
            setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            addGeofences(geofenceList)
        }.build()
    }

    companion object {

        const val GEOFENCE_EXPIRATION_IN_MILLISECONDS: Long = 3 * 60 * 1000
    }
}
