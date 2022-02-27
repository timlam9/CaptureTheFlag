package com.lamti.capturetheflag.presentation.location.geofences

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingEvent

class GeofenceBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        handleIntent(intent ?: return, context ?: return)
    }

    private fun handleIntent(intent: Intent, context: Context) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent)
        if (geofencingEvent.hasError()) {
            val errorMessage = GeofenceStatusCodes.getStatusCodeString(geofencingEvent.errorCode)
            Log.e(TAG, errorMessage)
            return
        }

        val geofenceTransition = geofencingEvent.geofenceTransition

        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER ||
            geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT
        ) {
            val triggeringGeofences = geofencingEvent.triggeringGeofences
            val geofenceTransitionDetails = getGeofenceTransitionDetails(
                geofenceTransition,
                triggeringGeofences
            )

            Toast.makeText(context, geofenceTransitionDetails, Toast.LENGTH_SHORT).show()
            Log.i(TAG, geofenceTransitionDetails)
        } else {
            Log.e(TAG, "invalid type: $geofenceTransition")
        }
    }

    private fun getGeofenceTransitionDetails(
        geofenceTransition: Int,
        triggeringGeofences: List<Geofence>
    ): String {
        val action = when (geofenceTransition) {
            1 -> "Enter"
            2 -> "Exit"
            else -> "other"
        }
        return "$action \"${triggeringGeofences[0].requestId}\" geofence"
    }

    companion object {

        const val TAG = "TAGARA_GEOFENCE"
    }

}
