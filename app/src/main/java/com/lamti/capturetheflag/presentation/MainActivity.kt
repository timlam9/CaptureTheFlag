package com.lamti.capturetheflag.presentation

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Share
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.lamti.capturetheflag.databinding.ActivityMainBinding
import com.lamti.capturetheflag.presentation.arcore.helpers.FullScreenHelper
import com.lamti.capturetheflag.presentation.fragments.navigation.MainFragmentFactory
import com.lamti.capturetheflag.presentation.fragments.navigation.Screen
import com.lamti.capturetheflag.presentation.fragments.navigation.navigateToScreen
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()

    private var collectFlowsJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        supportFragmentManager.fragmentFactory = MainFragmentFactory()
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        collectFlows()
    }

    override fun onStop() {
        collectFlowsJob?.cancel()
        super.onStop()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        FullScreenHelper.setFullScreenOnWindowFocusChanged(this, hasFocus)
    }

    private fun setupUI() {
        binding.bottomView.setContent {
            BottomNavigationView(
                onStatsClicked = { viewModel.onStatsClicked() },
                onMapClicked = { viewModel.onMapClicked() },
                onChatClicked = { viewModel.onChatClicked() }
            )
        }
    }

    private fun collectFlows() {
        collectFlowsJob = lifecycleScope.launchWhenCreated {
            viewModel.currentScreen.onEach(::navigate).launchIn(lifecycleScope)
        }
    }

    private fun navigate(screen: Screen) {
        supportFragmentManager.navigateToScreen(screen)
    }

}


@Composable
fun BottomNavigationView(
    modifier: Modifier = Modifier,
    onStatsClicked: () -> Unit,
    onMapClicked: () -> Unit,
    onChatClicked: () -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(20.dp).copy())
            .background(Color.Magenta),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onStatsClicked) {
            Icon(Icons.Filled.Phone, null)
        }
        IconButton(onClick = onMapClicked) {
            Icon(Icons.Filled.Home, null)
        }
        IconButton(onClick = onChatClicked) {
            Icon(Icons.Filled.Share, null)
        }
    }
}

