package com.gideon.buecherwelt.data.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Query

@Serializable
data class GbVolumesResponse(
    @SerialName("totalItems") val totalItems: Int = 0,
    val items: List<GbVolume> = emptyList()
)

@Serializable
data class GbVolume(
    val volumeInfo: GbVolumeInfo? = null
)

@Serializable
data class GbVolumeInfo(
    val title: String? = null,
    val subtitle: String? = null,
    val authors: List<String>? = null,
    val publisher: String? = null,
    @SerialName("publishedDate") val publishedDate: String? = null,
    val description: String? = null,
    val categories: List<String>? = null,
    val pageCount: Int? = null,
    val language: String? = null,
    val imageLinks: GbImageLinks? = null,
    @SerialName("industryIdentifiers") val industryIdentifiers: List<GbIdentifier>? = null
)

@Serializable
data class GbImageLinks(
    val extraLarge: String? = null,
    val large: String? = null,
    val medium: String? = null,
    val small: String? = null,
    val thumbnail: String? = null,
    val smallThumbnail: String? = null
) {
    fun bestUrl(): String? =
        (large ?: medium ?: small ?: thumbnail ?: extraLarge ?: smallThumbnail)
            ?.replace("http://", "https://")
            ?.replace("&edge=curl", "")
}

@Serializable
data class GbIdentifier(
    val type: String? = null,
    val identifier: String? = null
)

interface GoogleBooksApi {

    @GET("books/v1/volumes")
    suspend fun search(@Query("q") query: String, @Query("maxResults") maxResults: Int = 20): GbVolumesResponse

    @GET("books/v1/volumes")
    suspend fun searchByIsbn(@Query("q") query: String): GbVolumesResponse
}
