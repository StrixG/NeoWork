package com.obrekht.neowork.media.ui

import android.content.ComponentName
import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.Window
import androidx.core.view.GestureDetectorCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.media3.common.MediaItem
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.media3.ui.PlayerView
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.transition.TransitionInflater
import coil.load
import com.google.common.util.concurrent.MoreExecutors
import com.obrekht.neowork.R
import com.obrekht.neowork.core.model.AttachmentType
import com.obrekht.neowork.databinding.FragmentMediaViewBinding
import com.obrekht.neowork.media.service.PlaybackService
import com.obrekht.neowork.utils.isLightTheme
import com.obrekht.neowork.utils.viewBinding
import java.util.concurrent.Future

class MediaViewFragment : Fragment(R.layout.fragment_media_view) {

    private val binding by viewBinding(FragmentMediaViewBinding::bind)
    private val viewModel: MediaViewViewModel by viewModels()

    private val args: MediaViewFragmentArgs by navArgs()

    private var insetsController: WindowInsetsControllerCompat? = null
    private var systemBarsVisible: Boolean = true

    private val gestureListener = object : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent): Boolean = true

        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            if (systemBarsVisible) {
                hideUi()
            } else {
                showUi()
            }
            return true
        }
    }

    private var mediaControllerFuture: Future<MediaController>? = null
    private var mediaController: MediaController? = null

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
        insetsController?.show(WindowInsetsCompat.Type.systemBars())
        if (resources.configuration.isLightTheme) {
            insetsController?.isAppearanceLightStatusBars = true
        }
        super.onStop()
    }

    override fun onDestroyView() {
        insetsController = null
        releaseMediaController()
        super.onDestroyView()
    }

    private fun setupHideUiListener(window: Window) {
        val gestureDetector = GestureDetectorCompat(requireContext(), gestureListener)

        ViewCompat.setOnApplyWindowInsetsListener(window.decorView) { decorView, windowInsets ->
            systemBarsVisible = (windowInsets.isVisible(WindowInsetsCompat.Type.navigationBars())
                    || windowInsets.isVisible(WindowInsetsCompat.Type.statusBars()))

            decorView.setOnTouchListener { view, event ->
                gestureDetector.onTouchEvent(event)
                view.performClick()
            }

            windowInsets
        }
    }

    private fun loadAttachment() {
        when (args.mediaType) {
            AttachmentType.IMAGE -> loadImage(args.url)
            else -> initializeMediaController()
        }
    }

    private fun loadImage(url: String) = with(binding) {
        image.isVisible = true
        image.load(url) {
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

    private fun loadVideo(url: String) = with(binding) {
        videoPlayer.isVisible = true
        videoPlayer.player = mediaController

        val mediaItem = MediaItem.fromUri(url)

        mediaController?.run {
            setMediaItem(mediaItem)
            play()
        }
        startPostponedEnterTransition()
    }

    private fun initializeMediaController() {
        if (mediaController != null) return

        val context = requireContext()
        val sessionToken =
            SessionToken(context, ComponentName(context, PlaybackService::class.java))
        mediaControllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
            .apply {
                addListener(
                    {
                        val mediaController = get()
                        this@MediaViewFragment.mediaController = mediaController
                        onMediaControllerReady(mediaController)
                    },
                    MoreExecutors.directExecutor()
                )
            }
    }

    private fun releaseMediaController() {
        mediaControllerFuture?.let(MediaController::releaseFuture)
        mediaController?.release()
        mediaControllerFuture = null
        mediaController = null
    }

    private fun onMediaControllerReady(mediaController: MediaController) {
        mediaController.run {
            prepare()
        }
        if (args.mediaType == AttachmentType.VIDEO) {
            binding.videoPlayer.player = mediaController
            binding.videoPlayer.setControllerVisibilityListener(PlayerView.ControllerVisibilityListener {
                if (it == View.VISIBLE) {
                    showUi()
                } else {
                    hideUi()
                }
            })
            loadVideo(args.url)
        }
    }

    private fun showUi() {
        insetsController?.show(WindowInsetsCompat.Type.systemBars())
        binding.toolbar.isVisible = true
    }

    private fun hideUi() {
        insetsController?.hide(WindowInsetsCompat.Type.systemBars())
        binding.toolbar.isVisible = false
    }
}
