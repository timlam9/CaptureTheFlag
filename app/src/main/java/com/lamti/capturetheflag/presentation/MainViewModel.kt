package com.lamti.capturetheflag.presentation

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MainViewModel: ViewModel() {

    private val _currentScreen = MutableStateFlow(Screen.Ar)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

}

enum class Screen {
    Stats,
    Map,
    Chat,
    Ar
}
