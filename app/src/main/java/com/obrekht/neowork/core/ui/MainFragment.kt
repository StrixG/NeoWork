package com.obrekht.neowork.core.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.obrekht.neowork.R
import com.obrekht.neowork.databinding.FragmentMainBinding
import com.obrekht.neowork.utils.viewBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainFragment : Fragment(R.layout.fragment_main) {

    private val binding by viewBinding(FragmentMainBinding::bind)
    private val viewModel: MainViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val navHostFragment = binding.mainFragmentContainer.getFragment<NavHostFragment>()
        binding.bottomNavigation.setupWithNavController(navHostFragment.navController)
    }
}