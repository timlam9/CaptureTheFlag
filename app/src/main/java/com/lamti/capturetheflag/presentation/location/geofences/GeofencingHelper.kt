package com.lamti.capturetheflag.presentation.location.geofences

import com.google.android.gms.maps.model.LatLng


interface GeofencingHelper {

    fun addGeofences()

    fun removeGeofences()

    fun addGeofence(position: LatLng, id: String, radius: Float)

}
