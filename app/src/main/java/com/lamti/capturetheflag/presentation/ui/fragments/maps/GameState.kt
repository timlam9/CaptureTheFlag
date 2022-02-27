package com.lamti.capturetheflag.presentation.ui.fragments.maps

import com.google.android.gms.maps.model.LatLng

val testSafeHousePosition = LatLng(37.930208, 23.713045)
val testFlagPosition = LatLng(37.931375288733555, 23.71944793177204)
val testOpponentFlagPosition = LatLng(37.93366241100668, 23.706517488109547)

val testSafeHouseRadius = 100f
val testFlagRadius = 40f

sealed class GameState {

    data class Started(
        val safeHousePosition: LatLng = testSafeHousePosition,
        val flagPosition: LatLng = testFlagPosition,
        val opponentFlagPosition: LatLng = testOpponentFlagPosition,
    ) : GameState()

}
