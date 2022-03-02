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

private const val TAG_FRAGMENT_STATS = "tag_fragment_stats"
private const val TAG_FRAGMENT_MAP = "tag_fragment_map"
private const val TAG_FRAGMENT_CHAT = "tag_fragment_chat"
private const val TAG_FRAGMENT_AR = "tag_fragment_ar"

@InternalCoroutinesApi
fun FragmentManager.navigateToScreen(screen: Screen) {
    commit {
        setReorderingAllowed(true)
        when (screen) {
            Screen.Stats -> showFragment(TAG_FRAGMENT_STATS)
            Screen.Map -> showFragment(TAG_FRAGMENT_MAP)
            Screen.Chat -> showFragment(TAG_FRAGMENT_CHAT)
            Screen.Ar -> showFragment(TAG_FRAGMENT_AR)
        }
    }
}

@InternalCoroutinesApi
private fun FragmentManager.showFragment(tag: String) {
    when (tag) {
        TAG_FRAGMENT_STATS -> {
            show(TAG_FRAGMENT_STATS)
            show(TAG_FRAGMENT_MAP, false)
            show(TAG_FRAGMENT_CHAT, false)
            show(TAG_FRAGMENT_AR, false)
        }
        TAG_FRAGMENT_MAP -> {
            show(TAG_FRAGMENT_STATS, false)
            show(TAG_FRAGMENT_MAP)
            show(TAG_FRAGMENT_CHAT, false)
            show(TAG_FRAGMENT_AR, false)
        }
        TAG_FRAGMENT_CHAT -> {
            show(TAG_FRAGMENT_STATS, false)
            show(TAG_FRAGMENT_MAP, false)
            show(TAG_FRAGMENT_CHAT)
            show(TAG_FRAGMENT_AR, false)
        }
        TAG_FRAGMENT_AR -> {
            show(TAG_FRAGMENT_STATS, false)
            show(TAG_FRAGMENT_MAP, false)
            show(TAG_FRAGMENT_CHAT, false)
            show(TAG_FRAGMENT_AR)
        }
    }
}

@InternalCoroutinesApi
private fun FragmentManager.show(tag: String, show: Boolean = true) {
    if (findFragmentByTag(tag) == null && !show) return

    when (findFragmentByTag(tag) == null && show) {
        true -> createFragment(tag)
        false -> when (show) {
            true -> beginTransaction().show(findFragmentByTag(tag)!!).commit()
            false -> beginTransaction().hide(findFragmentByTag(tag)!!).commit()
        }
    }
}

@InternalCoroutinesApi
private fun FragmentManager.createFragment(tag: String) {
    val fragment = when (tag) {
        TAG_FRAGMENT_STATS -> StatsFragment()
        TAG_FRAGMENT_MAP -> MapFragment()
        TAG_FRAGMENT_CHAT -> ChatFragment()
        TAG_FRAGMENT_AR -> ArFragment()
        else -> MapFragment()
    }
    beginTransaction().add(R.id.fragment_container_view, fragment, tag).commit()
}


