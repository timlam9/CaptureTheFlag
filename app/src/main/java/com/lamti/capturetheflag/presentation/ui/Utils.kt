package com.lamti.capturetheflag.presentation.ui

import android.content.Context
import android.graphics.Bitmap
import android.location.Location
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.navigation.NavHostController
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.lamti.capturetheflag.utils.EMPTY

fun Int.secondsToTime(): String {
    val hours = this / 3600
    val minutes = (this % 3600) / 60
    val seconds = this % 60

    return if (hours == 0) {
        String.format("%02d:%02d", minutes, seconds)
    } else {
        String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }
}

fun Context.bitmapDescriptorFromVector(vectorResId: Int, @ColorRes tintColor: Int? = null): BitmapDescriptor? {
    // retrieve the actual drawable
    val drawable = ContextCompat.getDrawable(this, vectorResId) ?: return null
    drawable.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
    val bm = Bitmap.createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)

    // add the tint if it exists
    tintColor?.let {
        DrawableCompat.setTint(drawable, ContextCompat.getColor(this, it))
    }
    // draw it onto the bitmap
    val canvas = android.graphics.Canvas(bm)
    drawable.draw(canvas)
    return BitmapDescriptorFactory.fromBitmap(bm)
}

fun getRandomString(length: Int): String {
    val allowedChars = ('A'..'Z') + ('a'..'z') + ('0'..'9')
    return (1..length)
        .map { allowedChars.random() }
        .joinToString(EMPTY)
}

fun Location.toLatLng() = LatLng(latitude, longitude)

const val DEFAULT_GAME_BOUNDARIES_RADIUS = 750f
const val DEFAULT_SAFEHOUSE_RADIUS = 100f
const val DEFAULT_FLAG_RADIUS = 40f

fun NavHostController.popNavigate(to: String) {
    navigate(to) {
        popUpTo(0) {
            inclusive = true
        }
    }
}
