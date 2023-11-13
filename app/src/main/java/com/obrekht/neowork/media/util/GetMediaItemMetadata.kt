package com.obrekht.neowork.media.util

import android.content.Context
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.exoplayer.MetadataRetriever
import androidx.media3.exoplayer.source.TrackGroupArray
import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures

typealias GetMetadataCallback = (mediaMetadata: MediaMetadata?) -> Unit

fun MediaItem.retrieveMediaMetadata(context: Context, callback: GetMetadataCallback) {
    val audioTitleCallback = object : FutureCallback<TrackGroupArray?> {
        override fun onSuccess(trackGroups: TrackGroupArray?) {
            trackGroups?.get(0)?.let { trackGroup ->
                val mediaMetadataBuilder = MediaMetadata.Builder()
                trackGroup.getFormat(0).metadata?.let {
                    for (entryIndex in 0..<it.length()) {
                        it.get(entryIndex).populateMediaMetadata(mediaMetadataBuilder)
                    }
                }
                val mediaMetadata = mediaMetadataBuilder.build()
                callback(mediaMetadata)
            } ?: callback(null)
        }

        override fun onFailure(t: Throwable) {
            callback(null)
        }
    }

    val metadataFuture =
        MetadataRetriever.retrieveMetadata(context, this)
    Futures.addCallback(
        metadataFuture,
        audioTitleCallback,
        ContextCompat.getMainExecutor(context)
    )
}
