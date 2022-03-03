package com.lamti.capturetheflag.presentation.ui.fragments.maps

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.android.gms.maps.MapsInitializer
import com.lamti.capturetheflag.R
import com.lamti.capturetheflag.databinding.FragmentMapBinding
import com.lamti.capturetheflag.presentation.ui.components.MenuScreen
import com.lamti.capturetheflag.presentation.ui.components.map.MapScreen
import com.lamti.capturetheflag.presentation.ui.style.CaptureTheFlagTheme
import com.lamti.capturetheflag.utils.EMPTY
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi

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
        setupMapView()
    }

    private fun initializeDataOnSplashScreen() {
        requireActivity().installSplashScreen().apply {
            setKeepOnScreenCondition {
                viewModel.getLastLocation()
                viewModel.observePlayer()
                return@setKeepOnScreenCondition viewModel.stayInSplashScreen.value
            }
        }
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }

    private fun setupMapView() = binding?.run {
        MapsInitializer.initialize(requireContext())

        mapComposeView.setContent {
            CaptureTheFlagTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    GameNavigation(viewModel)
                }
            }
        }
    }
}

@InternalCoroutinesApi
@ExperimentalCoroutinesApi
@Composable
fun GameNavigation(viewModel: MapViewModel) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = viewModel.currentScreen.value.route,
    ) {
        composable(route = Screen.Menu.route) {
            MenuScreen(
                onNewGameClicked = {
                    navController.navigate(Screen.CreateGame.route)
                },
                onAvailableGamesClicked = {}
            )
        }
        composable(route = Screen.Map.route) { MapScreen(viewModel = viewModel) }
        composable(route = Screen.CreateGame.route) { CreateGameScreen(viewModel = viewModel) }
    }
}

fun NavHostController.navigateToFrom(to: String, from: String) {
    navigate(to) {
        popUpTo(from) {
            inclusive = true
        }
    }
}

sealed class Screen(open val route: String = EMPTY) {

    object Map : Screen("map")

    object Menu : Screen("menu")

    object CreateGame : Screen("create_game")

}


@ExperimentalCoroutinesApi
@Composable
fun CreateGameScreen(viewModel: MapViewModel) {
    Text(text = "Create Game")
}
