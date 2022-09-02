package com.lamti.capturetheflag.presentation.ui.fragments.navigation

import androidx.fragment.app.FragmentManager
import androidx.fragment.app.commit
import com.lamti.capturetheflag.R
import com.lamti.capturetheflag.presentation.ui.fragments.ar.ArFragment
import com.lamti.capturetheflag.presentation.ui.fragments.maps.MapFragment
import com.lamti.capturetheflag.presentation.ui.fragments.noGps.NoGpsFragment

private const val TAG_FRAGMENT_NO_GPS = "tag_fragment_no_gps"
private const val TAG_FRAGMENT_MAP = "tag_fragment_map"
private const val TAG_FRAGMENT_AR = "tag_fragment_ar"

fun FragmentManager.navigateToScreen(screen: FragmentScreen) {
    when (screen) {
        FragmentScreen.Map -> {
            show(TAG_FRAGMENT_MAP)
            show(TAG_FRAGMENT_AR, show = false, destroy = true)
            show(TAG_FRAGMENT_NO_GPS, show = false, destroy = true)
        }
        FragmentScreen.Ar -> {
            show(TAG_FRAGMENT_MAP, false)
            show(TAG_FRAGMENT_AR)
            show(TAG_FRAGMENT_NO_GPS, show = false, destroy = true)
        }
        FragmentScreen.NoGps -> {
            show(TAG_FRAGMENT_MAP, show = false, destroy = true)
            show(TAG_FRAGMENT_AR, show = false, destroy = true)
            show(TAG_FRAGMENT_NO_GPS)
        }
    }
}

private fun FragmentManager.show(tag: String, show: Boolean = true, destroy: Boolean = false) = commit {
    if (findFragmentByTag(tag) == null && !show) return
    setReorderingAllowed(true)

    when (findFragmentByTag(tag) == null && show) {
        true -> createFragment(tag)
        false -> when (show) {
            true -> show(findFragmentByTag(tag)!!)
            false -> if (destroy) remove(findFragmentByTag(tag)!!) else hide(findFragmentByTag(tag)!!)
        }
    }
}

private fun FragmentManager.createFragment(tag: String) = commit {
    val fragment = when (tag) {
        TAG_FRAGMENT_MAP -> MapFragment()
        TAG_FRAGMENT_AR -> ArFragment()
        TAG_FRAGMENT_NO_GPS -> NoGpsFragment()
        else -> MapFragment()
    }

    add(R.id.fragment_container_view, fragment, tag)
}


