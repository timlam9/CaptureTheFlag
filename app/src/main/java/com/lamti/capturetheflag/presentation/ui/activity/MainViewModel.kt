package com.lamti.capturetheflag.presentation.ui.activity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lamti.capturetheflag.data.authentication.AuthenticationRepository
import com.lamti.capturetheflag.domain.FirestoreRepository
import com.lamti.capturetheflag.domain.player.Player
import com.lamti.capturetheflag.domain.player.Player.Companion.emptyPlayer
import com.lamti.capturetheflag.presentation.ui.fragments.navigation.FragmentScreen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val authenticationRepository: AuthenticationRepository,
    private val firestoreRepository: FirestoreRepository
) : ViewModel() {

    fun isUserLoggedIn() = authenticationRepository.getCurrentUser()?.uid != null

    private val _currentScreen = MutableStateFlow(FragmentScreen.Map)
    val currentScreen: StateFlow<FragmentScreen> = _currentScreen.asStateFlow()

    private val _player = MutableStateFlow(emptyPlayer())
    val player: StateFlow<Player> = _player.asStateFlow()

    fun onArBackPressed() {
        _currentScreen.value = FragmentScreen.Map
    }

    fun onSettingFlagsClicked() {
        _currentScreen.value = FragmentScreen.Ar
    }

    fun onArScannerButtonClicked() {
        _currentScreen.value = FragmentScreen.Ar
    }

    fun observePlayer() {
        firestoreRepository.observePlayer().onEach {
            _player.value = it
        }.launchIn(viewModelScope)
    }

}
