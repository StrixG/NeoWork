package com.obrekht.neowork.users.ui.list

import android.os.Bundle
import android.view.View
import androidx.annotation.StringRes
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.paging.LoadState
import com.google.android.material.snackbar.Snackbar
import com.obrekht.neowork.R
import com.obrekht.neowork.auth.ui.navigateToLogIn
import com.obrekht.neowork.auth.ui.suggestauth.SuggestAuthDialogFragment
import com.obrekht.neowork.core.ui.MainFragment
import com.obrekht.neowork.databinding.FragmentUserListBinding
import com.obrekht.neowork.utils.findParent
import com.obrekht.neowork.utils.repeatOnStarted
import com.obrekht.neowork.utils.viewBinding
import com.obrekht.neowork.utils.viewLifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

@AndroidEntryPoint
class UserListFragment : Fragment(R.layout.fragment_user_list) {

    private val binding by viewBinding(FragmentUserListBinding::bind)
    private val viewModel: UserListViewModel by viewModels()

    private var snackbar: Snackbar? = null
    private var adapter: UserListAdapter? = null

    private val userClickListener: UserClickListener = { user, position ->
        // TODO: Open user profile
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        findParent<MainFragment>()?.setFragmentResult(
            MainFragment.REQUEST_KEY_SCROLL_TARGET,
            bundleOf(MainFragment.RESULT_TARGET_VIEW_ID to R.id.user_list_view)
        )

        setFragmentResultListener(
            SuggestAuthDialogFragment.REQUEST_KEY
        ) { _, bundle ->
            val positive = bundle.getBoolean(SuggestAuthDialogFragment.RESULT_POSITIVE)
            if (positive) {
                navigateToLogIn()
            }
        }

        with(binding) {
            adapter = UserListAdapter(userClickListener).apply {
                userListView.adapter = this
            }.also { adapter ->
                viewLifecycleScope.launch {
                    viewLifecycleOwner.repeatOnStarted {
                        launch {
                            adapter.loadStateFlow
                                .distinctUntilChangedBy { it.refresh }
                                .filter { it.refresh is LoadState.NotLoading }
                                .collect {
                                    userListView.scrollToPosition(0)
                                }
                        }
                    }
                }
            }

            swipeRefresh.setOnRefreshListener { refresh() }
            retryButton.setOnClickListener { refresh() }
        }

        viewLifecycleScope.launch {
            viewLifecycleOwner.repeatOnStarted {
                viewModel.data.onEach { adapter?.submitData(it) }.launchIn(this)
                viewModel.uiState.onEach(::handleState).launchIn(this)
                viewModel.event.onEach(::handleEvent).launchIn(this)
            }
        }
    }

    override fun onDestroyView() {
        snackbar = null
        adapter = null
        super.onDestroyView()
    }

    private fun handleState(state: UserListUiState) = with(binding) {
        if (state.dataState is DataState.Loading) {
            snackbar?.dismiss()
            if (adapter?.itemCount == 0) {
                progress.show()
                swipeRefresh.isEnabled = false
            } else {
                progress.hide()
                swipeRefresh.isRefreshing = true
            }
        } else {
            progress.hide()
            swipeRefresh.isEnabled = true
            swipeRefresh.isRefreshing = false
        }

        if (state.dataState == DataState.Error) {
            if (adapter?.itemCount == 0) {
                errorGroup.isVisible = true
            } else {
                errorGroup.isVisible = false
                showErrorSnackbar(R.string.error_loading) {
                    refresh()
                }
            }
        } else {
            errorGroup.isVisible = false
        }

        Unit
    }

    private fun handleEvent(event: Event) {
        when (event) {
            Event.ErrorLoadUsers -> showErrorSnackbar(R.string.error_loading) {
                refresh()
            }
        }
    }

    private fun refresh() {
        viewModel.refresh()
    }

    private fun showErrorSnackbar(@StringRes resId: Int, action: View.OnClickListener?) {
        snackbar = Snackbar.make(binding.userListView, resId, Snackbar.LENGTH_LONG).apply {
            setAnchorView(R.id.bottom_navigation)
            if (action != null) {
                setAction(R.string.retry, action)
            }
            show()
        }
    }
}