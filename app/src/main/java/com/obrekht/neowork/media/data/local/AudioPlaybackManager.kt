package com.obrekht.neowork.media.data.local

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.Util
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.obrekht.neowork.core.di.ApplicationCoroutineScope
import com.obrekht.neowork.media.service.PlaybackService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioPlaybackManager @Inject constructor(
    @ApplicationContext context: Context,
    @ApplicationCoroutineScope scope: CoroutineScope
) {

    private val _playbackState = MutableStateFlow(PlaybackState.EMPTY)
    val playbackState = _playbackState.asStateFlow()

    private val _nowPlaying = MutableStateFlow(MediaItem.EMPTY)
    val nowPlaying = _nowPlaying.asStateFlow()

    var mediaController: MediaController? = null
        private set

    init {
        scope.launch {
            val sessionToken =
                SessionToken(context, ComponentName(context, PlaybackService::class.java))
            mediaController = MediaController.Builder(context, sessionToken)
                .buildAsync()
                .await()
            mediaController?.addListener(PlayerListener())
        }
    }

    fun playUrl(url: String) {
        val nowPlaying = nowPlaying.value
        val player = mediaController ?: return

        val isPrepared = player.playbackState != Player.STATE_IDLE
        if (isPrepared && url == nowPlaying.mediaId) {
            Util.handlePlayPauseButtonAction(player)
        } else {
            player.run {
                val mediaItem = MediaItem.Builder()
                    .setMediaId(url)
                    .setUri(url)
                    .build()

                setMediaItem(mediaItem)
                prepare()
                play()
            }
        }
    }

    private fun updatePlaybackState(player: Player) {
        _playbackState.value = PlaybackState(
            player.playbackState,
            player.playWhenReady,
            player.duration
        )
    }

    private fun updateNowPlaying(player: Player) {
        val mediaItem = player.currentMediaItem ?: return
        _nowPlaying.value = mediaItem
    }

    private inner class PlayerListener : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            if (events.contains(Player.EVENT_PLAY_WHEN_READY_CHANGED)
                || events.contains(Player.EVENT_PLAYBACK_STATE_CHANGED)
                || events.contains(Player.EVENT_MEDIA_ITEM_TRANSITION)
            ) {
                updatePlaybackState(player)
            }
            if (events.contains(Player.EVENT_MEDIA_METADATA_CHANGED)
                || events.contains(Player.EVENT_MEDIA_ITEM_TRANSITION)
                || events.contains(Player.EVENT_PLAY_WHEN_READY_CHANGED)
            ) {
                updateNowPlaying(player)
            }
        }
    }
}

class PlaybackState(
    private val playbackState: Int = Player.STATE_IDLE,
    private val playWhenReady: Boolean = false,
    val duration: Long = C.TIME_UNSET
) {
    val isPlaying: Boolean
        get() {
            return (playbackState == Player.STATE_BUFFERING
                    || playbackState == Player.STATE_READY)
                    && playWhenReady
        }

    companion object {
        val EMPTY = PlaybackState()
    }
}

