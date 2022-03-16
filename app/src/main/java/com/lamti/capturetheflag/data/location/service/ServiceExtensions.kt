package com.lamti.capturetheflag.data.location.service

import android.Manifest
import android.annotation.TargetApi
import android.app.Activity
import android.app.ActivityManager
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat

fun Activity.isMyServiceRunning(serviceClass: Class<*>): Boolean {
    val manager: ActivityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    for (service in manager.getRunningServices(Int.MAX_VALUE)) {
        if (serviceClass.name == service.service.className) {
            return true
        }
    }
    return false
}

fun isLocationEnabledOrNot(context: Context): Boolean {
    val locationManager: LocationManager? = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager?
    return locationManager!!.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
        LocationManager.NETWORK_PROVIDER
    )
}

fun Activity.showAlertLocation(title: String, message: String, btnText: String) {
    val alertDialog = AlertDialog.Builder(this).create()
    alertDialog.setTitle(title)
    alertDialog.setMessage(message)
    alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, btnText) { dialog, _ ->
        dialog.dismiss()
        startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
    }
    alertDialog.show()
}

@RequiresApi(Build.VERSION_CODES.Q)
fun Activity.checkLocationPermissionAPI29(locationRequestCode: Int) {
    if (checkSinglePermission(Manifest.permission.ACCESS_FINE_LOCATION) &&
        checkSinglePermission(Manifest.permission.ACCESS_COARSE_LOCATION) &&
        checkSinglePermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
    ) return
    val permList = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_BACKGROUND_LOCATION
    )
    requestPermissions(permList, locationRequestCode)
}

@TargetApi(Build.VERSION_CODES.R)
fun Activity.checkBackgroundLocationPermissionAPI30(backgroundLocationRequestCode: Int) {
    if (checkSinglePermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION)) return
    androidx.appcompat.app.AlertDialog.Builder(this)
        .setTitle("Background Permission")
        .setMessage("\'All the time\' background location permission is needed")
        .setPositiveButton("Yes") { _, _ ->
            requestPermissions(arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION), backgroundLocationRequestCode)
        }
        .setNegativeButton("No") { dialog, _ ->
            dialog.dismiss()
        }
        .create()
        .show()

}

fun Context.checkSinglePermission(permission: String): Boolean {
    return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
}
