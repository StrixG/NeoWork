package com.obrekht.neowork.users.ui.profile

import android.os.Bundle
import android.view.View
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import coil.load
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayoutMediator
import com.obrekht.neowork.R
import com.obrekht.neowork.auth.ui.navigateToLogIn
import com.obrekht.neowork.auth.ui.suggestauth.SuggestAuthDialogFragment
import com.obrekht.neowork.databinding.FragmentUserProfileBinding
import com.obrekht.neowork.jobs.ui.addedit.navigateToJobEditor
import com.obrekht.neowork.users.model.User
import com.obrekht.neowork.utils.repeatOnStarted
import com.obrekht.neowork.utils.viewBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

@AndroidEntryPoint
class UserProfileFragment : Fragment(R.layout.fragment_user_profile) {

    private val binding by viewBinding(FragmentUserProfileBinding::bind)
    private val viewModel: UserProfileViewModel
            by hiltNavGraphViewModels(R.id.user_profile_graph)

    private val args: UserProfileFragmentArgs by navArgs()

    private var snackbar: Snackbar? = null
    private lateinit var user: User

    private var viewPagerAdapter: UserProfileTabsAdapter? = null

    private val pageChangeCallback = object : OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            with (binding.buttonAddJob) {
                val pagerTab = viewPagerAdapter?.getTab(position)
                when {
                    (pagerTab == UserProfileTabsAdapter.Tabs.JOBS) && viewModel.isOwnProfile -> {
                        show()
                    }
                    else -> hide()
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setFragmentResultListener(
            SuggestAuthDialogFragment.REQUEST_KEY
        ) { _, bundle ->
            val positive = bundle.getBoolean(SuggestAuthDialogFragment.RESULT_POSITIVE)
            if (positive) {
                navigateToLogIn()
            }
        }

        viewPagerAdapter = UserProfileTabsAdapter(this, args.userId)

        with(binding) {
            toolbar.setNavigationOnClickListener {
                findNavController().navigateUp()
            }

            toolbar.setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.log_out -> {
                        showLogOutConfirmation()
                        true
                    }

                    else -> false
                }
            }

            viewPager.adapter = viewPagerAdapter
            viewPager.registerOnPageChangeCallback(pageChangeCallback)

            TabLayoutMediator(tabLayout, viewPager) { tab, position ->
                val pagerTab = UserProfileTabsAdapter.Tabs.values()[position]
                tab.text = getString(pagerTab.titleResId)
            }.attach()

            buttonAddJob.setOnClickListener {
                navigateToJobEditor()
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnStarted {
                viewModel.uiState.onEach(::handleState).launchIn(this)
                viewModel.event.onEach(::handleEvent).launchIn(this)
            }
        }
    }

    override fun onDestroyView() {
        snackbar = null
        viewPagerAdapter = null
        binding.viewPager.unregisterOnPageChangeCallback(pageChangeCallback)
        super.onDestroyView()
    }

    private fun handleState(state: UserProfileUiState) {
        user = state.user?.also {
            bindUser(it)
        } ?: return

        if (state.dataState == DataState.Error) {
            showErrorSnackbar(R.string.error_loading_user) {
                viewModel.refresh()
            }
        }
    }

    private fun handleEvent(event: UiEvent) {
        when (event) {
            UiEvent.ErrorLoadUser -> {
                showErrorSnackbar(R.string.error_loading_user) {
                    viewModel.refresh()
                }
            }

            UiEvent.ErrorConnection -> {
                showErrorSnackbar(R.string.error_connection) {
                    viewModel.refresh()
                }
            }
        }
    }

    private fun bindUser(user: User) {
        with(binding) {
            toolbar.title = if (viewModel.isOwnProfile) {
                getString(R.string.you)
            } else {
                "${user.name} / ${user.login}"
            }
            toolbar.menu.findItem(R.id.log_out)
                .isVisible = viewModel.isOwnProfile

            user.avatar?.let {
                avatar.load(it) {
                    placeholder(R.drawable.avatar_placeholder)
                    crossfade(true)
                }
            } ?: avatar.load(R.drawable.avatar_placeholder)
        }
    }

    private fun showLogOutConfirmation() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.log_out_confirmation_title))
            .setMessage(getString(R.string.log_out_confirmation_message))
            .setNegativeButton(getString(R.string.cancel), null)
            .setPositiveButton(getString(R.string.option_log_out)) { _, _ ->
                viewModel.logOut()
            }
            .show()
    }

    private fun showErrorSnackbar(
        @StringRes resId: Int,
        duration: Int = Snackbar.LENGTH_LONG,
        action: View.OnClickListener? = null
    ) {
        snackbar = Snackbar.make(requireView(), resId, duration).apply {
            if (action != null) {
                setAction(R.string.retry, action)
            }
            show()
        }
    }
}
