package com.obrekht.neowork.utils

import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.CoroutineScope

val Fragment.viewLifecycle
    get() = viewLifecycleOwner.lifecycle

val Fragment.viewLifecycleScope
    get() = viewLifecycleOwner.lifecycleScope

suspend inline fun LifecycleOwner.repeatOnStarted(noinline block: suspend CoroutineScope.() -> Unit) =
    repeatOnLifecycle(Lifecycle.State.STARTED, block)