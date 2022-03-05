package com.lamti.capturetheflag.presentation.ui.fragments.maps

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.maps.MapsInitializer
import com.lamti.capturetheflag.R
import com.lamti.capturetheflag.databinding.FragmentMapBinding
import com.lamti.capturetheflag.presentation.ui.activity.MainActivity
import com.lamti.capturetheflag.presentation.ui.components.GameNavigation
import com.lamti.capturetheflag.presentation.ui.fragments.ar.AR_MODE_KEY
import com.lamti.capturetheflag.presentation.ui.style.CaptureTheFlagTheme
import com.lamti.capturetheflag.utils.EMPTY
import com.lamti.capturetheflag.utils.myAppPreferences
import com.lamti.capturetheflag.utils.set
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@ExperimentalCoroutinesApi
@InternalCoroutinesApi
@AndroidEntryPoint
@SuppressLint("UnspecifiedImmutableFlag")
class MapFragment : Fragment(R.layout.fragment_map) {

    private var binding: FragmentMapBinding? = null
    private val viewModel: MapViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentMapBinding.bind(view)

        initializeDataOnSplashScreen()
        observeArMode()
        setupMapView()
    }

    private fun observeArMode() {
        lifecycleScope.launchWhenStarted {
            viewModel.arMode.onEach { requireActivity().myAppPreferences[AR_MODE_KEY] = it.name }.launchIn(lifecycleScope)
        }
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }

    private fun initializeDataOnSplashScreen() {
        requireActivity().installSplashScreen().apply {
            setKeepOnScreenCondition {
                viewModel.getLastLocation()
                viewModel.observePlayer()
                if (viewModel.player.value.gameDetails != null && viewModel.player.value.gameDetails?.gameID != EMPTY)
                    viewModel.observeGameState(viewModel.player.value.gameDetails!!.gameID )
                return@setKeepOnScreenCondition viewModel.stayInSplashScreen.value
            }
        }
    }

    private fun setupMapView() = binding?.run {
        MapsInitializer.initialize(requireContext())

        mapComposeView.setContent {
            CaptureTheFlagTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colors.background) {
                    GameNavigation(viewModel) {
                        (requireActivity() as MainActivity).onSettingFlagsClicked()
                    }
                }
            }
        }
    }
}
