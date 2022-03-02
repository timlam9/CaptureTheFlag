package com.lamti.capturetheflag.data.location.service

import android.os.Parcelable
import kotlinx.parcelize.Parcelize


@Parcelize
enum class LocationServiceCommand : Parcelable {

    Start,
    Pause,
    Stop
}
