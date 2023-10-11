package com.obrekht.neowork.core.ui

import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import com.obrekht.neowork.R

fun Fragment.findRootNavController() =
    Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_container)