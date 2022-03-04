package com.lamti.capturetheflag.presentation.ui.activity

import androidx.lifecycle.ViewModel
import com.lamti.capturetheflag.data.authentication.AuthenticationRepository
import com.lamti.capturetheflag.presentation.ui.fragments.navigation.FragmentScreen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(authenticationRepository: AuthenticationRepository) : ViewModel() {

    val isUserLoggedIn = authenticationRepository.currentUser?.uid != null

    private val _currentScreen = MutableStateFlow(FragmentScreen.Map)
    val currentScreen: StateFlow<FragmentScreen> = _currentScreen.asStateFlow()

    fun onArBackPressed() {
        _currentScreen.value = FragmentScreen.Map
    }

    fun onSettingFlagsClicked() {
        _currentScreen.value = FragmentScreen.Ar
    }

}
