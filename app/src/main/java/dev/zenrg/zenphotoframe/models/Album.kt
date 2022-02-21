package dev.zenrg.zenphotoframe.models

import kotlinx.serialization.Serializable

@Serializable
data class Album(
    val coverPhotoBaseUrl: String? = null,
    val coverPhotoMediaItemId: String? = null,
    val id: String? = null,
    val isWriteable: String? = null,
    val mediaItemsCount: String? = null,
    val productUrl: String? = null,
    val title: String? = null
)

@Serializable
data class Albums(
    val albums: List<Album>? = null,
    val nextPageToken: String? = null
)
