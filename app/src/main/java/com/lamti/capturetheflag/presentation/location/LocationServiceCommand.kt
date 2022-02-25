package com.lamti.capturetheflag.presentation.location

import android.os.Parcelable
import kotlinx.parcelize.Parcelize


@Parcelize
enum class LocationServiceCommand : Parcelable {

    Start,
    Pause,
    Stop
}
