package com.obrekht.neowork.users.ui.allusers

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.paging.CombinedLoadStates
import androidx.paging.LoadState
import com.google.android.material.snackbar.Snackbar
import com.obrekht.neowork.R
import com.obrekht.neowork.core.ui.MainFragment
import com.obrekht.neowork.databinding.FragmentAllUsersBinding
import com.obrekht.neowork.userlist.ui.UserClickListener
import com.obrekht.neowork.userlist.ui.UserListAdapter
import com.obrekht.neowork.users.ui.navigateToUserProfile
import com.obrekht.neowork.utils.repeatOnStarted
import com.obrekht.neowork.utils.setBarsInsetsListener
import com.obrekht.neowork.utils.viewBinding
import com.obrekht.neowork.utils.viewLifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AllUsersFragment : Fragment(R.layout.fragment_all_users) {

    private val binding by viewBinding(FragmentAllUsersBinding::bind)
    private val viewModel: AllUsersViewModel by viewModels()

    private var snackbar: Snackbar? = null
    private var adapter: UserListAdapter? = null

    private val userClickListener: UserClickListener = { user, _ ->
        navigateToUserProfile(user.id)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setFragmentResult(
            MainFragment.REQUEST_KEY_SCROLL_TARGET,
            bundleOf(MainFragment.RESULT_TARGET_VIEW_ID to R.id.user_list_view)
        )

        view.setBarsInsetsListener {
            updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = it.bottom
            }
        }

        val adapter = UserListAdapter(userClickListener)
        this.adapter = adapter

        with(binding) {
            userListView.adapter = adapter
            swipeRefresh.setOnRefreshListener { refresh() }
            retryButton.setOnClickListener { refresh() }
        }

        viewLifecycleScope.launch {
            viewLifecycleOwner.repeatOnStarted {
                launch {
                    adapter.loadStateFlow.collectLatest(::handleLoadState)
                }
                viewModel.data.onEach { adapter.submitData(it) }.launchIn(this)
                viewModel.uiState.onEach(::handleState).launchIn(this)
            }
        }
    }

    override fun onDestroyView() {
        snackbar = null
        adapter = null
        super.onDestroyView()
    }

    private fun handleState(state: AllUsersUiState) = with(binding) {
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

        if (state.dataState is DataState.Error) {
            val text = when (state.dataState.type) {
                ErrorType.FailedToLoad -> R.string.error_loading_users
                ErrorType.Connection -> R.string.error_connection
                else -> R.string.error_loading
            }
            showErrorSnackbar(text) {
                refresh()
            }
            errorGroup.isVisible = adapter?.itemCount == 0
        } else {
            errorGroup.isVisible = false
        }

        Unit
    }

    private fun handleLoadState(state: CombinedLoadStates): Unit = with(binding) {
        val dataState = viewModel.uiState.value.dataState

        if (state.refresh is LoadState.Error || dataState is DataState.Error) {
            errorGroup.isVisible = adapter?.itemCount == 0
        }
    }

    private fun refresh() {
        viewModel.refresh()
        adapter?.refresh()
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
