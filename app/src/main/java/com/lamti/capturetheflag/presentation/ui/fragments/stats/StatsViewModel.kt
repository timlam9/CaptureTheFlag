package com.lamti.capturetheflag.presentation.ui.fragments.stats

import androidx.lifecycle.ViewModel
import com.lamti.capturetheflag.data.authentication.AuthenticationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val authenticationRepository: AuthenticationRepository
) : ViewModel() {

    fun onLogoutButtonClicked() = authenticationRepository.logout()

}
