package com.lamti.capturetheflag.presentation.ui.activity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lamti.capturetheflag.data.authentication.AuthenticationRepository
import com.lamti.capturetheflag.domain.FirestoreRepository
import com.lamti.capturetheflag.domain.game.Game
import com.lamti.capturetheflag.domain.game.Game.Companion.initialGame
import com.lamti.capturetheflag.domain.player.Player
import com.lamti.capturetheflag.domain.player.Player.Companion.emptyPlayer
import com.lamti.capturetheflag.presentation.ui.fragments.navigation.FragmentScreen
import com.lamti.capturetheflag.utils.LOGGER_TAG
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val authenticationRepository: AuthenticationRepository,
    private val firestoreRepository: FirestoreRepository
) : ViewModel() {

    val isUserLoggedIn
        get() = authenticationRepository.getCurrentUser()?.uid != null

    val userID: String?
        get() = authenticationRepository.getCurrentUser()?.uid

    private val _currentScreen = MutableStateFlow(FragmentScreen.Map)
    val currentScreen: StateFlow<FragmentScreen> = _currentScreen.asStateFlow()

    private val _player = MutableStateFlow(emptyPlayer())
    val player: StateFlow<Player> = _player.asStateFlow()

    private val _game = MutableStateFlow(initialGame())
    val game: StateFlow<Game> = _game.asStateFlow()

    private val _startLocationService = MutableStateFlow(false)
    val startLocationService: StateFlow<Boolean> = _startLocationService.asStateFlow()

    fun onArBackPressed() {
        _currentScreen.value = FragmentScreen.Map
    }

    fun onSettingFlagsClicked() {
        _currentScreen.value = FragmentScreen.Ar
    }

    fun onArScannerButtonClicked() {
        _currentScreen.value = FragmentScreen.Ar
    }

    fun observePlayer() = firestoreRepository
        .observePlayer()
        .onEach {
            Timber.d("[$LOGGER_TAG] Player updated: $it")
            _player.value = it
            if (it.userID.isNotEmpty()) _startLocationService.update { true }
            if (it.gameDetails?.gameID?.isNotEmpty() == true) {
                _startLocationService.update { true }
                observeGame(it.gameDetails.gameID)
            }
        }
        .launchIn(viewModelScope)

    private fun observeGame(gameID: String) = viewModelScope.launch {
        firestoreRepository
            .observeGame(gameID)
            .onEach { _game.value = it }
            .launchIn(viewModelScope)
    }

}
