package com.lamti.capturetheflag.presentation

import androidx.lifecycle.ViewModel
import com.lamti.capturetheflag.presentation.fragments.navigation.Screen
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class MainViewModel: ViewModel() {

    private val _currentScreen = MutableStateFlow(Screen.Map)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    fun onStatsClicked() {
        _currentScreen.update { Screen.Ar }
    }

    fun onMapClicked() {
        _currentScreen.update { Screen.Map }
    }

    fun onChatClicked() {
        _currentScreen.update { Screen.Chat }
    }

}
