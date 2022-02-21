package dev.zenrg.zenphotoframe.models

import kotlinx.serialization.Serializable

@Serializable
data class MediaDate(
    val day: String? = null,
    val month: String? = null,
    val year: String? = null
)

@Serializable
data class DateRange(
    val endDate: MediaDate? = null,
    val startDate: MediaDate? = null
)
