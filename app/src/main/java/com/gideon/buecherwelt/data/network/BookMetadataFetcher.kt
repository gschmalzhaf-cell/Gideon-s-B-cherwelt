package com.gideon.buecherwelt.data.network

import retrofit2.HttpException
import java.io.IOException

class BookMetadataFetcher(
    private val googleBooksApi: GoogleBooksApi,
    private val openLibraryApi: OpenLibraryApi
) {

    suspend fun fetchByIsbn(rawIsbn: String): FetchResult {
        val isbn = normalizeIsbn(rawIsbn)
            ?: return FetchResult.Error("Ungültige ISBN – 10 oder 13 Zeichen (Ziffern, ggf. X).")
        return try {
            val gb = callApi { googleBooksApi.searchByIsbn("isbn:$isbn").items.firstOrNull()?.volumeInfo }
            val ol = callApi { openLibraryApi.getByIsbn(isbn) }
            val olAuthors = ol?.authors.orEmpty().mapNotNull { ref ->
                ref.key?.substringAfterLast('/')?.let { key ->
                    callApi { openLibraryApi.getAuthor(key)?.name }
                }
            }
            val metadata = merge(isbn = isbn, gb = gb, ol = ol, olAuthors = olAuthors)
            if (metadata.title.isBlank()) FetchResult.NotFound else FetchResult.Success(metadata)
        } catch (e: NoNetworkException) {
            FetchResult.NoNetwork
        } catch (e: Exception) {
            FetchResult.Error(e.message ?: "Unbekannter Fehler bei der ISBN-Abfrage.")
        }
    }

    suspend fun search(query: String, maxResults: Int = 20): List<BookMetadata> = try {
        val isbn = normalizeIsbn(query)
        val effectiveQuery = if (isbn != null) "isbn:$isbn" else query
        val gb = callApi { googleBooksApi.search(effectiveQuery, maxResults) }?.items.orEmpty()
        val ol = callApi { openLibraryApi.search(effectiveQuery, maxResults) }?.docs.orEmpty()
        val fromGb = gb.mapNotNull { it.volumeInfo?.let { v -> toMetadata(null, v) } }
        val fromOl = ol.map { d ->
            BookMetadata(
                isbn = d.isbn?.firstOrNull { it.length == 13 } ?: d.isbn?.firstOrNull(),
                title = d.title ?: "",
                author = d.authorName?.joinToString(", ").orEmpty(),
                publisher = d.publisher?.firstOrNull().orEmpty(),
                publishedYear = d.firstPublishYear,
                categories = d.subjects?.take(5).orEmpty(),
                coverUrl = d.coverId?.let { "https://covers.openlibrary.org/b/id/$it-M.jpg" }
            )
        }
        (fromGb + fromOl)
    } catch (e: NoNetworkException) {
        emptyList()
    } catch (e: Exception) {
        emptyList()
    }

    suspend fun findByTitleAuthor(title: String, author: String): FetchResult = try {
        val q = buildString {
            append("intitle:${title.trim()}")
            if (author.isNotBlank()) append(" inauthor:${author.trim()}")
        }
        val results = search(q, 5)
        val best = results.firstOrNull { it.title.isNotBlank() }
            ?: return FetchResult.NotFound
        FetchResult.Success(best)
    } catch (e: NoNetworkException) {
        FetchResult.NoNetwork
    } catch (e: Exception) {
        FetchResult.Error(e.message ?: "Unbekannter Fehler.")
    }

    private suspend fun <T> callApi(block: suspend () -> T): T? = try {
        block()
    } catch (e: IOException) {
        throw NoNetworkException()
    } catch (e: HttpException) {
        null
    } catch (e: Exception) {
        null
    }

    private fun merge(
        isbn: String,
        gb: GbVolumeInfo?,
        ol: OlBook?,
        olAuthors: List<String>
    ): BookMetadata {
        val coverFromGb = gb?.imageLinks?.thumbnail
            ?.replace("http://", "https://")
            ?.replace("&edge=curl", "")
        val coverFromOl = ol?.coverId?.let { "https://covers.openlibrary.org/b/id/$it-L.jpg" }
        return BookMetadata(
            isbn = isbn,
            title = gb?.title?.ifBlank { null } ?: ol?.title.orEmpty(),
            author = gb?.authors?.joinToString(", ")?.ifBlank { null }
                ?: olAuthors.joinToString(", "),
            publisher = gb?.publisher?.ifBlank { null }
                ?: ol?.publishers?.firstOrNull().orEmpty(),
            publishedYear = parseYear(gb?.publishedDate) ?: parseYear(ol?.publishDate),
            description = gb?.description.orEmpty(),
            categories = gb?.categories?.take(6)
                ?: ol?.subjects?.map { it.substringBefore('/') }?.distinct()?.take(6)
                ?: emptyList(),
            coverUrl = coverFromGb ?: coverFromOl
        )
    }

    private fun toMetadata(isbn: String?, info: GbVolumeInfo): BookMetadata {
        val identifiers = info.industryIdentifiers.orEmpty()
        val isbn13 = identifiers.firstOrNull { it.type == "ISBN_13" }?.identifier
        val isbn10 = identifiers.firstOrNull { it.type == "ISBN_10" }?.identifier
        return BookMetadata(
            isbn = isbn ?: isbn13 ?: isbn10,
            title = info.title.orEmpty(),
            author = info.authors?.joinToString(", ").orEmpty(),
            publisher = info.publisher.orEmpty(),
            publishedYear = parseYear(info.publishedDate),
            description = info.description.orEmpty(),
            categories = info.categories?.take(6).orEmpty(),
            coverUrl = info.imageLinks?.thumbnail
                ?.replace("http://", "https://")
                ?.replace("&edge=curl", "")
        )
    }

    companion object {
        fun normalizeIsbn(raw: String): String? {
            val cleaned = raw.trim().uppercase().filter { it.isDigit() || it == 'X' }
            if (cleaned.length != 10 && cleaned.length != 13) return null
            if (cleaned.length == 10 && cleaned.dropLast(1).any { !it.isDigit() }) return null
            if (cleaned.length == 13 && cleaned.any { !it.isDigit() }) return null
            return cleaned
        }

        fun parseYear(raw: String?): Int? =
            raw?.let { Regex("\\d{4}").find(it)?.value?.toIntOrNull() }
    }
}
