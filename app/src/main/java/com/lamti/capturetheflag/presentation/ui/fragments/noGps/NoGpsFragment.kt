package com.lamti.capturetheflag.presentation.ui.fragments.noGps

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.lamti.capturetheflag.R
import com.lamti.capturetheflag.databinding.FragmentNogpsBinding
import com.lamti.capturetheflag.presentation.ui.activity.MainActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class NoGpsFragment : Fragment(R.layout.fragment_nogps) {

    private var binding: FragmentNogpsBinding? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentNogpsBinding.bind(view)

        binding?.refreshButton?.setOnClickListener {
            (requireActivity() as MainActivity).checkIfGpsIsEnabledAndRefreshScreen()
        }
    }
}
