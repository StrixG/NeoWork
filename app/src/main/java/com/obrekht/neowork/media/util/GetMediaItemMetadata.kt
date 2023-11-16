package com.obrekht.neowork.media.util

import android.content.Context
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.exoplayer.MetadataRetriever
import androidx.media3.exoplayer.source.TrackGroupArray
import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures
import kotlinx.coroutines.cancelFutureOnCancellation
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

suspend fun MediaItem.retrieveMediaMetadata(context: Context): MediaMetadata? =
    suspendCancellableCoroutine { continuation ->
        val metadataFuture =
            MetadataRetriever.retrieveMetadata(context, this)

        val audioTitleCallback = object : FutureCallback<TrackGroupArray?> {
            override fun onSuccess(trackGroups: TrackGroupArray?) {
                val mediaMetadata = trackGroups?.get(0)?.let { trackGroup ->
                    val mediaMetadataBuilder = MediaMetadata.Builder()
                    trackGroup.getFormat(0).metadata?.let { metadata ->
                        for (entryIndex in 0..<metadata.length()) {
                            metadata.get(entryIndex).populateMediaMetadata(mediaMetadataBuilder)
                        }
                    }
                    mediaMetadataBuilder.build()
                }
                continuation.resume(mediaMetadata)
            }

            override fun onFailure(t: Throwable) {
                continuation.resume(null)
            }
        }

        Futures.addCallback(
            metadataFuture,
            audioTitleCallback,
            ContextCompat.getMainExecutor(context)
        )
        continuation.cancelFutureOnCancellation(metadataFuture)
    }
