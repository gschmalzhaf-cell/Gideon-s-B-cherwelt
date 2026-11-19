package com.gideon.buecherwelt.data.network

import android.util.Xml
import okhttp3.OkHttpClient
import okhttp3.Request
import org.xmlpull.v1.XmlPullParser
import retrofit2.HttpException
import java.io.IOException
import java.io.StringReader

sealed interface SearchListResult {
    data class Success(val items: List<BookMetadata>) : SearchListResult
    data object Empty : SearchListResult
    data object NoNetwork : SearchListResult
    data class Error(val message: String) : SearchListResult
}

class BookMetadataFetcher(
    private val googleBooksApi: GoogleBooksApi,
    private val openLibraryApi: OpenLibraryApi,
    private val okHttpClient: OkHttpClient
) {

    suspend fun fetchByIdentifier(raw: String): FetchResult {
        val candidates = classifyIdentifier(raw)
        if (candidates.isEmpty()) {
            return FetchResult.Error(
                "Ungültige Eingabe – erwartet ISBN (10/13 Zeichen), ASIN (z. B. B0…), Amazon-Link oder Goodreads-ID (Zahl)."
            )
        }
        return try {
            for ((kind, value) in candidates) {
                val metadata = when (kind) {
                    IdentifierKind.ISBN -> fetchIsbnMetadata(value)
                    IdentifierKind.ASIN -> fetchAsinMetadata(value)
                    IdentifierKind.GOODREADS -> olSearchFirst("id_goodreads:$value")?.toMetadata()?.enrichMetadata()
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
            if (olSearch == null) {
                val doc = olSearchFirst("isbn:$v")
                if (doc != null && !doc.title.isNullOrBlank()) olSearch = doc.toMetadata()
            }
            if (gb != null && ol != null && olSearch != null) break
        }
        var metadata = merge(isbn = isbn, gb = gb, ol = ol, olAuthors = olAuthors)
        if (olSearch != null) {
            val s = olSearch!!
            metadata = metadata.copy(
                title = metadata.title.ifBlank { s.title },
                subtitle = metadata.subtitle.ifBlank { s.subtitle },
                author = metadata.author.ifBlank { s.author },
                publisher = metadata.publisher.ifBlank { s.publisher },
                publishedYear = metadata.publishedYear ?: s.publishedYear,
                categories = metadata.categories.ifEmpty { s.categories },
                coverUrl = metadata.coverUrl ?: s.coverUrl,
                pageCount = metadata.pageCount ?: s.pageCount,
                language = metadata.language.ifBlank { s.language },
                seriesHints = (metadata.seriesHints + s.seriesHints).distinct()
            )
        }
        if (metadata.title.isBlank()) {
            for (v in variants) {
                val dnb = fetchDnb(v)
                if (dnb != null && dnb.title.isNotBlank()) {
                    metadata = metadata.copy(
                        title = dnb.title,
                        subtitle = metadata.subtitle.ifBlank { dnb.subtitle },
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
        // If ISBN lookup found title/author but lacked coverUrl or description, enrich via title+author search!
        if (metadata.title.isNotBlank() && (metadata.coverUrl.isNullOrBlank() || metadata.description.isBlank())) {
            val enriched = searchAndRankByTitleAuthor(metadata.title, metadata.author).firstOrNull()
            if (enriched != null) {
                metadata = metadata.copy(
                    subtitle = metadata.subtitle.ifBlank { enriched.subtitle },
                    author = metadata.author.ifBlank { enriched.author },
                    publisher = metadata.publisher.ifBlank { enriched.publisher },
                    publishedYear = metadata.publishedYear ?: enriched.publishedYear,
                    description = metadata.description.ifBlank { enriched.description },
                    categories = metadata.categories.ifEmpty { enriched.categories },
                    coverUrl = metadata.coverUrl ?: enriched.coverUrl,
                    pageCount = metadata.pageCount ?: enriched.pageCount,
                    language = metadata.language.ifBlank { enriched.language },
                    seriesHints = (metadata.seriesHints + enriched.seriesHints).distinct()
                )
            }
        }
        if (metadata.coverUrl.isNullOrBlank() && metadata.title.isNotBlank()) {
            metadata = metadata.copy(
                coverUrl = "https://covers.openlibrary.org/b/isbn/$isbn-L.jpg?default=false"
            )
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

    private fun parseDnbDc(xml: String, isbn: String?): BookMetadata? {
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
        val beforeSlash = title
            .replace(Regex("^\\[[^\\]]*\\]\\s*;\\s*"), "")
            .substringBefore(" / ")
            .trim()
        val cleanTitle = beforeSlash.substringBefore(" : ").trim()
        val subtitle = if (" : " in beforeSlash) beforeSlash.substringAfter(" : ").trim() else ""
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
            subtitle = subtitle,
            author = cleanAuthor,
            publisher = cleanPublisher,
            publishedYear = year,
            categories = categories
        )
    }

    private suspend fun olSearchFirst(query: String): OlDoc? =
        callApi { openLibraryApi.search(query, 5) }?.docs?.firstOrNull()

    private suspend fun fetchAsinMetadata(asin: String): BookMetadata? {
        // 1. Open Library: id_amazon field
        val fromOlAmazon = olSearchFirst("id_amazon:$asin")?.toMetadata()
        if (fromOlAmazon != null && fromOlAmazon.title.isNotBlank()) {
            return fromOlAmazon.enrichMetadata()
        }

        // 2. Open Library: general search with ASIN
        val fromOlGeneral = olSearchFirst(asin)?.toMetadata()
        if (fromOlGeneral != null && fromOlGeneral.title.isNotBlank()) {
            return fromOlGeneral.enrichMetadata()
        }

        // 3. Google Books: general search with ASIN
        val fromGb = callApi { googleBooksApi.search(asin, 5) }
            ?.items.orEmpty()
            .mapNotNull { it.volumeInfo?.let { v -> toMetadata(null, v) } }
            .firstOrNull { it.title.isNotBlank() }
        if (fromGb != null) {
            return fromGb.enrichMetadata()
        }

        // 4. Audnex public API (covers Audible / Amazon ASINs)
        val fromAudnex = fetchAudnex(asin)
        if (fromAudnex != null && fromAudnex.title.isNotBlank()) {
            return fromAudnex.enrichMetadata()
        }

        return null
    }

    private suspend fun fetchAudnex(asin: String): BookMetadata? {
        for (region in listOf("de", "us")) {
            val url = "https://api.audnex.us/books/$asin?region=$region"
            val body = callApi {
                okHttpClient.newCall(Request.Builder().url(url).build()).execute().use { resp ->
                    if (resp.isSuccessful) resp.body?.string() else null
                }
            } ?: continue
            val parsed = parseAudnexJson(body)
            if (parsed != null && parsed.title.isNotBlank()) return parsed
        }
        return null
    }

    private fun parseAudnexJson(raw: String): BookMetadata? = try {
        val obj = org.json.JSONObject(raw)
        val title = obj.optString("title").trim()
        if (title.isBlank()) return null
        val subtitle = obj.optString("subtitle").trim()
        val authorsArr = obj.optJSONArray("authors")
        val authors = buildList {
            if (authorsArr != null) {
                for (i in 0 until authorsArr.length()) {
                    val name = authorsArr.optJSONObject(i)?.optString("name")?.trim().orEmpty()
                    if (name.isNotBlank()) add(name)
                }
            }
        }.joinToString(", ")
        val publisher = obj.optString("publisherName").trim()
        val year = parseYear(obj.optString("releaseDate"))
        val summary = obj.optString("summary")
            .replace(Regex("<[^>]+>"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
        val image = obj.optString("image").trim().ifBlank { null }
        val language = obj.optString("language").trim()
        val genresArr = obj.optJSONArray("genres")
        val categories = buildList {
            if (genresArr != null) {
                for (i in 0 until genresArr.length()) {
                    val name = genresArr.optJSONObject(i)?.optString("name")?.trim().orEmpty()
                    if (name.isNotBlank()) add(name)
                }
            }
        }.distinct().take(6)
        val seriesPrimary = obj.optJSONObject("seriesPrimary")
        val seriesHints = buildList {
            if (seriesPrimary != null) {
                val sName = seriesPrimary.optString("name").trim()
                val sPos = seriesPrimary.optString("position").trim()
                if (sName.isNotBlank()) {
                    add(if (sPos.isNotBlank()) "$sName #$sPos" else sName)
                }
            }
        }
        val isbn = normalizeIsbn(obj.optString("isbn"))
        BookMetadata(
            isbn = isbn,
            title = title,
            subtitle = subtitle,
            author = authors,
            publisher = publisher,
            publishedYear = year,
            description = summary,
            categories = categories,
            coverUrl = image,
            language = language,
            seriesHints = seriesHints
        )
    } catch (_: Exception) {
        null
    }

    private suspend fun BookMetadata.enrichMetadata(): BookMetadata {
        var current = isbn?.let { fetchIsbnMetadata(it) } ?: this
        if (current.title.isNotBlank() && (current.coverUrl.isNullOrBlank() || current.description.isBlank() || current.pageCount == null)) {
            val ranked = searchAndRankByTitleAuthor(current.title, current.author, maxResults = 8).firstOrNull()
            if (ranked != null) {
                current = current.copy(
                    isbn = current.isbn ?: ranked.isbn,
                    subtitle = current.subtitle.ifBlank { ranked.subtitle },
                    author = current.author.ifBlank { ranked.author },
                    publisher = current.publisher.ifBlank { ranked.publisher },
                    publishedYear = current.publishedYear ?: ranked.publishedYear,
                    description = current.description.ifBlank { ranked.description },
                    categories = current.categories.ifEmpty { ranked.categories },
                    coverUrl = current.coverUrl ?: ranked.coverUrl,
                    pageCount = current.pageCount ?: ranked.pageCount,
                    language = current.language.ifBlank { ranked.language },
                    seriesHints = (current.seriesHints + ranked.seriesHints).distinct()
                )
            }
        }
        return current
    }

    private fun OlDoc.toMetadata(): BookMetadata = BookMetadata(
        isbn = isbn?.firstOrNull { it.length == 13 } ?: isbn?.firstOrNull(),
        title = title ?: "",
        subtitle = subtitle ?: "",
        author = authorName?.joinToString(", ").orEmpty(),
        publisher = publisher?.firstOrNull().orEmpty(),
        publishedYear = firstPublishYear,
        categories = subjects?.take(6).orEmpty(),
        coverUrl = coverId?.takeIf { it > 0 }?.let { "https://covers.openlibrary.org/b/id/$it-L.jpg" },
        pageCount = numberOfPagesMedian?.takeIf { it > 0 },
        language = language?.firstOrNull().orEmpty(),
        seriesHints = series.orEmpty()
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
            else -> query.replace("intitle:", "").replace("inauthor:", "")
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

    private suspend fun searchAndRankByTitleAuthor(
        title: String,
        author: String,
        maxResults: Int = 12
    ): List<BookMetadata> {
        val cleanTitle = title.trim()
        if (cleanTitle.isBlank()) return emptyList()
        val baseTitle = stripSeriesFromTitle(cleanTitle)
        val cleanAuthor = author.trim()
        val primaryAuthor = cleanAuthor.substringBefore(",").substringBefore("&").trim()

        val allCandidates = mutableListOf<BookMetadata>()

        // 1. Google Books precise query
        val gbPreciseQuery = buildString {
            append("intitle:\"$baseTitle\"")
            if (primaryAuthor.isNotBlank()) append(" inauthor:\"$primaryAuthor\"")
        }
        val gbPrecise = callApi { googleBooksApi.search(gbPreciseQuery, maxResults) }
            ?.items.orEmpty()
            .mapNotNull { it.volumeInfo?.let { v -> toMetadata(null, v) } }
        allCandidates += gbPrecise

        // 2. Google Books broader query (if precise had few results or none with cover)
        if (allCandidates.none { !it.coverUrl.isNullOrBlank() } || allCandidates.size < 3) {
            val gbBroadQuery = buildString {
                append(baseTitle)
                if (primaryAuthor.isNotBlank()) append(" $primaryAuthor")
            }
            val gbBroad = callApi { googleBooksApi.search(gbBroadQuery, maxResults) }
                ?.items.orEmpty()
                .mapNotNull { it.volumeInfo?.let { v -> toMetadata(null, v) } }
            allCandidates += gbBroad
        }

        // 3. Open Library dedicated title + author search
        val olDocs = callApi {
            openLibraryApi.searchByTitleAuthor(
                title = baseTitle,
                author = primaryAuthor.ifBlank { null },
                limit = 10
            )
        }?.docs.orEmpty().map { it.toMetadata() }
        allCandidates += olDocs

        if (allCandidates.isEmpty()) return emptyList()

        val scored = allCandidates
            .filter { it.title.isNotBlank() }
            .map { candidate -> candidate to scoreCandidate(candidate, cleanTitle, baseTitle, primaryAuthor) }
            .filter { it.second > -30 }
            .sortedByDescending { it.second }

        if (scored.isEmpty()) return emptyList()

        // Merge top candidate with best cover and best description from same-book editions
        val ranked = scored.map { it.first }
        val top = ranked.first()
        val bestCover = ranked.firstOrNull { !it.coverUrl.isNullOrBlank() }?.coverUrl
        val bestDesc = ranked.firstOrNull { it.description.length > top.description.length }?.description ?: top.description
        val bestSubtitle = ranked.firstOrNull { it.subtitle.isNotBlank() }?.subtitle ?: top.subtitle
        val bestPages = ranked.firstOrNull { (it.pageCount ?: 0) > 0 }?.pageCount ?: top.pageCount
        val bestYear = top.publishedYear ?: ranked.firstOrNull { it.publishedYear != null }?.publishedYear
        val bestPublisher = top.publisher.ifBlank { ranked.firstOrNull { it.publisher.isNotBlank() }?.publisher.orEmpty() }
        val bestCategories = (top.categories + ranked.flatMap { it.categories }).distinct().take(6)
        val bestSeriesHints = ranked.flatMap { it.seriesHints }.distinct()

        val mergedTop = top.copy(
            coverUrl = top.coverUrl ?: bestCover,
            description = bestDesc,
            subtitle = top.subtitle.ifBlank { bestSubtitle },
            pageCount = bestPages,
            publishedYear = bestYear,
            publisher = bestPublisher,
            categories = bestCategories,
            seriesHints = bestSeriesHints
        )

        return listOf(mergedTop) + ranked.drop(1)
    }

    suspend fun findByTitleAuthor(title: String, author: String): FetchResult = try {
        val ranked = searchAndRankByTitleAuthor(title, author)
        val best = ranked.firstOrNull()
        if (best != null && (!best.coverUrl.isNullOrBlank() || best.description.isNotBlank())) {
            return FetchResult.Success(best)
        }
        val baseTitle = stripSeriesFromTitle(title.trim())
        val cql = buildString {
            append("tit=\"${baseTitle.replace("\"", "")}\"")
            if (author.isNotBlank()) append(" and per=\"${author.trim().replace("\"", "")}\"")
        }
        val dnb = fetchDnbQuery(cql)
        if (dnb != null && dnb.title.isNotBlank()) {
            val isbn = dnb.isbn
            if (isbn != null) {
                val byIsbn = fetchByIdentifier(isbn)
                if (byIsbn is FetchResult.Success) return byIsbn
            }
            return FetchResult.Success(
                if (best != null) {
                    dnb.copy(
                        coverUrl = dnb.coverUrl ?: best.coverUrl,
                        description = dnb.description.ifBlank { best.description },
                        pageCount = dnb.pageCount ?: best.pageCount,
                        seriesHints = (dnb.seriesHints + best.seriesHints).distinct()
                    )
                } else dnb
            )
        }
        if (best != null) FetchResult.Success(best) else FetchResult.NotFound
    } catch (e: NoNetworkException) {
        FetchResult.NoNetwork
    } catch (e: Exception) {
        FetchResult.Error(e.message ?: "Unbekannter Fehler.")
    }

    /**
     * Liefert mehrere passende Cover-Kandidaten (verschiedene Ausgaben von Google Books & Open Library)
     * für die manuelle oder automatische Cover-Auswahl auf der Buchdetailseite.
     */
    suspend fun findCoverCandidates(
        title: String,
        author: String,
        isbn: String? = null
    ): List<BookMetadata> = try {
        val list = mutableListOf<BookMetadata>()
        if (!isbn.isNullOrBlank()) {
            when (val byIsbn = fetchByIsbn(isbn)) {
                is FetchResult.Success -> if (!byIsbn.metadata.coverUrl.isNullOrBlank()) {
                    list += byIsbn.metadata
                }
                else -> Unit
            }
            val cleanIsbn = normalizeIsbn(isbn)
            if (cleanIsbn != null) {
                list += BookMetadata(
                    isbn = cleanIsbn,
                    title = title,
                    author = author,
                    publisher = "Open Library ISBN-Cover",
                    coverUrl = "https://covers.openlibrary.org/b/isbn/$cleanIsbn-L.jpg?default=false"
                )
            }
        }
        list += searchAndRankByTitleAuthor(title, author, maxResults = 15)
            .filter { !it.coverUrl.isNullOrBlank() }

        list.distinctBy { it.coverUrl }.take(8)
    } catch (e: Exception) {
        emptyList()
    }

    private fun stripSeriesFromTitle(raw: String): String =
        raw.replace(Regex("\\s*\\([^()]*\\)\\s*$"), "")
            .substringBefore(" – Band ")
            .substringBefore(" - Band ")
            .substringBefore(": Band ")
            .substringBefore(" – Teil ")
            .substringBefore(" - Teil ")
            .trim()
            .ifBlank { raw.trim() }

    private fun scoreCandidate(
        candidate: BookMetadata,
        fullTitle: String,
        baseTitle: String,
        author: String
    ): Int {
        var score = 0
        val candTitle = candidate.title.trim().lowercase()
        val targetFull = fullTitle.trim().lowercase()
        val targetBase = baseTitle.trim().lowercase()
        val candAuthor = candidate.author.trim().lowercase()
        val targetAuthor = author.trim().lowercase()

        // Reject spam / study guides / summaries if not requested
        val spamKeywords = listOf(
            "summary of", "study guide", "workbook", "trivia", "analysis of",
            "lektürehilfe", "zusammenfassung", "coloring book", "malbuch", "notizbuch"
        )
        if (spamKeywords.any { kw -> candTitle.contains(kw) && !targetFull.contains(kw) }) {
            score -= 120
        }

        // Title match
        when {
            candTitle == targetFull || candTitle == targetBase -> score += 55
            candTitle.startsWith(targetBase) || targetBase.startsWith(candTitle) -> score += 38
            candTitle.contains(targetBase) || targetBase.contains(candTitle) -> score += 26
            else -> {
                val targetWords = targetBase.split(Regex("\\s+")).filter { it.length >= 3 }
                if (targetWords.isNotEmpty()) {
                    val matchedWords = targetWords.count { w -> candTitle.contains(w) }
                    score += (matchedWords * 25) / targetWords.size
                }
            }
        }

        // Author match
        if (targetAuthor.isNotBlank()) {
            val authorTokens = targetAuthor.split(Regex("\\s+")).filter { it.length >= 3 }
            val lastName = authorTokens.lastOrNull().orEmpty()
            when {
                candAuthor.contains(targetAuthor) || targetAuthor.contains(candAuthor) && candAuthor.isNotBlank() -> score += 45
                lastName.isNotBlank() && candAuthor.contains(lastName) -> score += 35
                candAuthor.isBlank() -> score -= 10
                else -> score -= 55
            }
        }

        // Cover availability
        if (!candidate.coverUrl.isNullOrBlank()) {
            score += 35
        }

        // Rich metadata bonus
        if (candidate.description.length > 60) score += 15
        if ((candidate.pageCount ?: 0) > 0) score += 5
        if (candidate.categories.isNotEmpty()) score += 5
        if (candidate.language.equals("de", ignoreCase = true)) score += 6

        return score
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
        val coverFromGb = gb?.imageLinks?.bestUrl()
        val coverFromOl = ol?.bestCoverId?.let { "https://covers.openlibrary.org/b/id/$it-L.jpg" }
        return BookMetadata(
            isbn = isbn,
            title = gb?.title?.ifBlank { null } ?: ol?.title.orEmpty(),
            subtitle = gb?.subtitle?.ifBlank { null } ?: ol?.subtitle.orEmpty(),
            author = gb?.authors?.joinToString(", ")?.ifBlank { null }
                ?: olAuthors.joinToString(", "),
            publisher = gb?.publisher?.ifBlank { null }
                ?: ol?.publishers?.firstOrNull().orEmpty(),
            publishedYear = parseYear(gb?.publishedDate) ?: parseYear(ol?.publishDate),
            description = gb?.description.orEmpty(),
            categories = gb?.categories?.take(6)
                ?: ol?.subjects?.map { it.substringBefore('/') }?.distinct()?.take(6)
                ?: emptyList(),
            coverUrl = coverFromGb ?: coverFromOl,
            pageCount = gb?.pageCount?.takeIf { it > 0 } ?: ol?.numberOfPages?.takeIf { it > 0 },
            language = gb?.language.orEmpty(),
            seriesHints = ol?.series.orEmpty()
        )
    }

    private fun toMetadata(isbn: String?, info: GbVolumeInfo): BookMetadata {
        val identifiers = info.industryIdentifiers.orEmpty()
        val isbn13 = identifiers.firstOrNull { it.type == "ISBN_13" }?.identifier
        val isbn10 = identifiers.firstOrNull { it.type == "ISBN_10" }?.identifier
        return BookMetadata(
            isbn = isbn ?: isbn13 ?: isbn10,
            title = info.title.orEmpty(),
            subtitle = info.subtitle.orEmpty(),
            author = info.authors?.joinToString(", ").orEmpty(),
            publisher = info.publisher.orEmpty(),
            publishedYear = parseYear(info.publishedDate),
            description = info.description.orEmpty(),
            categories = info.categories?.take(6).orEmpty(),
            coverUrl = info.imageLinks?.bestUrl(),
            pageCount = info.pageCount?.takeIf { it > 0 },
            language = info.language.orEmpty()
        )
    }

    companion object {
        enum class IdentifierKind { ISBN, ASIN, GOODREADS }

        private val amazonUrlRegex = Regex("(?:dp|gp/product|ASIN)/([A-Z0-9]{10})", RegexOption.IGNORE_CASE)

        fun classifyIdentifier(raw: String): List<Pair<IdentifierKind, String>> {
            val extracted = amazonUrlRegex.find(raw)?.groupValues?.getOrNull(1) ?: raw
            val cleaned = extracted.trim().uppercase().filter { it.isLetterOrDigit() }
            val result = mutableListOf<Pair<IdentifierKind, String>>()
            normalizeIsbn(extracted)?.let { result += IdentifierKind.ISBN to it }
            if (cleaned.length == 10 && cleaned[0] in listOf('B', 'A') && cleaned.any { it.isLetter() }) {
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
