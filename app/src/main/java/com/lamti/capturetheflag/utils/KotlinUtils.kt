package com.lamti.capturetheflag.utils

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import android.location.Location
import com.google.android.gms.maps.model.LatLng
import com.google.gson.Gson

const val EMPTY = ""
const val LOGGER_TAG = "LOGGER"
const val GEOFENCE_LOGGER_TAG = "GEOFENCE_LOGGER"
const val LOCATION_LOGGER_TAG = "LOCATION_LOGGER"
const val FIRESTORE_LOGGER_TAG = "FIRESTORE_LOGGER"
const val CLOUD_ANCHOR_LOGGER_TAG = "CLOUD_ANCHOR_LOGGER"
const val SERVICE_LOCATION_LOGGER_TAG = "SERVICE_LOCATION_LOGGER"

fun emptyPosition() = LatLng(0.0, 0.0)

fun LatLng.isInRangeOf(position: LatLng, range: Float): Boolean = distanceToKm(position) <= range

fun LatLng.distanceToKm(position: LatLng): Float {
    val result = floatArrayOf(0f)
    Location.distanceBetween(
        latitude,
        longitude,
        position.latitude,
        position.longitude,
        result
    )
    return result[0]
}


// Shared Prefs extensions
val Context.myAppPreferences: SharedPreferences
    get() = getSharedPreferences(
        "${this.packageName}_${this.javaClass.simpleName}",
        MODE_PRIVATE
    )

inline fun <reified T : Any> SharedPreferences.getObject(key: String): T? {
    return Gson().fromJson<T>(getString(key, null), T::class.java)
}

@Suppress("UNCHECKED_CAST")
inline operator fun <reified T : Any> SharedPreferences.get(key: String, defaultValue: T? = null): T {
    return when (T::class) {
        Boolean::class -> getBoolean(key, defaultValue as? Boolean? ?: false) as T
        Float::class -> getFloat(key, defaultValue as? Float? ?: 0.0f) as T
        Int::class -> getInt(key, defaultValue as? Int? ?: 0) as T
        Long::class -> getLong(key, defaultValue as? Long? ?: 0L) as T
        String::class -> getString(key, defaultValue as? String? ?: "") as T
        else -> {
            if (defaultValue is Set<*>) {
                getStringSet(key, defaultValue as Set<String>) as T
            } else {
                val typeName = T::class.java.simpleName
                throw Error("Unable to get shared preference with value type '$typeName'. Use getObject")
            }
        }
    }
}


@Suppress("UNCHECKED_CAST")
inline operator fun <reified T : Any> SharedPreferences.set(key: String, value: T) {
    with(edit()) {
        when (T::class) {
            Boolean::class -> putBoolean(key, value as Boolean)
            Float::class -> putFloat(key, value as Float)
            Int::class -> putInt(key, value as Int)
            Long::class -> putLong(key, value as Long)
            String::class -> putString(key, value as String)
            else -> {
                if (value is Set<*>) {
                    putStringSet(key, value as Set<String>)
                } else {
                    val json = Gson().toJson(value)
                    putString(key, json)
                }
            }
        }
        commit()
    }
}

