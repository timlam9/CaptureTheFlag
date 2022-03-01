package com.lamti.capturetheflag.presentation.ui.fragments.navigation

import androidx.fragment.app.FragmentManager
import androidx.fragment.app.commit
import androidx.fragment.app.replace
import com.lamti.capturetheflag.R
import com.lamti.capturetheflag.presentation.ui.fragments.ar.ArFragment
import com.lamti.capturetheflag.presentation.ui.fragments.chat.ChatFragment
import com.lamti.capturetheflag.presentation.ui.fragments.maps.MapFragment
import com.lamti.capturetheflag.presentation.ui.fragments.stats.StatsFragment
import kotlinx.coroutines.InternalCoroutinesApi

@InternalCoroutinesApi
fun FragmentManager.navigateToScreen(screen: Screen) {
    commit {
        setReorderingAllowed(true)
         when (screen) {
             Screen.Stats -> replace<StatsFragment>(R.id.fragment_container_view)
            Screen.Map -> replace<MapFragment>(R.id.fragment_container_view)
            Screen.Chat -> replace<ChatFragment>(R.id.fragment_container_view)
            Screen.Ar -> replace<ArFragment>(R.id.fragment_container_view)
        }
    }
}
