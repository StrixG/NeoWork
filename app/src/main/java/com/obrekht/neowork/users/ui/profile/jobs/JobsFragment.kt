package com.obrekht.neowork.users.ui.profile.jobs

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import androidx.annotation.StringRes
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.navigation.fragment.findNavController
import androidx.paging.CombinedLoadStates
import androidx.paging.LoadState
import com.google.android.material.snackbar.Snackbar
import com.obrekht.neowork.R
import com.obrekht.neowork.UserProfileGraphDirections
import com.obrekht.neowork.databinding.FragmentJobsBinding
import com.obrekht.neowork.deleteconfirmation.ui.DeleteConfirmationDialogFragment
import com.obrekht.neowork.deleteconfirmation.ui.DeleteElementType
import com.obrekht.neowork.jobs.model.Job
import com.obrekht.neowork.jobs.ui.addedit.navigateToJobEditor
import com.obrekht.neowork.users.ui.profile.UserProfileViewModel
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
class JobsFragment : Fragment(R.layout.fragment_jobs) {

    private val binding by viewBinding(FragmentJobsBinding::bind)
    private val viewModel: JobsViewModel by viewModels()
    private val userProfileViewModel: UserProfileViewModel
            by hiltNavGraphViewModels(R.id.user_profile_graph)

    private var snackbar: Snackbar? = null
    private var adapter: JobListAdapter? = null

    private val interactionListener = object : JobInteractionListener {
        override fun onEdit(job: Job) {
            navigateToJobEditor(job.id)
        }

        override fun onDelete(job: Job) {
            showDeleteConfirmation(job.id)
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        with(DeleteConfirmationDialogFragment) {
            parentFragment?.setFragmentResultListener(
                getRequestKey(DeleteElementType.JOB)
            ) { _, bundle ->
                val clickedButton = bundle.getInt(RESULT_CLICKED_BUTTON)
                if (clickedButton == DialogInterface.BUTTON_POSITIVE) {
                    val jobId = bundle.getLong(RESULT_ELEMENT_ID)
                    viewModel.deleteJobById(jobId)
                }
            }
        }

        val adapter = JobListAdapter(interactionListener, userProfileViewModel.isOwnProfile)
        this.adapter = adapter

        with(binding) {
            jobListView.adapter = adapter
            jobListView.setBarsInsetsListener {
                setPadding(
                    paddingLeft, paddingTop, paddingRight,
                    it.bottom + resources.getDimension(R.dimen.fab_bottom_padding).toInt()
                )
            }

            swipeRefresh.setOnRefreshListener {
                refresh()
            }
        }

        viewLifecycleScope.launch {
            viewLifecycleOwner.repeatOnStarted {
                launch {
                    adapter.loadStateFlow.collectLatest(::handleLoadState)
                }

                userProfileViewModel.uiState.onEach {
                    adapter.isOwnProfile = it.isOwnProfile
                    adapter.notifyDataSetChanged()
                }.launchIn(this)

                viewModel.data.onEach {
                    adapter.submitData(it)
                }.launchIn(this)

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

    private fun handleState(state: JobsUiState) = with(binding) {
        if (state.dataState is DataState.Loading) {
            snackbar?.dismiss()
            swipeRefresh.isRefreshing = true
        } else {
            swipeRefresh.isRefreshing = false
        }

        if (state.dataState is DataState.Error) {
            showErrorSnackbar(R.string.error_loading) {
                refresh()
            }
        }
    }

    private fun handleLoadState(state: CombinedLoadStates): Unit = with(binding) {
        adapter?.let { adapter ->
            if (state.refresh is LoadState.NotLoading
                && viewModel.uiState.value.dataState != DataState.Loading) {
                if (state.append.endOfPaginationReached) {
                    emptyText.isVisible = adapter.itemCount == 0
                }
            } else {
                emptyText.isVisible = false
            }
        }
    }

    private fun handleEvent(event: UiEvent) {
        when (event) {
            is UiEvent.ErrorDeleting -> {
                showErrorSnackbar(R.string.error_deleting_job) {
                    viewModel.deleteJobById(event.jobId)
                }
            }
        }
    }

    private fun refresh() {
        userProfileViewModel.refresh()
        viewModel.refresh()
        adapter?.refresh()
    }

    private fun showDeleteConfirmation(jobId: Long) {
        val action = UserProfileGraphDirections.actionOpenDeleteConfirmation(
            jobId,
            DeleteElementType.JOB
        )
        findNavController().navigate(action)
    }

    private fun showErrorSnackbar(@StringRes resId: Int, action: View.OnClickListener?) {
        snackbar = Snackbar.make(binding.root, resId, Snackbar.LENGTH_LONG).apply {
            if (action != null) {
                setAction(R.string.retry, action)
            }
            show()
        }
    }

    companion object {
        fun newInstance(userId: Long): JobsFragment {
            return JobsFragment().apply {
                arguments = bundleOf(
                    JobsArguments.USER_ID to userId
                )
            }
        }
    }
}
