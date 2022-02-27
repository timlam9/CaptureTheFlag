package com.lamti.capturetheflag.presentation.location.geofences

import android.app.PendingIntent
import com.google.android.gms.maps.model.LatLng


interface GeofencingHelper {

    fun addGeofences(geofencePendingIntent: PendingIntent)

    fun removeGeofences(geofencePendingIntent: PendingIntent)

    fun addGeofence(position: LatLng, id: String, radius: Float)

}
