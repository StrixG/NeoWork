package com.obrekht.neowork.core.ui

import android.os.Bundle
import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.core.view.children
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.shape.MaterialShapeDrawable
import com.obrekht.neowork.R
import com.obrekht.neowork.auth.ui.navigateToLogIn
import com.obrekht.neowork.auth.ui.navigateToSignUp
import com.obrekht.neowork.databinding.FragmentMainBinding
import com.obrekht.neowork.users.ui.navigateToUserProfile
import com.obrekht.neowork.utils.repeatOnStarted
import com.obrekht.neowork.utils.viewBinding
import com.obrekht.neowork.utils.viewLifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainFragment : Fragment(R.layout.fragment_main) {

    private val binding by viewBinding(FragmentMainBinding::bind)
    private val viewModel: MainViewModel by viewModels()

    private val appBarClickListener = Toolbar.OnMenuItemClickListener {
        when (it.itemId) {
            R.id.log_in -> {
                navigateToLogIn()
                true
            }

            R.id.sign_up -> {
                navigateToSignUp()
                true
            }

            R.id.profile -> {
                navigateToUserProfile(viewModel.loggedInUserId)
                true
            }

            else -> false
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) = with(binding) {
        val navHostFragment = mainFragmentContainer.getFragment<NavHostFragment>()
        val navController = navHostFragment.navController

        navHostFragment.childFragmentManager.setFragmentResultListener(
            REQUEST_KEY_SCROLL_TARGET,
            viewLifecycleOwner
        ) { _, bundle ->
            val targetViewId = bundle.getInt(RESULT_TARGET_VIEW_ID)
            val targetView = view.findViewById<View>(targetViewId)
            binding.appBar.setLiftOnScrollTargetView(targetView)
        }

        val appBarConfiguration = AppBarConfiguration(
            bottomNavigation.menu.children
                .map { it.itemId }.toSet()
        )
        toolbar.setupWithNavController(navController, appBarConfiguration)
        toolbar.setOnMenuItemClickListener(appBarClickListener)
        bottomNavigation.setupWithNavController(navController)

        appBar.statusBarForeground =
            MaterialShapeDrawable.createWithElevationOverlay(requireContext())

        viewLifecycleScope.launch {
            viewLifecycleOwner.repeatOnStarted {
                viewModel.loggedInState.collect { isLoggedIn ->
                    with(toolbar) {
                        menu.setGroupVisible(R.id.unauthenticated, !isLoggedIn)
                        menu.setGroupVisible(R.id.authenticated, isLoggedIn)
                    }
                }
            }
        }

        Unit
    }

    companion object {
        const val REQUEST_KEY_SCROLL_TARGET = "scrollTarget"
        const val RESULT_TARGET_VIEW_ID = "targetViewId"
    }
}