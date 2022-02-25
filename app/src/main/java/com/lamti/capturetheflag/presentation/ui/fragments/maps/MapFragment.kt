package com.lamti.capturetheflag.presentation.ui.fragments.maps

import android.os.Bundle
import android.view.View
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.fragment.app.Fragment
import com.google.android.gms.maps.GoogleMapOptions
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.lamti.capturetheflag.R
import com.lamti.capturetheflag.databinding.FragmentMapBinding
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
        val singapore = LatLng(37.93997336615248, 23.693031663615965)
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            googleMapOptionsFactory = {
                GoogleMapOptions().camera(CameraPosition.fromLatLngZoom(singapore, 24f))
            }
        )
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }

}
