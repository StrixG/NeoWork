package com.obrekht.neowork.core.ui

import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import com.obrekht.neowork.R

fun Fragment.findRootNavController() =
    requireActivity().findNavController(R.id.nav_host_fragment_container)