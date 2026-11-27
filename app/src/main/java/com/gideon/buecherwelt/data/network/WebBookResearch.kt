package com.gideon.buecherwelt.data.network

import android.text.Html
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLDecoder
import java.net.URLEncoder

/**
 * Internet-Recherche als Fallback für den Cover-Scan:
 * Wenn ein Buch weder per LLM noch per OCR + Katalog (Google Books / Open Library / DNB)
 * erkannt wird, wird das offene Web durchsucht (DuckDuckGo / Bing, HTML-Endpunkte ohne
 * API-Key) – mit den auf dem Cover erkannten Texten. Die Top-Treffer (z. B. Autor:innen-Seite,
 * Verlag, Buchhandlung) werden geladen und Titel / Autor:in / ISBN / Cover / Jahr werden aus
 * den Meta-Tags der Seite extrahiert. Jedes Buch ist irgendwo auffindbar.
 */
class WebBookResearch(private val okHttpClient: OkHttpClient) {

    /** Ein Treffer einer Websuche. */
    data class WebHit(val url: String, val title: String)

    /**
     * Sucht im Web nach einem Buch anhand der OCR-Cover-Zeilen und optional eines
     * LLM-erkannten Titels/Autors. Liefert die Metadaten des ersten plausiblen Treffers.
     */
    suspend fun research(ocrLines: List<String>, llmTitle: String?, llmAuthor: String?): BookMetadata? =
        withContext(Dispatchers.IO) {
            val queries = buildQueries(ocrLines, llmTitle, llmAuthor)
            for (query in queries) {
                var hits = searchWeb(query)
                if (hits.isEmpty()) hits = searchWeb("$query buch")
                for (hit in hits.filter { it.url.startsWith("http") }.take(4)) {
                    val html = fetch(hit.url) ?: continue
                    val meta = extractMetadata(html, query)
                    if (meta != null && isPlausible(meta, query)) {
                        return@withContext meta
                    }
                }
            }
            null
        }

    /** Baut Suchanfragen aus LLM-Ergebnis und OCR-Zeilen (Titel/Autor-Kombinationen zuerst). */
    private fun buildQueries(ocrLines: List<String>, llmTitle: String?, llmAuthor: String?): List<String> {
        val out = mutableListOf<String>()
        if (!llmTitle.isNullOrBlank()) {
            out += if (!llmAuthor.isNullOrBlank()) "$llmTitle $llmAuthor" else llmTitle.trim()
        }
        val lines = ocrLines
            .map { it.trim() }
            .filter { it.any(Char::isLetter) && it.length in 4..80 }
            .distinct()
            .take(6)
        if (lines.size >= 2) out += "${lines[0]} ${lines[1]}"
        if (lines.size >= 3) out += "${lines[0]} ${lines[1]} ${lines[2]}"
        out += lines.take(3)
        return out
            .map { it.replace(Regex("\\s+"), " ").trim() }
            .filter { it.length >= 3 }
            .distinct()
            .take(4)
    }

    /** Websuche über DuckDuckGo (html + lite) und Bing – jeweils ohne API-Key. */
    private suspend fun searchWeb(query: String): List<WebHit> {
        val q = URLEncoder.encode(query, "UTF-8")
        val ddg = fetch("https://html.duckduckgo.com/html/?q=$q")?.let(::parseDdg).orEmpty()
        if (ddg.isNotEmpty()) return ddg
        val lite = fetch("https://lite.duckduckgo.com/lite/?q=$q")?.let(::parseDdgLite).orEmpty()
        if (lite.isNotEmpty()) return lite
        return fetch("https://www.bing.com/search?q=$q&count=15")?.let(::parseBing).orEmpty()
    }

    private fun parseDdg(html: String): List<WebHit> =
        Regex(
            "<a[^>]+class=\"[^\"]*result__a[^\"]*\"[^>]+href=\"([^\"]+)\"[^>]*>(.*?)</a>",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
        ).findAll(html).mapNotNull { m ->
            val url = decodeDdgRedirect(m.groupValues[1])
            val title = cleanText(m.groupValues[2])
            if (url.startsWith("http") && title.isNotBlank()) WebHit(url, title) else null
        }.toList()

    private fun parseDdgLite(html: String): List<WebHit> =
        Regex(
            "<a[^>]+class='result-link'[^>]+href=\"([^\"]+)\"[^>]*>(.*?)</a>",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
        ).findAll(html).mapNotNull { m ->
            val url = m.groupValues[1]
            val title = cleanText(m.groupValues[2])
            if (url.startsWith("http") && title.isNotBlank()) WebHit(url, title) else null
        }.toList()

    private fun parseBing(html: String): List<WebHit> =
        Regex(
            "<h2[^>]*>\\s*<a[^>]+href=\"([^\"]+)\"[^>]*>(.*?)</a>",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
        ).findAll(html).mapNotNull { m ->
            val url = m.groupValues[1]
            val title = cleanText(m.groupValues[2])
            if (url.startsWith("http") && title.isNotBlank()) WebHit(url, title) else null
        }.toList()

    private fun decodeDdgRedirect(href: String): String {
        if (href.contains("uddg=")) {
            val enc = Regex("uddg=([^&]+)").find(href)?.groupValues?.get(1)
            if (enc != null) return runCatching { URLDecoder.decode(enc, "UTF-8") }.getOrDefault(href)
        }
        return if (href.startsWith("//")) "https:$href" else href
    }

    /** Lädt eine Seite (GET, Browser-UA) und liefert den HTML-Body (gekappt). */
    private suspend fun fetch(url: String): String? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) GideonsBuecherwelt/1.7")
                .header("Accept", "text/html,application/xhtml+xml")
                .get()
                .build()
            okHttpClient.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext null
                val body = resp.body?.string() ?: return@withContext null
                if (body.length > 1_500_000) body.take(1_500_000) else body
            }
        } catch (e: Exception) {
            null
        }
    }

    /** Extrahiert Buch-Metadaten aus dem HTML einer Trefferseite. */
    private fun extractMetadata(html: String, query: String): BookMetadata? {
        val metas = metaTags(html)
        val titleTag = Regex("<title[^>]*>(.*?)</title>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
            .find(html)?.groupValues?.get(1).orEmpty()
        var title = cleanText(metas["og:title"] ?: titleTag)
        if (title.length < 3) title = cleanText(query)
        val isbnRaw = Regex("97[89][0-9\\-\\s]{9,16}[0-9Xx]").find(html)?.value
        val isbn = isbnRaw?.let { runCatching { BookMetadataFetcher.normalizeIsbn(it) }.getOrNull() }
        val author = cleanText(metas["author"].orEmpty()).take(80)
        val description = cleanText(metas["og:description"] ?: metas["description"].orEmpty()).take(800)
        val coverUrl = metas["og:image"]?.take(500)
        val year = Regex("\\b(19|20)\\d{2}\\b").find(description)?.value?.toIntOrNull()
        return BookMetadata(
            isbn = isbn,
            title = title,
            author = author,
            description = description,
            coverUrl = coverUrl,
            publishedYear = year
        )
    }

    /** Nur plausible Treffer akzeptieren: ISBN vorhanden oder Titel enthält Suchwort. */
    private fun isPlausible(meta: BookMetadata, query: String): Boolean {
        val t = meta.title.lowercase()
        if (t.length < 3 || t.length > 120) return false
        if (meta.isbn != null) return true
        val words = query.lowercase()
            .split(Regex("[^a-zäöüß0-9]+"))
            .filter { it.length >= 4 && it !in setOf("buch", "books", "roman", "novel") }
        return words.any { t.contains(it) }
    }

    /** Alle <meta>-Tags als Attribut-Map (property/name -> content). */
    private fun metaTags(html: String): Map<String, String> {
        val map = mutableMapOf<String, String>()
        Regex("<meta\\b[^>]*>", RegexOption.IGNORE_CASE).findAll(html).forEach { m ->
            val tag = m.value
            val key = attr(tag, "property") ?: attr(tag, "name") ?: return@forEach
            val content = attr(tag, "content") ?: return@forEach
            map[key.lowercase()] = content
        }
        return map
    }

    private fun attr(tag: String, name: String): String? =
        Regex("$name\\s*=\\s*\"([^\"]*)\"", RegexOption.IGNORE_CASE).find(tag)?.groupValues?.get(1)
            ?: Regex("$name\\s*=\\s*'([^']*)'", RegexOption.IGNORE_CASE).find(tag)?.groupValues?.get(1)

    private fun cleanText(s: String): String =
        Html.fromHtml(s, Html.FROM_HTML_MODE_LEGACY).toString()
            .replace(Regex("\\s+"), " ")
            .let { if (it.contains(" | ")) it.substringBefore(" | ") else it }
            .trim()
            .trimEnd('-', '–', '—', ':', ';', '.', ',')
}
