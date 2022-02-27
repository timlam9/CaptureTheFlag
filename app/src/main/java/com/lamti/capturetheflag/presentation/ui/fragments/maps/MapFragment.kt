package com.lamti.capturetheflag.presentation.ui.fragments.maps

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.ui.Modifier
import androidx.fragment.app.Fragment
import com.google.android.gms.maps.MapsInitializer
import com.lamti.capturetheflag.R
import com.lamti.capturetheflag.databinding.FragmentMapBinding
import com.lamti.capturetheflag.presentation.location.geofences.GeofenceBroadcastReceiver
import com.lamti.capturetheflag.presentation.ui.components.map.MapScreen
import com.lamti.capturetheflag.presentation.ui.style.CaptureTheFlagTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
@SuppressLint("UnspecifiedImmutableFlag")
class MapFragment : Fragment(R.layout.fragment_map) {

    private var binding: FragmentMapBinding? = null

    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(requireContext(), GeofenceBroadcastReceiver::class.java)
        PendingIntent.getBroadcast(requireContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentMapBinding.bind(view)

        setupMapView()
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }

    private fun setupMapView() = binding?.run {
        MapsInitializer.initialize(requireContext())

        mapComposeView.setContent {
            CaptureTheFlagTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colors.background) {
                    MapScreen(geofencePendingIntent = geofencePendingIntent)
                }
            }
        }
    }

}
