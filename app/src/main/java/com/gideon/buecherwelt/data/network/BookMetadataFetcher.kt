package com.gideon.buecherwelt.data.network

import retrofit2.HttpException
import java.io.IOException

class BookMetadataFetcher(
    private val googleBooksApi: GoogleBooksApi,
    private val openLibraryApi: OpenLibraryApi
) {

    suspend fun fetchByIdentifier(raw: String): FetchResult {
        val candidates = classifyIdentifier(raw)
        if (candidates.isEmpty()) {
            return FetchResult.Error(
                "Ungültige Eingabe – erwartet ISBN (10/13 Zeichen), ASIN (z. B. B0…) oder Goodreads-ID (Zahl)."
            )
        }
        return try {
            for ((kind, value) in candidates) {
                val metadata = when (kind) {
                    IdentifierKind.ISBN -> fetchIsbnMetadata(value)
                    IdentifierKind.ASIN -> olSearchFirst("id_amazon:$value")?.toMetadata()?.enrichByIsbn()
                    IdentifierKind.GOODREADS -> olSearchFirst("id_goodreads:$value")?.toMetadata()?.enrichByIsbn()
                }
                if (metadata != null && metadata.title.isNotBlank()) return FetchResult.Success(metadata)
            }
            FetchResult.NotFound
        } catch (e: NoNetworkException) {
            FetchResult.NoNetwork
        } catch (e: Exception) {
            FetchResult.Error(e.message ?: "Unbekannter Fehler bei der Abfrage.")
        }
    }

    suspend fun fetchByIsbn(rawIsbn: String): FetchResult = fetchByIdentifier(rawIsbn)

    private suspend fun fetchIsbnMetadata(isbn: String): BookMetadata? {
        val gb = callApi { googleBooksApi.searchByIsbn("isbn:$isbn").items.firstOrNull()?.volumeInfo }
        val ol = callApi { openLibraryApi.getByIsbn(isbn) }
        val olAuthors = ol?.authors.orEmpty().mapNotNull { ref ->
            ref.key?.substringAfterLast('/')?.let { key ->
                callApi { openLibraryApi.getAuthor(key)?.name }
            }
        }
        return merge(isbn = isbn, gb = gb, ol = ol, olAuthors = olAuthors)
    }

    private suspend fun olSearchFirst(query: String): OlDoc? =
        callApi { openLibraryApi.search(query, 5) }?.docs?.firstOrNull()

    private suspend fun BookMetadata.enrichByIsbn(): BookMetadata =
        isbn?.let { fetchIsbnMetadata(it) } ?: this

    private fun OlDoc.toMetadata(): BookMetadata = BookMetadata(
        isbn = isbn?.firstOrNull { it.length == 13 } ?: isbn?.firstOrNull(),
        title = title ?: "",
        author = authorName?.joinToString(", ").orEmpty(),
        publisher = publisher?.firstOrNull().orEmpty(),
        publishedYear = firstPublishYear,
        categories = subjects?.take(5).orEmpty(),
        coverUrl = coverId?.let { "https://covers.openlibrary.org/b/id/$it-M.jpg" }
    )

    suspend fun search(query: String, maxResults: Int = 20): List<BookMetadata> = try {
        val candidates = classifyIdentifier(query)
        val gbQuery = candidates.firstOrNull { it.first == IdentifierKind.ISBN }
            ?.let { "isbn:${it.second}" } ?: query
        val olQuery = when {
            candidates.any { it.first == IdentifierKind.ISBN } ->
                "isbn:" + candidates.first { it.first == IdentifierKind.ISBN }.second
            candidates.any { it.first == IdentifierKind.ASIN } ->
                "id_amazon:" + candidates.first { it.first == IdentifierKind.ASIN }.second
            candidates.any { it.first == IdentifierKind.GOODREADS } ->
                "id_goodreads:" + candidates.first { it.first == IdentifierKind.GOODREADS }.second
            else -> query
        }
        val gb = callApi { googleBooksApi.search(gbQuery, maxResults) }?.items.orEmpty()
        val ol = callApi { openLibraryApi.search(olQuery, maxResults) }?.docs.orEmpty()
        val fromGb = gb.mapNotNull { it.volumeInfo?.let { v -> toMetadata(null, v) } }
        val fromOl = ol.map { it.toMetadata() }
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
        enum class IdentifierKind { ISBN, ASIN, GOODREADS }

        fun classifyIdentifier(raw: String): List<Pair<IdentifierKind, String>> {
            val cleaned = raw.trim().uppercase().filter { it.isLetterOrDigit() }
            val result = mutableListOf<Pair<IdentifierKind, String>>()
            normalizeIsbn(raw)?.let { result += IdentifierKind.ISBN to it }
            if (cleaned.length == 10 && cleaned[0] == 'B') {
                result += IdentifierKind.ASIN to cleaned
            }
            if (cleaned.all { it.isDigit() } && cleaned.length in 5..12) {
                result += IdentifierKind.GOODREADS to cleaned
            }
            return result
        }

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
