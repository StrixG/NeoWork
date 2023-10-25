package com.obrekht.neowork.jobs.ui.addedit

import android.os.Bundle
import android.text.Editable
import android.view.View
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.obrekht.neowork.R
import com.obrekht.neowork.databinding.FragmentAddEditJobBinding
import com.obrekht.neowork.utils.repeatOnStarted
import com.obrekht.neowork.utils.viewBinding
import com.obrekht.neowork.utils.viewLifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@AndroidEntryPoint
class AddEditJobFragment : Fragment(R.layout.fragment_add_edit_job) {

    private val binding by viewBinding(FragmentAddEditJobBinding::bind)
    private val viewModel: AddEditJobViewModel
            by hiltNavGraphViewModels(R.id.add_edit_job_fragment)

    private val args: AddEditJobFragmentArgs by navArgs()

    private var formEditTextList = emptyList<TextInputEditText>()
    private var snackbar: Snackbar? = null

    private val formatter =
        DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT).withZone(ZoneId.systemDefault())

    private val textChangedListener = { _: Editable? ->
        with(binding) {
            viewModel.updateForm(
                nameEditText.text.toString(),
                positionEditText.text.toString(),
                linkEditText.text.toString()
            )
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        with(binding) {
            if (args.jobId == AddEditJobViewModel.NEW_JOB_ID) {
                toolbar.setTitle(R.string.title_new_job)
            } else {
                toolbar.setTitle(R.string.title_edit_job)
            }

            toolbar.setNavigationOnClickListener {
                findNavController().popBackStack()
            }

            toolbar.setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.save -> {
                        viewModel.save()
                        true
                    }

                    else -> false
                }
            }

            formEditTextList = listOf(
                nameEditText,
                positionEditText,
                linkEditText
            )

            formEditTextList.forEach {
                it.doAfterTextChanged(textChangedListener)
            }

            buttonStartEndDates.setOnClickListener {
                val action = AddEditJobFragmentDirections.actionSelectJobDatesDialog()
                findNavController().navigate(action)
            }
        }

        setInteractionsActive(false)

        viewLifecycleScope.launch {
            viewLifecycleOwner.repeatOnStarted {
                viewModel.uiState.onEach(::handleState).launchIn(this)
                viewModel.event.onEach(::handleEvent).launchIn(this)
            }
        }
    }

    override fun onDestroyView() {
        formEditTextList = emptyList()
        snackbar = null
        super.onDestroyView()
    }

    private fun handleState(state: AddEditJobUiState) {
        setInteractionsActive(state.initialized && !state.isLoading)

        with(binding) {
            if (state.shouldInitialize) {
                nameEditText.append(state.formState.name)
                positionEditText.append(state.formState.position)
                linkEditText.append(state.formState.link)
                viewModel.onInitialized()
            }

            progressBar.isVisible = state.isLoading
            updateDatesButton(state.startDate, state.endDate)

            nameTextField.error = when (state.formState.nameError) {
                NameError.Empty -> getString(R.string.error_empty_name)
                else -> null
            }

            positionTextField.error = when (state.formState.positionError) {
                PositionError.Empty -> getString(R.string.error_empty_position)
                else -> null
            }

            linkTextField.error = when (state.formState.linkError) {
                LinkError.InvalidLink -> getString(R.string.error_invalid_link)
                else -> null
            }
        }
    }

    private fun handleEvent(event: Event) {
        when (event) {
            Event.JobSaved -> {
                findNavController().popBackStack()
            }

            Event.ErrorInvalidData -> {
                showErrorSnackbar(R.string.error_invalid_data)
            }

            Event.ErrorSaving -> {
                showErrorSnackbar(R.string.error_saving_job)
            }
        }

    }

    private fun updateDatesButton(start: Instant, end: Instant? = null) {
        val jobStart = formatter.format(start)
        val jobEnd = end?.let { formatter.format(it) }
            ?: resources.getString(R.string.present)

        binding.buttonStartEndDates.text = getString(R.string.work_period, jobStart, jobEnd)
    }

    private fun setInteractionsActive(active: Boolean) {
        formEditTextList.forEach { it.isEnabled = active }
        with(binding) {
            buttonStartEndDates.isEnabled = active
            toolbar.menu.findItem(R.id.save).isEnabled = active
        }
    }

    private fun showErrorSnackbar(@StringRes resId: Int, action: View.OnClickListener? = null) {
        snackbar = Snackbar.make(requireView(), resId, Snackbar.LENGTH_LONG).apply {
            if (action != null) {
                setAction(R.string.retry, action)
            }
            show()
        }
    }
}