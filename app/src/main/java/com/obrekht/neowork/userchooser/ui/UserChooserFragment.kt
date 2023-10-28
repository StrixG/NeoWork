package com.obrekht.neowork.userchooser.ui

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.shape.MaterialShapeDrawable
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

    private var adapter: UserChooserAdapter? = null

    private val interactionListener = object : UserInteractionListener {
        override fun onCheckboxClick(user: User, isChecked: Boolean, position: Int) {
            viewModel.setUserSelected(user.id, isChecked)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val adapter = UserChooserAdapter(interactionListener)
        this.adapter = adapter

        with (binding) {
            appBar.statusBarForeground =
                MaterialShapeDrawable.createWithElevationOverlay(requireContext())

            toolbar.setNavigationOnClickListener {
                findNavController().popBackStack()
            }

            toolbar.setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.accept -> {
                        setFragmentResult(args.requestKey, bundleOf(
                            RESULT_CHOSEN_USER_IDS to viewModel.selectedUserIds.toLongArray()
                        ))
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
                viewModel.data.onEach {
                    adapter.submitData(it)
                }.launchIn(this)
            }
        }
    }

    override fun onDestroyView() {
        adapter = null
        super.onDestroyView()
    }

    companion object {
        const val REQUEST_KEY = "userChooser"
        const val RESULT_CHOSEN_USER_IDS = "chosenUserIds"
    }
}