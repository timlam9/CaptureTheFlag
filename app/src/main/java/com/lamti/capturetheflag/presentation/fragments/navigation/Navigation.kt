package com.lamti.capturetheflag.presentation.fragments.navigation

import androidx.fragment.app.FragmentManager
import androidx.fragment.app.commit
import androidx.fragment.app.replace
import com.lamti.capturetheflag.R
import com.lamti.capturetheflag.presentation.fragments.ar.ArFragment
import com.lamti.capturetheflag.presentation.fragments.chat.ChatFragment
import com.lamti.capturetheflag.presentation.fragments.maps.MapFragment
import com.lamti.capturetheflag.presentation.fragments.stats.StatsFragment

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
