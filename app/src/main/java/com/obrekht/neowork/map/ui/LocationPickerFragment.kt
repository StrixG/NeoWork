package com.obrekht.neowork.map.ui

import android.Manifest
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.annotation.SuppressLint
import android.location.Location
import android.os.Bundle
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.doOnLayout
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.material.snackbar.Snackbar
import com.obrekht.neowork.R
import com.obrekht.neowork.databinding.FragmentLocationPickerBinding
import com.obrekht.neowork.utils.hasLocationPermission
import com.obrekht.neowork.utils.isLightTheme
import com.obrekht.neowork.utils.setBarsInsetsListener
import com.obrekht.neowork.utils.viewBinding
import com.yandex.mapkit.Animation
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.ScreenPoint
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.logo.Alignment
import com.yandex.mapkit.logo.HorizontalAlignment
import com.yandex.mapkit.logo.Padding
import com.yandex.mapkit.logo.VerticalAlignment
import com.yandex.mapkit.map.CameraListener
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.map.CameraUpdateReason
import com.yandex.mapkit.map.GestureFocusPointMode
import com.yandex.mapkit.map.InputListener
import com.yandex.mapkit.map.Map
import com.yandex.mapkit.map.Map.CameraCallback
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber

@AndroidEntryPoint
class LocationPickerFragment : Fragment(R.layout.fragment_location_picker) {

    private val binding by viewBinding(FragmentLocationPickerBinding::bind)
    private val viewModel: LocationPickerViewModel by viewModels()

    private val args: LocationPickerFragmentArgs by navArgs()
    
    private var previousSoftInputMode: Int? = null

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private lateinit var map: Map

    private var pinTranslationY: Float = 0f
    private var pinAnimationDuration: Int = 0
    private var pinAnimator: ObjectAnimator? = null

    private var isPinLifted: Boolean = false

    private val isInteractive: Boolean
        get() = findNavController().currentBackStackEntry?.let {
            it.destination.id == R.id.location_picker_fragment
        } ?: false

    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.any { it.value }) {
            moveCameraToMe()
        } else {
            Snackbar.make(
                requireView(),
                R.string.location_permission_denied,
                Snackbar.LENGTH_SHORT
            )
                .setAnchorView(binding.buttonFindMe)
                .show()
        }
    }

    private val cameraListener = CameraListener { _, cameraPosition, cameraUpdateReason, finished ->
        with(binding.buttonCompass) {
            rotation = FULL_ANGLE - cameraPosition.azimuth
            if (cameraPosition.azimuth == 0f) {
                if (finished && isVisible) {
                    animate().alpha(0f)
                        .withEndAction { isVisible = false }
                        .start()
                }
            } else {
                if (!isVisible) {
                    alpha = 0f
                    isVisible = true
                    animate().alpha(1f).start()
                }
            }
        }

        if (cameraUpdateReason == CameraUpdateReason.GESTURES) {
            binding.mapView.mapWindow.focusRect?.let {
                Timber.d("${it.bottomRight.x} ${it.bottomRight.y}")
            }
            if (finished) {
                tryStickToNorth()
            }
        }

        if (finished) {
            setPinLifted(false)

            viewModel.onCameraTargetChange(
                cameraPosition.target.latitude,
                cameraPosition.target.longitude
            )
        } else {
            setPinLifted(true)
        }
    }

    private val inputListener = object : InputListener {
        override fun onMapTap(map: Map, point: Point) {
            if (!isInteractive) return

            moveCamera(point)
        }

        override fun onMapLongTap(map: Map, point: Point) {}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val activity = requireActivity()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(activity)

        previousSoftInputMode = activity.window?.attributes?.softInputMode
        activity.window?.setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        requireActivity().window.statusBarColor =
            ContextCompat.getColor(requireContext(), R.color.transparent_gray)

        with(binding) {
            mapView.doOnLayout {
                mapView.mapWindow.apply {
                    gestureFocusPointMode = GestureFocusPointMode.AFFECTS_ALL_GESTURES
                    gestureFocusPoint = ScreenPoint(view.width / 2f, view.height / 2f)
                }
            }

            map = mapView.mapWindow.map.apply {
                logo.setAlignment(Alignment(HorizontalAlignment.LEFT, VerticalAlignment.BOTTOM))
                addInputListener(inputListener)
                addCameraListener(cameraListener)
                isNightModeEnabled = !resources.configuration.isLightTheme
            }

            searchBar.setNavigationOnClickListener {
                findNavController().popBackStack()
            }

            contentLayout.setBarsInsetsListener {
                updateLayoutParams<MarginLayoutParams> {
                    bottomMargin = it.bottom
                }
                map.logo.setPadding(Padding(0, it.bottom))
            }
            buttonApply.setBarsInsetsListener {
                val margin = resources.getDimension(R.dimen.common_spacing).toInt()
                updateLayoutParams<MarginLayoutParams> {
                    bottomMargin = margin + it.bottom
                }
            }

            searchView.editText.setOnEditorActionListener { _, _, _ ->
                searchView.hide()
                false
            }

            buttonApply.setOnClickListener {
                setFragmentResult(REQUEST_KEY, bundleOf(
                    RESULT_LOCATION_LATITUDE to viewModel.latitude,
                    RESULT_LOCATION_LONGITUDE to viewModel.longitude
                ))
                findNavController().popBackStack()
            }
        }

        savedInstanceState?.run {
            getDoubleArray(KEY_CAMERA_POSITION_TARGET)?.let {
                val (latitude, longitude) = it
                moveCamera(
                    Point(latitude, longitude),
                    getFloat(KEY_CAMERA_POSITION_ZOOM),
                    getFloat(KEY_CAMERA_POSITION_AZIMUTH),
                    getFloat(KEY_CAMERA_POSITION_TILT),
                    animate = false
                )
            }
        } ?: run {
            if (args.position == null) {
                moveCameraToMe(false)
            }
        }

        val pinSize = resources.getDimension(R.dimen.pin_size)
        pinTranslationY = -pinSize / 2
        pinAnimationDuration = resources.getInteger(android.R.integer.config_shortAnimTime)
        binding.pin.translationY = pinTranslationY

        binding.buttonCompass.setOnClickListener {
            rotateCameraToNorth()
        }

        binding.buttonFindMe.setOnClickListener {
            moveCameraToMe()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.flowWithLifecycle(viewLifecycleOwner.lifecycle)
                .collect(::handleUiState)

        }
    }

    override fun onDestroyView() {
        requireActivity().window.statusBarColor = 0

        map.removeCameraListener(cameraListener)
        map.removeInputListener(inputListener)

        super.onDestroyView()
    }

    override fun onStart() {
        super.onStart()
        MapKitFactory.getInstance().onStart()
        binding.mapView.onStart()
    }

    override fun onStop() {
        binding.mapView.onStop()
        MapKitFactory.getInstance().onStop()
        super.onStop()
    }

    override fun onDestroy() {
        previousSoftInputMode?.let {
            activity?.window?.setSoftInputMode(it)
        }
        super.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        map.cameraPosition.run {
            outState.putAll(
                bundleOf(
                    KEY_CAMERA_POSITION_TARGET to doubleArrayOf(target.latitude, target.longitude),
                    KEY_CAMERA_POSITION_ZOOM to zoom,
                    KEY_CAMERA_POSITION_AZIMUTH to azimuth,
                    KEY_CAMERA_POSITION_TILT to tilt
                )
            )
        }
    }

    private fun requestLocationPermissions() {
        locationPermissionRequest.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    private fun handleUiState(state: MapUiState) {
        if (!state.isCameraMoved && state.moveToPosition != null) {
            moveCamera(state.moveToPosition, DEFAULT_ZOOM, 0f, 0f, animate = false)
            viewModel.cameraMoved()
        }
    }

    private fun setPinLifted(isLifted: Boolean) {
        if (isLifted == isPinLifted) return

        pinAnimator?.cancel()
        pinAnimator = if (isPinLifted) {
            ObjectAnimator.ofPropertyValuesHolder(
                binding.pin,
                PropertyValuesHolder.ofFloat(
                    View.TRANSLATION_Y,
                    binding.pin.translationY,
                    pinTranslationY
                )
            )

        } else {
            ObjectAnimator.ofPropertyValuesHolder(
                binding.pin,
                PropertyValuesHolder.ofFloat(
                    View.TRANSLATION_Y,
                    binding.pin.translationY,
                    pinTranslationY * PIN_LIFT_OFFSET
                )
            )
        }.apply {
            duration = pinAnimationDuration.toLong()
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }

        isPinLifted = isLifted
    }

    @SuppressLint("MissingPermission")
    private fun moveCameraToMe(animate: Boolean = true) {
        if (!hasLocationPermission()) {
            requestLocationPermissions()
            return
        }
        fusedLocationClient.getCurrentLocation(
            Priority.PRIORITY_LOW_POWER,
            CancellationTokenSource().token
        )
            .addOnSuccessListener { location: Location? ->
                location?.let {
                    moveCamera(Point(it.latitude, it.longitude), DEFAULT_ZOOM, animate = animate)
                }
            }
    }

    private fun moveCamera(
        target: Point? = null,
        zoom: Float? = null,
        azimuth: Float? = null,
        tilt: Float? = null,
        animate: Boolean = true,
        callback: CameraCallback? = null
    ) {
        map.cameraPosition.run {
            val position = CameraPosition(
                target ?: this.target,
                zoom ?: this.zoom,
                azimuth ?: this.azimuth,
                tilt ?: this.tilt
            )
            if (animate) {
                map.move(position, CAMERA_ANIMATION, callback)
            } else {
                map.move(position)
            }
        }
    }

    private fun rotateCameraToNorth() {
        map.cameraPosition.run {
            val position = CameraPosition(target, zoom, 0f, tilt)
            map.move(position, CAMERA_ANIMATION, null)
        }
    }

    private fun tryStickToNorth() {
        map.cameraPosition.run {
            if (azimuth <= STICK_NORTH_AZIMUTH_OFFSET || azimuth >= FULL_ANGLE - STICK_NORTH_AZIMUTH_OFFSET) {
                map.move(
                    CameraPosition(
                        target, zoom, 0f, tilt
                    ), CAMERA_ANIMATION, null
                )
            }
        }
    }

    companion object {
        private const val FULL_ANGLE = 360f

        private val CAMERA_ANIMATION = Animation(Animation.Type.SMOOTH, 0.4f)
        private const val STICK_NORTH_AZIMUTH_OFFSET = 10f
        private const val DEFAULT_ZOOM = 15f
        private const val PIN_LIFT_OFFSET = 1.5f

        private const val KEY_CAMERA_POSITION_TARGET = "camera_position_target"
        private const val KEY_CAMERA_POSITION_ZOOM = "camera_position_zoom"
        private const val KEY_CAMERA_POSITION_AZIMUTH = "camera_position_azimuth"
        private const val KEY_CAMERA_POSITION_TILT = "camera_position_tilt"

        const val REQUEST_KEY = "pickLocation"
        const val RESULT_LOCATION_LATITUDE = "locationLatitude"
        const val RESULT_LOCATION_LONGITUDE = "locationLongitude"
    }
}
