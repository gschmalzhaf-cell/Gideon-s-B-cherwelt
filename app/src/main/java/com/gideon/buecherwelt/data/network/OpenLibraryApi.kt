package com.gideon.buecherwelt.data.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

@Serializable
data class OlBook(
    val title: String? = null,
    val subtitle: String? = null,
    val authors: List<OlAuthorRef>? = null,
    val publishers: List<String>? = null,
    @SerialName("publish_date") val publishDate: String? = null,
    val covers: List<Int>? = null,
    @SerialName("cover_i") val coverId: Int? = null,
    val subjects: List<String>? = null,
    val series: List<String>? = null,
    @SerialName("number_of_pages") val numberOfPages: Int? = null
) {
    val bestCoverId: Int?
        get() = covers?.firstOrNull { it > 0 } ?: coverId?.takeIf { it > 0 }
}

@Serializable
data class OlAuthorRef(
    val key: String? = null
)

@Serializable
data class OlAuthor(
    val name: String? = null
)

@Serializable
data class OlSearchResponse(
    val docs: List<OlDoc> = emptyList()
)

@Serializable
data class OlDoc(
    val title: String? = null,
    val subtitle: String? = null,
    @SerialName("author_name") val authorName: List<String>? = null,
    @SerialName("first_publish_year") val firstPublishYear: Int? = null,
    val publisher: List<String>? = null,
    val isbn: List<String>? = null,
    @SerialName("cover_i") val coverId: Int? = null,
    val subjects: List<String>? = null,
    val series: List<String>? = null,
    @SerialName("number_of_pages_median") val numberOfPagesMedian: Int? = null,
    val language: List<String>? = null
)

interface OpenLibraryApi {

    @GET("isbn/{isbn}.json")
    suspend fun getByIsbn(@Path("isbn") isbn: String): OlBook

    @GET("authors/{key}.json")
    suspend fun getAuthor(@Path("key") key: String): OlAuthor

    @GET("search.json")
    suspend fun search(@Query("q") query: String, @Query("limit") limit: Int = 20): OlSearchResponse

    @GET("search.json")
    suspend fun searchByTitleAuthor(
        @Query("title") title: String,
        @Query("author") author: String? = null,
        @Query("limit") limit: Int = 10
    ): OlSearchResponse
}
