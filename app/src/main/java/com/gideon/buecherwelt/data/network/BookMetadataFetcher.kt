package com.gideon.buecherwelt.data.network

import android.util.Xml
import okhttp3.OkHttpClient
import okhttp3.Request
import org.xmlpull.v1.XmlPullParser
import retrofit2.HttpException
import java.io.IOException
import java.io.StringReader

class BookMetadataFetcher(
    private val googleBooksApi: GoogleBooksApi,
    private val openLibraryApi: OpenLibraryApi,
    private val okHttpClient: OkHttpClient
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
        val variants = isbnVariants(isbn)
        var gb: GbVolumeInfo? = null
        var ol: OlBook? = null
        var olAuthors: List<String> = emptyList()
        var olSearch: BookMetadata? = null
        for (v in variants) {
            if (gb == null) {
                gb = callApi { googleBooksApi.searchByIsbn("isbn:$v").items.firstOrNull()?.volumeInfo }
            }
            if (ol == null) {
                ol = callApi { openLibraryApi.getByIsbn(v) }
                if (ol != null) {
                    olAuthors = ol!!.authors.orEmpty().mapNotNull { ref ->
                        ref.key?.substringAfterLast('/')?.let { key ->
                            callApi { openLibraryApi.getAuthor(key)?.name }
                        }
                    }
                }
            }
            if (ol == null && olSearch == null) {
                val doc = olSearchFirst("isbn:$v")
                if (doc != null && !doc.title.isNullOrBlank()) olSearch = doc.toMetadata()
            }
            if (gb != null && (ol != null || olSearch != null)) break
        }
        var metadata = merge(isbn = isbn, gb = gb, ol = ol, olAuthors = olAuthors)
        if (metadata.title.isBlank() && olSearch != null) {
            val s = olSearch!!
            metadata = metadata.copy(
                title = s.title,
                author = metadata.author.ifBlank { s.author },
                publisher = metadata.publisher.ifBlank { s.publisher },
                publishedYear = metadata.publishedYear ?: s.publishedYear,
                categories = metadata.categories.ifEmpty { s.categories },
                coverUrl = metadata.coverUrl ?: s.coverUrl
            )
        }
        if (metadata.title.isBlank()) {
            for (v in variants) {
                val dnb = fetchDnb(v)
                if (dnb != null && dnb.title.isNotBlank()) {
                    metadata = metadata.copy(
                        title = dnb.title,
                        author = metadata.author.ifBlank { dnb.author },
                        publisher = metadata.publisher.ifBlank { dnb.publisher },
                        publishedYear = metadata.publishedYear ?: dnb.publishedYear,
                        categories = metadata.categories.ifEmpty { dnb.categories },
                        description = metadata.description.ifBlank { dnb.description }
                    )
                    break
                }
            }
        }
        return metadata
    }

    private suspend fun fetchDnb(isbn: String): BookMetadata? =
        fetchDnbQuery("isbn=$isbn", fallbackIsbn = isbn)

    private suspend fun fetchDnbQuery(cql: String, fallbackIsbn: String? = null): BookMetadata? {
        val encoded = java.net.URLEncoder.encode(cql, "UTF-8").replace("+", "%20")
        val url = "https://services.dnb.de/sru/dnb?version=1.1&operation=searchRetrieve" +
            "&query=$encoded&recordSchema=oai_dc&maximumRecords=5"
        val body = callApi {
            okHttpClient.newCall(Request.Builder().url(url).build()).execute().use { resp ->
                if (resp.isSuccessful) resp.body?.string() else null
            }
        } ?: return null
        return parseDnbDc(body, fallbackIsbn)
    }

    private fun parseDnbDc(xml: String, isbn: String): BookMetadata? {
        val parser = Xml.newPullParser()
        parser.setInput(StringReader(xml))
        var title = ""
        var creator = ""
        var publisher = ""
        var date = ""
        val subjects = mutableListOf<String>()
        var tag = ""
        var records = 0
        val isbns = mutableListOf<String>()
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT && records <= 1) {
            if (event == XmlPullParser.START_TAG) {
                tag = parser.name.substringAfterLast(':')
                if (tag == "record") records++
            } else if (event == XmlPullParser.TEXT) {
                val text = parser.text?.trim().orEmpty()
                if (text.isNotEmpty()) {
                    when (tag) {
                        "title" -> if (title.isEmpty()) title = text
                        "creator" -> if (creator.isEmpty()) creator = text
                        "publisher" -> if (publisher.isEmpty()) publisher = text
                        "date" -> if (date.isEmpty()) date = text
                        "subject" -> if (subjects.size < 6) subjects += text
                        "identifier" -> {
                            val normalized = normalizeIsbn(text)
                            if (normalized != null && normalized !in isbns) isbns += normalized
                        }
                    }
                }
            }
            event = parser.next()
        }
        if (title.isEmpty()) return null
        val cleanTitle = title
            .replace(Regex("^\\[[^\\]]*\\]\\s*;\\s*"), "")
            .substringBefore(" / ")
            .substringBefore(" : ")
            .trim()
        val cleanAuthor = creator
            .replace(Regex("\\s*\\[[^\\]]*\\]$"), "")
            .let { c -> if (", " in c) c.split(", ", limit = 2).let { (l, f) -> "$f $l" } else c }
            .trim()
        val cleanPublisher = publisher.substringAfterLast(": ").trim()
        val year = Regex("\\d{4}").find(date)?.value?.toIntOrNull()
        val categories = subjects
            .map { s -> Regex("^[0-9A-Z]{1,3}\\s+(.*)$").find(s)?.groupValues?.get(1) ?: s }
            .distinct()
        return BookMetadata(
            isbn = isbns.firstOrNull { it.length == 13 } ?: isbns.firstOrNull() ?: isbn,
            title = cleanTitle,
            author = cleanAuthor,
            publisher = cleanPublisher,
            publishedYear = year,
            categories = categories
        )
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
        if (best != null) return FetchResult.Success(best)
        val cql = buildString {
            append("tit=\"${title.trim().replace("\"", "")}\"")
            if (author.isNotBlank()) append(" and per=\"${author.trim().replace("\"", "")}\"")
        }
        val dnb = fetchDnbQuery(cql)
        if (dnb != null && dnb.title.isNotBlank()) {
            val isbn = dnb.isbn
            if (isbn != null) {
                val byIsbn = fetchByIdentifier(isbn)
                if (byIsbn is FetchResult.Success) return byIsbn
            }
            return FetchResult.Success(dnb)
        }
        FetchResult.NotFound
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

        fun isbnVariants(isbn: String): List<String> {
            val result = mutableListOf(isbn)
            if (isbn.length == 13 && isbn.startsWith("978")) {
                val core = isbn.substring(3, 12)
                val sum = core.mapIndexed { i, c -> c.digitToInt() * (10 - i) }.sum()
                val check = (11 - sum % 11) % 11
                result += core + (if (check == 10) "X" else check.toString())
            } else if (isbn.length == 10) {
                val core = isbn.take(9)
                val sum = core.mapIndexed { i, c -> c.digitToInt() * (1 + 2 * i) }.sum()
                val check = (10 - sum % 10) % 10
                result += "978$core$check"
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
