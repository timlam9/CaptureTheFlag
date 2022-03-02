package com.lamti.capturetheflag.data.location.service

import android.app.Activity
import android.app.ActivityManager
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.provider.Settings

fun isMyServiceRunning(serviceClass: Class<*>, mActivity: Activity): Boolean {
    val manager: ActivityManager = mActivity.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
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

fun showAlertLocation(context: Context, title: String, message: String, btnText: String) {
    val alertDialog = AlertDialog.Builder(context).create()
    alertDialog.setTitle(title)
    alertDialog.setMessage(message)
    alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, btnText) { dialog, _ ->
        dialog.dismiss()
        context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
    }
    alertDialog.show()
}
