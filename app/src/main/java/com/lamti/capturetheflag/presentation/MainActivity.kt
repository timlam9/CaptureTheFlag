package com.lamti.capturetheflag.presentation

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.add
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import com.lamti.capturetheflag.R
import com.lamti.capturetheflag.databinding.ActivityMainBinding
import com.lamti.capturetheflag.presentation.arcore.helpers.FullScreenHelper
import com.lamti.capturetheflag.presentation.fragments.ArFragment
import com.lamti.capturetheflag.presentation.fragments.addMainFragment
import com.lamti.capturetheflag.presentation.fragments.navigateToScreen
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel = MainViewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        collectFlows(savedInstanceState)
    }

    private fun collectFlows(savedInstanceState: Bundle?) {
        viewModel.currentScreen
            .onEach { screen -> navigate(screen, savedInstanceState) }
            .launchIn(lifecycleScope)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        FullScreenHelper.setFullScreenOnWindowFocusChanged(this, hasFocus)
    }

    private fun navigate(screen: Screen, savedInstanceState: Bundle?) {
        when (savedInstanceState) {
            null -> supportFragmentManager.addMainFragment()
            else -> supportFragmentManager.navigateToScreen(screen)
        }
    }

}


