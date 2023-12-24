package com.obrekht.neowork.map

import androidx.fragment.app.Fragment
import com.obrekht.neowork.NavGraphDirections
import com.obrekht.neowork.core.ui.findRootNavController
import com.obrekht.neowork.map.model.LocationPoint

fun Fragment.navigateToLocationPicker(point: LocationPoint? = null) {
    val action = NavGraphDirections.actionOpenLocationPicker(point)
    findRootNavController().navigate(action)
}
