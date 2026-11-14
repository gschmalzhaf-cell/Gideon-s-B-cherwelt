package com.gideon.buecherwelt.data.csv

import android.content.Context
import android.net.Uri
import com.gideon.buecherwelt.data.local.ReadingStatus
import com.gideon.buecherwelt.data.network.BookMetadata
import com.gideon.buecherwelt.data.network.BookMetadataFetcher
import com.gideon.buecherwelt.data.network.CoverCache
import com.gideon.buecherwelt.data.network.FetchResult
import com.gideon.buecherwelt.domain.model.Book
import com.gideon.buecherwelt.domain.repository.BookRepository
import com.gideon.buecherwelt.domain.repository.TropeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class StoryGraphCsvImporter(
    private val context: Context,
    private val fetcher: BookMetadataFetcher,
    private val coverCache: CoverCache,
    private val bookRepository: BookRepository,
    private val tropeRepository: TropeRepository
) {

    data class Row(
        val title: String,
        val author: String,
        val isbn: String?,
        val rating: Int?,
        val status: ReadingStatus,
        val tags: List<String>
    )

    data class Progress(
        val done: Int,
        val total: Int,
        val currentTitle: String,
        val imported: Int,
        val skipped: Int
    )

    data class Summary(
        val imported: Int,
        val skipped: Int,
        val messages: List<String>
    )

    suspend fun import(
        uri: Uri,
        onProgress: (Progress) -> Unit
    ): Summary = withContext(Dispatchers.IO) {
        val text = try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                input.bufferedReader().readText()
            }
        } catch (e: Exception) {
            null
        } ?: return@withContext Summary(0, 0, listOf("Datei konnte nicht gelesen werden."))

        val rows = parseCsv(text).mapNotNull { mapRow(it) }
        if (rows.isEmpty()) {
            return@withContext Summary(0, 0, listOf("Keine Bucheinträge in der CSV gefunden."))
        }
        val messages = mutableListOf<String>()
        var imported = 0
        var skipped = 0
        rows.forEachIndexed { index, row ->
            onProgress(Progress(index + 1, rows.size, row.title, imported, skipped))
            try {
                val duplicate = bookRepository.findDuplicate(row.title, row.author, row.isbn)
                if (duplicate != null) {
                    skipped++
                    messages.add("Übersprungen (Duplikat): ${row.title}")
                    return@forEachIndexed
                }
                var metadata: BookMetadata? = null
                if (!row.isbn.isNullOrBlank()) {
                    when (val result = fetcher.fetchByIsbn(row.isbn)) {
                        is FetchResult.Success -> metadata = result.metadata
                        else -> Unit
                    }
                }
                if (metadata == null && row.title.isNotBlank()) {
                    when (val result = fetcher.findByTitleAuthor(row.title, row.author)) {
                        is FetchResult.Success -> metadata = result.metadata
                        else -> Unit
                    }
                }
                val base = metadata ?: BookMetadata(
                    isbn = row.isbn,
                    title = row.title,
                    author = row.author
                )
                val coverPath = base.coverUrl?.let {
                    coverCache.download(it, CoverCache.keyFor(base.isbn, base.title, base.author))
                }
                val now = System.currentTimeMillis()
                val book = Book(
                    id = 0,
                    isbn = (base.isbn ?: row.isbn)?.ifBlank { null },
                    title = base.title.ifBlank { row.title },
                    author = base.author.ifBlank { row.author },
                    publisher = base.publisher,
                    publishedYear = base.publishedYear,
                    description = base.description,
                    coverUrl = base.coverUrl,
                    coverPath = coverPath,
                    genre = base.categories.firstOrNull().orEmpty(),
                    subgenre = "",
                    spiceLevel = null,
                    rating = row.rating,
                    status = row.status,
                    notes = "",
                    tropes = emptyList(),
                    createdAt = now,
                    updatedAt = now
                )
                val tropeIds = tropeRepository.resolveIds(row.tags)
                bookRepository.insert(book, tropeIds)
                imported++
            } catch (e: Exception) {
                skipped++
                messages.add("Fehler bei „${row.title}“: ${e.message}")
            }
        }
        Summary(imported = imported, skipped = skipped, messages = messages)
    }

    fun parseCsv(text: String): List<Map<String, String>> {
        val records = mutableListOf<List<String>>()
        var field = StringBuilder()
        var row = mutableListOf<String>()
        var inQuotes = false
        var i = 0
        while (i < text.length) {
            val c = text[i]
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < text.length && text[i + 1] == '"') {
                        field.append('"')
                        i++
                    } else {
                        inQuotes = false
                    }
                } else {
                    field.append(c)
                }
            } else {
                when (c) {
                    '"' -> inQuotes = true
                    ',' -> {
                        row.add(field.toString())
                        field = StringBuilder()
                    }
                    '\n' -> {
                        row.add(field.toString())
                        records.add(row)
                        row = mutableListOf()
                        field = StringBuilder()
                    }
                    '\r' -> Unit
                    else -> field.append(c)
                }
            }
            i++
        }
        if (field.isNotEmpty() || row.isNotEmpty()) {
            row.add(field.toString())
            records.add(row)
        }
        if (records.isEmpty()) return emptyList()
        val header = records.first().map { it.trim().lowercase() }
        return records.drop(1)
            .filter { record -> record.any { it.isNotBlank() } }
            .map { record ->
                header.mapIndexedNotNull { index, key ->
                    if (key.isNotBlank() && index < record.size) key to record[index].trim() else null
                }.toMap()
            }
    }

    private fun mapRow(columns: Map<String, String>): Row? {
        fun find(vararg keys: String): String? =
            keys.firstNotNullOfOrNull { key -> columns.entries.firstOrNull { it.key == key }?.value }
                ?: columns.entries.firstOrNull { it.key.contains(keys.first()) }?.value

        val title = find("title")?.takeIf { it.isNotBlank() } ?: return null
        val author = find("author")?.takeIf { it.isNotBlank() && !it.contains("additional") }
            ?: columns["author"]?.takeIf { it.isNotBlank() } ?: ""
        val isbn = find("isbn13") ?: find("isbn")
        val ratingRaw = find("my rating")
        val rating = ratingRaw?.toIntOrNull()?.takeIf { it in 1..5 }
        val statusRaw = columns.entries.firstOrNull { it.key.contains("status") }?.value
        val status = mapStatus(statusRaw)
        val tagsRaw = find("tags") ?: find("bookshelves") ?: ""
        val tags = tagsRaw.split(",", ";").map { it.trim() }.filter { it.isNotBlank() }
        return Row(
            title = title,
            author = author,
            isbn = isbn?.ifBlank { null },
            rating = rating,
            status = status,
            tags = tags
        )
    }

    private fun mapStatus(raw: String?): ReadingStatus {
        val normalized = raw?.lowercase()?.replace('_', '-')?.replace(' ', '-')?.trim().orEmpty()
        return when (normalized) {
            "read", "finished" -> ReadingStatus.READ
            "currently-reading", "reading", "currently-reading-book" -> ReadingStatus.READING
            "to-read", "to read", "planned", "want-to-read" -> ReadingStatus.PLANNED
            "stopped-reading", "abandoned", "dnf", "did-not-finish" -> ReadingStatus.ABANDONED
            else -> ReadingStatus.PLANNED
        }
    }
}
