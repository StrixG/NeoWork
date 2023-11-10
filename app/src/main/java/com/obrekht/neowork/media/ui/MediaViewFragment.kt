package com.obrekht.neowork.media.ui

import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.Window
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.transition.TransitionInflater
import coil.load
import com.obrekht.neowork.R
import com.obrekht.neowork.core.model.AttachmentType
import com.obrekht.neowork.databinding.FragmentMediaViewBinding
import com.obrekht.neowork.utils.isLightTheme
import com.obrekht.neowork.utils.viewBinding

class MediaViewFragment : Fragment(R.layout.fragment_media_view) {

    private val binding by viewBinding(FragmentMediaViewBinding::bind)
    private val viewModel: MediaViewViewModel by viewModels()

    private val args: MediaViewFragmentArgs by navArgs()

    private var insetsController: WindowInsetsControllerCompat? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedElementEnterTransition = TransitionInflater.from(requireContext())
            .inflateTransition(android.R.transition.move)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        postponeEnterTransition()

        val window = requireActivity().window
        insetsController = WindowCompat.getInsetsController(window, window.decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        setupHideUiListener(window)

        with(binding) {
            toolbar.setNavigationOnClickListener {
                findNavController().navigateUp()
            }

            image.transitionName = args.url

            loadAttachment()
        }
    }

    override fun onStart() {
        super.onStart()
        insetsController?.isAppearanceLightStatusBars = false
    }

    override fun onStop() {
        if (resources.configuration.isLightTheme) {
            insetsController?.isAppearanceLightStatusBars = true
        }
        super.onStop()
    }

    override fun onDestroyView() {
        insetsController = null
        super.onDestroyView()
    }

    private fun setupHideUiListener(window: Window) {
        ViewCompat.setOnApplyWindowInsetsListener(window.decorView) { decorView, windowInsets ->
            val isSystemBarsVisible =
                (windowInsets.isVisible(WindowInsetsCompat.Type.navigationBars())
                        || windowInsets.isVisible(WindowInsetsCompat.Type.statusBars()))

            decorView.setOnTouchListener { v, event ->
                if (event.action == MotionEvent.ACTION_UP) {
                    if (isSystemBarsVisible) {
                        insetsController?.hide(WindowInsetsCompat.Type.systemBars())
                        binding.toolbar.isVisible = false
                    } else {
                        insetsController?.show(WindowInsetsCompat.Type.systemBars())
                        binding.toolbar.isVisible = true
                    }
                    v.performClick()
                } else {
                    false
                }
            }

            windowInsets
        }
    }

    private fun loadAttachment() = with(binding) {
        when (args.mediaType) {
            AttachmentType.IMAGE -> {
                image.load(args.url) {
                    placeholderMemoryCacheKey(args.memoryCacheKey)
                    listener(
                        onError = { _, _ ->
                            startPostponedEnterTransition()
                        },
                        onSuccess = { _, _ ->
                            startPostponedEnterTransition()
                        }
                    )
                }
            }

            else -> {
                // TODO: Load video
                startPostponedEnterTransition()
            }
        }

        Unit
    }
}
