package dev.zenrg.zenphotoframe.models

import kotlinx.serialization.Serializable

@Serializable
data class MediaSearchRequest(
    val albumId: String? = null,
    val filters: Filters? = null,
    val orderBy: String? = null,
    val pageSize: String? = null,
    val pageToken: String? = null
)

@Serializable
data class MediaSearchResponse(
    val mediaItems: List<MediaItem>? = null,
    val nextPageToken: String? = null
)

@Serializable
data class MediaItem(
    val baseUrl: String? = null,
    val contributorInfo: ContributorInfo? = null,
    val description: String? = null,
    val filename: String? = null,
    val id: String? = null,
    val mediaMetadata: MediaMetadata? = null,
    val mimeType: String? = null,
    val productUrl: String? = null
)

@Serializable
data class ContributorInfo(
    val profilePictureBaseUrl: String? = null,
    val displayName: String? = null,
)

@Serializable
data class MediaMetadata(
    val creationTime: String? = null,
    val height: String? = null,
    val photo: Photo? = null,
    val video: Video? = null,
    val width: String? = null
)

@Serializable
data class Photo(
    val apertureFNumber: Float? = null,
    val cameraMake: String? = null,
    val cameraModel: String? = null,
    val exposureTime: String? = null,
    val focalLength: Float? = null,
    val isoEquivalent: Int? = null
)

@Serializable
data class Video(
    val cameraMake: String? = null,
    val cameraModel: String? = null,
    val fps: Float? = null,
    val status: VideoProcessingStatus? = null
)

@Serializable
data class Filters(
    val dateFilter: DateFilter? = null,
    val contentFilter: ContentFilter? = null,
    val mediaTypeFilter: MediaTypeFilter? = null,
    val featureFilter: FeatureFilter? = null,
    val excludeNonAppCreatedData: Boolean? = false,
    val includeArchivedMedia: Boolean? = false,
)

@Serializable
data class DateFilter(
    val dates: List<MediaDate>? = null,
    val ranges: List<DateRange>? = null
)

@Serializable
data class ContentFilter(
    val includedContentCategories: List<ContentCategory>? = null,
    val excludedContentCategories: List<ContentCategory>? = null
)

@Serializable
data class MediaTypeFilter(
    val mediaTypes: List<MediaType>? = null
)

@Serializable
data class FeatureFilter(
    val includedFeatures: List<Feature>? = null
)

enum class ContentCategory {
    NONE, LANDSCAPES, RECEIPTS, CITYSCAPES, LANDMARKS, SELFIES, PEOPLE, PETS, WEDDINGS, BIRTHDAYS, DOCUMENTS, TRAVEL, ANIMALS, FOOD, SPORT, NIGHT, PERFORMANCES, WHITEBOARDS, SCREENSHOTS, UTILITY, ARTS, CRAFTS, FASHION, HOUSES, GARDENS, FLOWERS, HOLIDAYS
}

enum class MediaType {
    ALL_MEDIA, VIDEO, PHOTO
}

enum class Feature {
    NONE, FAVORITES
}

enum class VideoProcessingStatus {
    UNSPECIFIED, PROCESSING, READY, FAILED
}
