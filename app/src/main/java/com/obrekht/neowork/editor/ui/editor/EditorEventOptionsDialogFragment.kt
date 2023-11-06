package com.obrekht.neowork.editor.ui.editor

import android.os.Bundle
import android.text.format.DateFormat.is24HourFormat
import android.view.View
import androidx.core.widget.doAfterTextChanged
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.obrekht.neowork.R
import com.obrekht.neowork.databinding.BottomSheetEventOptionsBinding
import com.obrekht.neowork.events.model.Event
import com.obrekht.neowork.events.model.EventType
import com.obrekht.neowork.utils.repeatOnStarted
import com.obrekht.neowork.utils.viewBinding
import com.obrekht.neowork.utils.viewLifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@AndroidEntryPoint
class EditorEventOptionsDialogFragment :
    BottomSheetDialogFragment(R.layout.bottom_sheet_event_options) {

    private val binding by viewBinding(BottomSheetEventOptionsBinding::bind)
    private val viewModel: EditorViewModel
            by hiltNavGraphViewModels(R.id.editor_fragment)

    private val formatter =
        DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT).withZone(ZoneId.systemDefault())

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        with(binding) {
            dateTextField.setEndIconOnClickListener {
                openDatePicker()
            }
            dateEditText.doAfterTextChanged {
                viewModel.validateEventDateTime(it.toString())
            }

            typeRadioGroup.setOnCheckedChangeListener { _, checkedId ->
                val eventType = when (checkedId) {
                    R.id.radio_button_online -> EventType.ONLINE
                    R.id.radio_button_offline -> EventType.OFFLINE
                    else -> error("Invalid radio button ID.")
                }
                viewModel.setEventType(eventType)
            }
        }

        viewLifecycleScope.launch {
            viewLifecycleOwner.repeatOnStarted {
                viewModel.edited.onEach { editable ->
                    val event = editable as Event
                    val checkedId = when (event.type) {
                        EventType.ONLINE -> R.id.radio_button_online
                        EventType.OFFLINE -> R.id.radio_button_offline
                    }
                    binding.typeRadioGroup.check(checkedId)

                    val dateTime = LocalDateTime.ofInstant(event.datetime, ZoneId.systemDefault())
                    formatDateTime(dateTime)
                }.launchIn(this)

                viewModel.uiState.onEach { state ->
                    binding.dateTextField.error = if (!state.isEventDateTimeValid) {
                        getString(R.string.error_invalid_datetime)
                    } else null
                }.launchIn(this)
            }
        }
    }

    private fun openDatePicker() {
        val selection = MaterialDatePicker.todayInUtcMilliseconds()
        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setSelection(selection)
            .setTitleText(R.string.select_event_date)
            .setPositiveButtonText(R.string.next)
            .build()

        datePicker.addOnPositiveButtonClickListener {
            openTimePicker(it)
        }

        datePicker.show(childFragmentManager, "event_date")
    }

    private fun openTimePicker(dateMillis: Long) {
        val now = LocalDateTime.now()
        val isSystem24Hour = is24HourFormat(requireContext())
        val clockFormat = if (isSystem24Hour) TimeFormat.CLOCK_24H else TimeFormat.CLOCK_12H
        val timePicker = MaterialTimePicker.Builder()
            .setTimeFormat(clockFormat)
            .setHour(now.hour)
            .setMinute(now.minute)
            .setTitleText(R.string.select_event_time)
            .build()

        timePicker.addOnPositiveButtonClickListener {
            val time = LocalTime.of(timePicker.hour, timePicker.minute)
            val dateTime = Instant.ofEpochMilli(dateMillis)
                .atZone(ZoneId.systemDefault())
                .with(time)

            viewModel.setEventDateTime(dateTime.toInstant())
        }

        timePicker.show(childFragmentManager, "event_time")
    }

    private fun formatDateTime(dateTime: LocalDateTime) {
        binding.dateEditText.setText(formatter.format(dateTime))
    }
}
