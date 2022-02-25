package com.lamti.capturetheflag.presentation.ui.fragments.navigation

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
import com.lamti.capturetheflag.presentation.ui.fragments.ar.ArFragment
import com.lamti.capturetheflag.presentation.ui.fragments.chat.ChatFragment
import com.lamti.capturetheflag.presentation.ui.fragments.maps.MapFragment
import com.lamti.capturetheflag.presentation.ui.fragments.stats.StatsFragment

class MainFragmentFactory : FragmentFactory() {

    override fun instantiate(classLoader: ClassLoader, className: String): Fragment =
        when (loadFragmentClass(classLoader, className)) {
            StatsFragment::class.java -> StatsFragment()
            MapFragment::class.java -> MapFragment()
            ChatFragment::class.java -> ChatFragment()
            ArFragment::class.java -> ArFragment()
            else -> super.instantiate(classLoader, className)
        }

}
