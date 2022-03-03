package com.lamti.capturetheflag.presentation.ui.fragments.navigation

import androidx.fragment.app.FragmentManager
import androidx.fragment.app.commit
import com.lamti.capturetheflag.R
import com.lamti.capturetheflag.presentation.ui.fragments.ar.ArFragment
import com.lamti.capturetheflag.presentation.ui.fragments.chat.ChatFragment
import com.lamti.capturetheflag.presentation.ui.fragments.maps.MapFragment
import com.lamti.capturetheflag.presentation.ui.fragments.stats.StatsFragment
import kotlinx.coroutines.InternalCoroutinesApi

private const val TAG_FRAGMENT_STATS = "tag_fragment_stats"
private const val TAG_FRAGMENT_MAP = "tag_fragment_map"
private const val TAG_FRAGMENT_CHAT = "tag_fragment_chat"
private const val TAG_FRAGMENT_AR = "tag_fragment_ar"

@InternalCoroutinesApi
fun FragmentManager.navigateToScreen(screen: FragmentScreen) {
    when (screen) {
        FragmentScreen.Stats -> {
            show(TAG_FRAGMENT_STATS)
            show(TAG_FRAGMENT_MAP, false)
            show(TAG_FRAGMENT_CHAT, false)
            show(TAG_FRAGMENT_AR, false)
        }
        FragmentScreen.Map -> {
            show(TAG_FRAGMENT_STATS, false)
            show(TAG_FRAGMENT_MAP)
            show(TAG_FRAGMENT_CHAT, false)
            show(TAG_FRAGMENT_AR, false)
        }
        FragmentScreen.Chat -> {
            show(TAG_FRAGMENT_STATS, false)
            show(TAG_FRAGMENT_MAP, false)
            show(TAG_FRAGMENT_CHAT)
            show(TAG_FRAGMENT_AR, false)
        }
        FragmentScreen.Ar -> {
            show(TAG_FRAGMENT_STATS, false)
            show(TAG_FRAGMENT_MAP, false)
            show(TAG_FRAGMENT_CHAT, false)
            show(TAG_FRAGMENT_AR)
        }
    }
}

@InternalCoroutinesApi
private fun FragmentManager.show(tag: String, show: Boolean = true) = commit {
    if (findFragmentByTag(tag) == null && !show) return
    setReorderingAllowed(true)

    when (findFragmentByTag(tag) == null && show) {
        true -> createFragment(tag)
        false -> when (show) {
            true -> show(findFragmentByTag(tag)!!)
            false -> hide(findFragmentByTag(tag)!!)
        }
    }
}

@InternalCoroutinesApi
private fun FragmentManager.createFragment(tag: String) = commit {
    val fragment = when (tag) {
        TAG_FRAGMENT_STATS -> StatsFragment()
        TAG_FRAGMENT_MAP -> MapFragment()
        TAG_FRAGMENT_CHAT -> ChatFragment()
        TAG_FRAGMENT_AR -> ArFragment()
        else -> MapFragment()
    }

    add(R.id.fragment_container_view, fragment, tag)
}


