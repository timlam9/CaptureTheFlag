package com.lamti.capturetheflag.presentation.ui.fragments.maps

import com.google.android.gms.maps.model.LatLng

val testSafeHousePosition = LatLng(37.930208, 23.713045)
val testGreenFlagPosition = LatLng(37.931375288733555, 23.71944793177204)
val testRedFlagPosition = LatLng(37.93366241100668, 23.706517488109547)

const val DEFAULT_GAME_BOUNDARIES_RADIUS = 750f
const val DEFAULT_SAFEHOUSE_RADIUS = 100f
const val DEFAULT_FLAG_RADIUS = 40f

sealed class GameState {

    data class Started(
        val safeHousePosition: LatLng = testSafeHousePosition,
        val greenFlagPosition: LatLng = testGreenFlagPosition,
        val redFlagPosition: LatLng = testRedFlagPosition,
    ) : GameState()

}
