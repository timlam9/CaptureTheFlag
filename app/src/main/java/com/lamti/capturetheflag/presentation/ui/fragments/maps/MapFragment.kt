package com.lamti.capturetheflag.presentation.ui.fragments.maps

import android.os.Bundle
import android.view.View
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.fragment.app.Fragment
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.lamti.capturetheflag.R
import com.lamti.capturetheflag.databinding.FragmentMapBinding
import com.lamti.capturetheflag.presentation.ui.bitmapDescriptorFromVector
import com.lamti.capturetheflag.presentation.ui.style.CaptureTheFlagTheme

class MapFragment : Fragment(R.layout.fragment_map) {

    private var binding: FragmentMapBinding? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentMapBinding.bind(view)

        setupMapView()
    }

    private fun setupMapView() = binding?.run {
        mapComposeView.setContent {
            CaptureTheFlagTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colors.background) {
                    MapView()
                }
            }
        }
    }

    @Composable
    fun MapView() {
        val randomPosition = LatLng(37.93997336615248, 23.693031663615965)
        val cameraPositionState = rememberCameraPositionState {
            position = CameraPosition.fromLatLngZoom(randomPosition, 15f)
        }
        val mapProperties by remember {
            mutableStateOf(MapProperties(isMyLocationEnabled = true))
        }
        val uiSettings by remember {
            mutableStateOf(
                MapUiSettings(
                    myLocationButtonEnabled = true,
                    zoomControlsEnabled = false,
                    mapToolbarEnabled = false
                )
            )
        }

        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = mapProperties,
            uiSettings = uiSettings
        ) {
            val icon = requireContext().bitmapDescriptorFromVector(R.drawable.ic_launcher_foreground, R.color.teal_700)
            Marker(
                position = randomPosition,
                title = "Random title",
                icon = icon,
                onClick = {
                    return@Marker false
                }
            )
        }
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }

}
