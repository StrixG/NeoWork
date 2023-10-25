package com.obrekht.neowork.jobs.ui.addedit

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog_Centered
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointForward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.obrekht.neowork.R
import com.obrekht.neowork.databinding.DialogSelectDatesBinding
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

class SelectJobDatesDialogFragment : DialogFragment() {

    private lateinit var binding: DialogSelectDatesBinding
    private val viewModel: AddEditJobViewModel
            by hiltNavGraphViewModels(R.id.add_edit_job_fragment)

    private var startMillis: Long = 0L
    private var endMillis: Long? = null

    private val formatter =
        DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT).withZone(ZoneId.systemDefault())

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        binding = DialogSelectDatesBinding.inflate(layoutInflater)

        with(binding) {
            startDateEditText.setOnClickListener {
                val datePicker = MaterialDatePicker.Builder.datePicker()
                    .setSelection(startMillis)
                    .setTitleText(R.string.select_start_date)
                    .build()

                datePicker.addOnPositiveButtonClickListener {
                    startMillis = it
                    endMillis?.let { end ->
                        if (startMillis >= end) {
                            endMillis = null
                        }
                    }
                    formatDates()
                }

                datePicker.show(childFragmentManager, "start_date")
            }
            endDateEditText.setOnClickListener {
                val constraints =
                    CalendarConstraints.Builder()
                        .setValidator(DateValidatorPointForward.from(startMillis))
                        .build()

                val selection = endMillis ?: startMillis
                val datePicker = MaterialDatePicker.Builder.datePicker()
                    .setCalendarConstraints(constraints)
                    .setSelection(selection)
                    .setTitleText(R.string.select_end_date)
                    .build()

                datePicker.addOnPositiveButtonClickListener {
                    endMillis = it
                    formatDates()
                }

                datePicker.show(childFragmentManager, "end_date")
            }
            endDateTextField.setEndIconOnClickListener {
                endMillis = null
                formatDates()
            }
        }

        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect {
                    startMillis = it.startDate.toEpochMilli()
                    endMillis = it.endDate?.toEpochMilli()
                    formatDates()
                }
            }
        }

        return MaterialAlertDialogBuilder(
            requireContext(),
            ThemeOverlay_Material3_MaterialAlertDialog_Centered
        )
            .setIcon(R.drawable.ic_date_range)
            .setTitle(R.string.period_of_work)
            .setView(binding.root)
            .setPositiveButton(R.string.ok) { _, _ ->
                viewModel.setDates(
                    Instant.ofEpochMilli(startMillis),
                    endMillis?.let(Instant::ofEpochMilli)
                )
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun formatDates() {
        with (binding) {
            startDateEditText.setText(
                formatter.format(Instant.ofEpochMilli(startMillis))
            )
            endDateEditText.setText(
                endMillis?.let {
                    formatter.format(Instant.ofEpochMilli(it))
                } ?: getString(R.string.present)
            )
        }
    }
}