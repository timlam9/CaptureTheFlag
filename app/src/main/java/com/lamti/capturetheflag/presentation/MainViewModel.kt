package com.lamti.capturetheflag.presentation

import androidx.lifecycle.ViewModel
import com.lamti.capturetheflag.presentation.fragments.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(): ViewModel() {

    private val _currentScreen = MutableStateFlow(Screen.Map)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    fun onStatsClicked() {
        _currentScreen.update { Screen.Stats }
    }

    fun onMapClicked() {
        _currentScreen.update { Screen.Map }
    }

    fun onChatClicked() {
        _currentScreen.update { Screen.Chat }
    }

}
