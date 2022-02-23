package com.lamti.capturetheflag.presentation.fragments

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory

class MainFragmentFactory : FragmentFactory() {

    override fun instantiate(classLoader: ClassLoader, className: String): Fragment =
        when (loadFragmentClass(classLoader, className)) {
            ArFragment::class.java -> ArFragment()
            else -> super.instantiate(classLoader, className)
        }

}
