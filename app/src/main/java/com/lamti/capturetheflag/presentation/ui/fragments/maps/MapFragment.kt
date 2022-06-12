package com.lamti.capturetheflag.presentation.ui.fragments.maps

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.BottomSheetScaffold
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.google.android.gms.maps.MapsInitializer
import com.google.ar.core.ArCoreApk
import com.lamti.capturetheflag.R
import com.lamti.capturetheflag.databinding.FragmentMapBinding
import com.lamti.capturetheflag.presentation.ui.DatastoreHelper
import com.lamti.capturetheflag.presentation.ui.activity.MainActivity
import com.lamti.capturetheflag.presentation.ui.components.composables.common.ConfirmationDialog
import com.lamti.capturetheflag.presentation.ui.components.composables.common.DefaultButton
import com.lamti.capturetheflag.presentation.ui.components.navigation.GameNavigation
import com.lamti.capturetheflag.presentation.ui.components.navigation.Screen
import com.lamti.capturetheflag.presentation.ui.fragments.ar.AR_MODE_KEY
import com.lamti.capturetheflag.presentation.ui.popNavigate
import com.lamti.capturetheflag.presentation.ui.style.CaptureTheFlagTheme
import com.lamti.capturetheflag.presentation.ui.style.Red
import com.lamti.capturetheflag.presentation.ui.style.White
import com.lamti.capturetheflag.utils.myAppPreferences
import com.lamti.capturetheflag.utils.set
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
@SuppressLint("UnspecifiedImmutableFlag")
class MapFragment : Fragment(R.layout.fragment_map) {

    private var binding: FragmentMapBinding? = null
    private val viewModel: MapViewModel by viewModels()
    @Inject lateinit var dataStore: DatastoreHelper

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentMapBinding.bind(view)

        initializeDataOnSplashScreen()
        observeArMode()
        setupMapView()
        updateEnteredGeofenceId()
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }

    private fun initializeDataOnSplashScreen() {
        requireActivity().installSplashScreen().apply {
            setKeepOnScreenCondition {
                return@setKeepOnScreenCondition viewModel.stayInSplashScreen.value
            }
        }
    }

    private fun observeArMode() {
        lifecycleScope.launchWhenStarted {
            viewModel.arMode.onEach { requireActivity().myAppPreferences[AR_MODE_KEY] = it.name }.launchIn(lifecycleScope)
        }
    }

    private fun setupMapView() = binding?.run {
        MapsInitializer.initialize(requireContext())

        mapComposeView.setContent {
            val mainActivity = (requireActivity() as MainActivity)
            val bottomSheetScaffoldState = rememberBottomSheetScaffoldState()
            var showConfirmationDialog by remember { mutableStateOf(false) }
            val navController = rememberNavController()
            val coroutineScope = rememberCoroutineScope()

            CaptureTheFlagTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colors.background) {
                    BottomSheetScaffold(
                        sheetContent = {
                            SheetContent {
                                showConfirmationDialog = true
                            }
                        },
                        scaffoldState = bottomSheetScaffoldState,
                        sheetShape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                        sheetBackgroundColor = MaterialTheme.colors.background,
                        sheetPeekHeight = 0.dp,
                    ) {
                        Scaffold {
                            GameNavigation(
                                viewModel = viewModel,
                                navController = navController,
                                coroutineScope = coroutineScope,
                                dataStore = dataStore,
                                onLogoutClicked = { mainActivity.onLogoutClicked() },
                                onSettingFlagsButtonClicked = { mainActivity.onSettingFlagsClicked() },
                                onArScannerButtonClicked = {
                                    if (ArCoreApk.getInstance().checkAvailability(requireContext()).isSupported) {
                                        mainActivity.onArScannerButtonClicked()
                                    } else {
                                        Toast.makeText(requireContext(), "Ar is not supported. So the flag is just yours!", Toast.LENGTH_SHORT).show()
                                        viewModel.onArCorelessCaptured {
                                            if (it) (requireActivity() as MainActivity).onBackPressed()
                                        }
                                    }
                                },
                                onSettingsClicked = {
                                    coroutineScope.launch {
                                        if (bottomSheetScaffoldState.bottomSheetState.isCollapsed) {
                                            bottomSheetScaffoldState.bottomSheetState.expand()
                                        } else {
                                            bottomSheetScaffoldState.bottomSheetState.collapse()
                                        }
                                    }
                                }
                            )
                            ConfirmationDialog(
                                title = stringResource(id = R.string.quit_game),
                                description = stringResource(R.string.are_you_sure),
                                showDialog = showConfirmationDialog,
                                onNegativeDialogClicked = { showConfirmationDialog = false },
                                onPositiveButtonClicked = {
                                    coroutineScope.launch {
                                        bottomSheetScaffoldState.bottomSheetState.collapse()
                                    }
                                    showConfirmationDialog = false
                                    viewModel.onGameOverOkClicked {
                                        if (it) navController.popNavigate(Screen.Menu.route)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun SheetContent(onQuitGameClicked: () -> Unit) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                painter = painterResource(id = R.drawable.intro_logo),
                contentDescription = "intro image"
            )
            DefaultButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                text = "Quit Game",
                textColor = White,
                color = Red,
                onclick = onQuitGameClicked
            )
        }
    }

    private fun updateEnteredGeofenceId() {
        (requireActivity() as MainActivity).geofenceIdFLow
            .onEach { viewModel.setEnteredGeofenceId(it) }
            .launchIn(lifecycleScope)
    }
}
