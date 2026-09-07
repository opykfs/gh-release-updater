package com.example.githubupdater.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** GitHub Release 的单个 asset（附件）DTO。 */
@Serializable
data class ReleaseAsset(
    val name: String = "",
    @SerialName("browser_download_url") val browserDownloadUrl: String? = null,
    val size: Long = 0,
    @SerialName("content_type") val contentType: String? = null,
)
