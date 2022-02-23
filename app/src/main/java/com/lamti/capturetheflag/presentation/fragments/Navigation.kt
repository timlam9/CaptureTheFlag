package com.lamti.capturetheflag.presentation.fragments

import androidx.fragment.app.FragmentManager
import androidx.fragment.app.add
import androidx.fragment.app.commit
import com.lamti.capturetheflag.R
import com.lamti.capturetheflag.presentation.Screen

fun FragmentManager.addMainFragment() {
    commit {
        setReorderingAllowed(true)
        add<ArFragment>(R.id.fragment_container_view)
    }
}

fun FragmentManager.navigateToScreen(screen: Screen) {
    val fragment = when (screen) {
        Screen.Stats -> TODO()
        Screen.Map -> TODO()
        Screen.Chat -> TODO()
        Screen.Ar -> findFragmentById(R.id.fragment_container_view) as ArFragment
    }
    commit {
        replace(R.id.fragment_container_view, fragment)
        setReorderingAllowed(true)
        addToBackStack(null)
    }
}
