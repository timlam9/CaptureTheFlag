package com.lamti.capturetheflag.presentation.ui.fragments.maps

import com.google.android.gms.maps.model.LatLng
import com.lamti.capturetheflag.utils.emptyPosition

const val DEFAULT_GAME_BOUNDARIES_RADIUS = 750f
const val DEFAULT_SAFEHOUSE_RADIUS = 100f
const val DEFAULT_FLAG_RADIUS = 40f

sealed class GameUiState {

    data class Started(
        val safeHousePosition: LatLng = emptyPosition(),
        val greenFlagPosition: LatLng = emptyPosition(),
        val redFlagPosition: LatLng = emptyPosition(),
        val isGreenFlagFound: Boolean = false,
        val isRedFlagFound: Boolean = false,
    ) : GameUiState()

}
