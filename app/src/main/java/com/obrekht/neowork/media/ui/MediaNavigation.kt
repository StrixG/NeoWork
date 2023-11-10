package com.obrekht.neowork.media.ui

import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.FragmentNavigatorExtras
import coil.request.SuccessResult
import coil.result
import com.obrekht.neowork.NavGraphDirections
import com.obrekht.neowork.core.model.AttachmentType
import com.obrekht.neowork.core.ui.findRootNavController

fun Fragment.navigateToMediaView(
    mediaType: AttachmentType,
    url: String,
    sharedElement: ImageView? = null
) {
    val extras = sharedElement?.let {
        FragmentNavigatorExtras(sharedElement to url)
    } ?: FragmentNavigatorExtras()

    val memoryCacheKey = sharedElement?.result?.let {
        (it as? SuccessResult)?.memoryCacheKey?.key
    }

    val action = NavGraphDirections.actionOpenMediaView(mediaType, url, memoryCacheKey)
    findRootNavController().navigate(action, extras)
}
