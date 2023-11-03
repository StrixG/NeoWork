package com.obrekht.neowork.map.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.yandex.mapkit.geometry.Point
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class LocationPickerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    private val args = LocationPickerFragmentArgs.fromSavedStateHandle(savedStateHandle)

    var latitude: Double = 0.0
        private set
    var longitude: Double = 0.0
        private set

    init {
        args.position?.let {
            moveToPosition(it.latitude, it.longitude)
        }
    }

    fun onCameraTargetChange(latitude: Double, longitude: Double) {
        this.latitude = latitude
        this.longitude = longitude
    }

    fun moveToPosition(latitude: Double, longitude: Double) {
        val point = Point(latitude, longitude)
        _uiState.update { it.copy(isCameraMoved = false, moveToPosition = point) }
    }

    fun cameraMoved() {
        _uiState.update { it.copy(isCameraMoved = true, moveToPosition = null) }
    }
}

data class MapUiState(
    val moveToPosition: Point? = null,
    val isCameraMoved: Boolean = false
)
