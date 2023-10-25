package com.obrekht.neowork.core.model

import kotlinx.serialization.Serializable

@Serializable
data class Coordinates(
    val lat: Double,
    val long: Double
)
