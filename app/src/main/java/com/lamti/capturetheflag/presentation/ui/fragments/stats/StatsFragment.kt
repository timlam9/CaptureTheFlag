package com.lamti.capturetheflag.presentation.ui.fragments.stats

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.lamti.capturetheflag.R
import com.lamti.capturetheflag.databinding.FragmentStatsBinding
import com.lamti.capturetheflag.presentation.ui.login.LoginActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import javax.inject.Inject

@ExperimentalCoroutinesApi
@InternalCoroutinesApi
@AndroidEntryPoint
class StatsFragment: Fragment(R.layout.fragment_stats) {

    private val viewModel: StatsViewModel by viewModels()
    private var binding: FragmentStatsBinding? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentStatsBinding.bind(view)

        binding?.logoutButton?.setOnClickListener {
            viewModel.onLogoutButtonClicked()
            startActivity(Intent(requireContext(), LoginActivity::class.java))
            requireActivity().finish()
        }
    }

}
