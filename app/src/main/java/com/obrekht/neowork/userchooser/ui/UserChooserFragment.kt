package com.obrekht.neowork.userchooser.ui

import android.os.Bundle
import android.view.View
import androidx.annotation.StringRes
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.snackbar.Snackbar
import com.obrekht.neowork.R
import com.obrekht.neowork.databinding.FragmentUserChooserBinding
import com.obrekht.neowork.users.model.User
import com.obrekht.neowork.utils.repeatOnStarted
import com.obrekht.neowork.utils.setBarsInsetsListener
import com.obrekht.neowork.utils.viewBinding
import com.obrekht.neowork.utils.viewLifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

@AndroidEntryPoint
class UserChooserFragment : Fragment(R.layout.fragment_user_chooser) {

    private val binding by viewBinding(FragmentUserChooserBinding::bind)
    private val viewModel: UserChooserViewModel by viewModels()

    private val args: UserChooserFragmentArgs by navArgs()

    private var snackbar: Snackbar? = null
    private var adapter: UserChooserAdapter? = null

    private val interactionListener = object : UserInteractionListener {
        override fun onCheckedChange(user: User, isChecked: Boolean, position: Int) {
            viewModel.setUserSelected(user.id, isChecked)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val adapter = UserChooserAdapter(interactionListener)
        this.adapter = adapter

        with(binding) {
            appBar.statusBarForeground =
                MaterialShapeDrawable.createWithElevationOverlay(requireContext())

            toolbar.setNavigationOnClickListener {
                findNavController().popBackStack()
            }

            toolbar.setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.accept -> {
                        setFragmentResult(
                            args.requestKey, bundleOf(
                                RESULT_CHOSEN_USER_IDS to viewModel.selectedUserIds.toLongArray()
                            )
                        )
                        findNavController().popBackStack()
                    }

                    else -> false
                }
            }

            userListView.adapter = adapter
            userListView.setBarsInsetsListener {
                setPadding(
                    paddingLeft, paddingTop, paddingRight,
                    it.bottom
                )
            }

            swipeRefresh.setOnRefreshListener {
                viewModel.refresh()
            }
        }

        viewLifecycleScope.launch {
            viewLifecycleOwner.repeatOnStarted {
                adapter.onPagesUpdatedFlow.onEach {
                    if (viewModel.uiState.value.dataState is DataState.Error) {
                        binding.errorGroup.isVisible = adapter.itemCount == 0
                    }
                }.launchIn(this)

                viewModel.data.onEach {
                    adapter.submitData(it)
                }.launchIn(this)

                viewModel.uiState.onEach(::handleState).launchIn(this)
            }
        }
    }

    override fun onDestroyView() {
        snackbar = null
        adapter = null
        super.onDestroyView()
    }

    private fun handleState(state: UserChooserUiState) = with(binding) {
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

    private fun refresh() {
        viewModel.refresh()
        adapter?.refresh()
    }

    private fun showErrorSnackbar(@StringRes resId: Int, action: View.OnClickListener?) {
        snackbar = Snackbar.make(binding.userListView, resId, Snackbar.LENGTH_LONG).apply {
            if (action != null) {
                setAction(R.string.retry, action)
            }
            show()
        }
    }

    companion object {
        const val RESULT_CHOSEN_USER_IDS = "chosenUserIds"
    }
}
