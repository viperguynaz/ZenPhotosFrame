package dev.zenrg.zenphotoframe.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ErrorBody(
    @SerialName("error")
    val error: Error? = null
)

@Serializable
data class Detail(
    @SerialName("domain")
    val domain: String? = null,
    @SerialName("metadata")
    val metadata: Metadata? = null,
    @SerialName("reason")
    val reason: String? = null,
    @SerialName("@type")
    val type: String? = null
)


@Serializable
data class Error(
    @SerialName("code")
    val code: Int? = null,
    @SerialName("details")
    val details: List<Detail>? = null,
    @SerialName("message")
    val message: String? = null,
    @SerialName("status")
    val status: String? = null
)


@Serializable
data class Metadata(
    @SerialName("method")
    val method: String? = null,
    @SerialName("service")
    val service: String? = null
)