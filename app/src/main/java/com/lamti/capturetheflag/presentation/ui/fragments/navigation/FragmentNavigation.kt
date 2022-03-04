package com.lamti.capturetheflag.presentation.ui.fragments.navigation

import androidx.fragment.app.FragmentManager
import androidx.fragment.app.commit
import com.lamti.capturetheflag.R
import com.lamti.capturetheflag.presentation.ui.fragments.ar.ArFragment
import com.lamti.capturetheflag.presentation.ui.fragments.maps.MapFragment
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi

private const val TAG_FRAGMENT_MAP = "tag_fragment_map"
private const val TAG_FRAGMENT_AR = "tag_fragment_ar"

@ExperimentalCoroutinesApi
@InternalCoroutinesApi
fun FragmentManager.navigateToScreen(screen: FragmentScreen) {
    when (screen) {
        FragmentScreen.Map -> {
            show(TAG_FRAGMENT_MAP)
            show(TAG_FRAGMENT_AR, false)
        }
        FragmentScreen.Ar -> {
            show(TAG_FRAGMENT_MAP, false)
            show(TAG_FRAGMENT_AR)
        }
    }
}

@ExperimentalCoroutinesApi
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

@ExperimentalCoroutinesApi
@InternalCoroutinesApi
private fun FragmentManager.createFragment(tag: String) = commit {
    val fragment = when (tag) {
        TAG_FRAGMENT_MAP -> MapFragment()
        TAG_FRAGMENT_AR -> ArFragment()
        else -> MapFragment()
    }

    add(R.id.fragment_container_view, fragment, tag)
}


