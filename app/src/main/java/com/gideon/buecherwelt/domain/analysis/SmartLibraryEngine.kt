package com.gideon.buecherwelt.domain.analysis

import com.gideon.buecherwelt.data.local.ReadingStatus
import com.gideon.buecherwelt.domain.model.Book
import kotlin.math.abs

enum class ReadingMood(val label: String, val emoji: String) {
    ALL("Alle", "✨"),
    NEXT_IN_SERIES("Fortsetzung", "📖"),
    DARK("Düster", "🌙"),
    ROMANTIC("Romantisch", "❤️"),
    MAGICAL("Magisch", "🐉"),
    THRILLING("Spannend", "⚡"),
    COZY("Wohlfühl", "☕"),
    SHORT("Kurz", "⏱")
}

data class SmartSearchResult(
    val bookIds: List<Long>,
    val explanation: String,
    val activeHints: List<String> = emptyList()
)

data class SeriesProgress(
    val seriesName: String,
    val universeName: String,
    val author: String,
    val ownedBooks: List<Book>,
    val readCount: Int,
    val readingCount: Int,
    val plannedCount: Int,
    val nextUnreadBook: Book?,
    val missingIndices: List<Int>,
    val highestOwnedIndex: Int?
) {
    val totalOwned: Int get() = ownedBooks.size
    val progressFraction: Float
        get() = if (totalOwned == 0) 0f else readCount.toFloat() / totalOwned.toFloat()
}

data class SimilarLocalBook(
    val book: Book,
    val matchPercent: Int,
    val reasons: List<String>
)

data class ReadingDnaSummary(
    val totalReadPages: Int = 0,
    val estimatedReadHours: Double = 0.0,
    val subBookCount: Int = 0,
    val estimatedSubHours: Double = 0.0,
    val avgPageCount: Int? = null,
    val topMoods: List<Pair<String, Int>> = emptyList(),
    val activeSeriesCount: Int = 0,
    val totalMissingVolumes: Int = 0
)

/**
 * Lokale Bibliotheks-Intelligenz (100 % offline):
 * - Natürliche Stimmungs- & Intent-Suche (ohne API-Key)
 * - Reihen-Wächter (Fortschritt, nächster Band im Regal, Lücken-Erkennung)
 * - Lokale Ähnlichkeitssuche im eigenen Bücherregal
 * - Stimmungsbasierter SuB-Berater & Lese-DNA-Auswertung
 */
object SmartLibraryEngine {

    private val stopWords = setOf(
        "ein", "eine", "einen", "einem", "einer", "der", "die", "das", "den", "dem", "des",
        "und", "oder", "aber", "mit", "ohne", "für", "von", "vom", "zu", "zum", "zur", "in",
        "im", "auf", "aus", "bei", "nach", "über", "unter", "nicht", "kein", "keine", "sehr",
        "viel", "wenig", "mehr", "buch", "bücher", "büchern", "roman", "romane", "geschichte",
        "mir", "mich", "ich", "mein", "meine", "suche", "zeige", "bitte", "etwas", "was",
        "the", "and", "with", "for", "book", "books", "about", "that", "this"
    )

    private val moodKeywordMap: List<Pair<String, List<String>>> = listOf(
        "Magisch & Episch" to listOf("magisch", "magie", "fantasy", "drache", "drachen", "episch", "fae", "zauber", "elfen", "romantasy"),
        "Düster & Geheimnisvoll" to listOf("düster", "dunkel", "dark", "schatten", "geheimnis", "mafia", "blut", "fluch", "abgrund", "morally grey"),
        "Emotional & Romantisch" to listOf("romantisch", "romance", "liebe", "herz", "gefühle", "leidenschaft", "kuss", "knisternd", "liebesgeschichte"),
        "Spannend & Nervenaufreibend" to listOf("spannend", "thriller", "krimi", "mord", "spannung", "pageturner", "atemlos", "jagd", "survival", "twist"),
        "Warmherzig & Charakterstark" to listOf("warmherzig", "cozy", "wohlfühl", "humor", "lustig", "freundschaft", "sommer", "familie", "leicht")
    )

    fun localSmartSearch(
        query: String,
        books: List<Book>,
        profile: ReadingProfile
    ): SmartSearchResult {
        val raw = query.trim()
        if (raw.isBlank() || books.isEmpty()) {
            return SmartSearchResult(emptyList(), "Bitte gib eine Stimmung, ein Thema oder Suchbegriffe ein.")
        }
        val lower = raw.lowercase()
        val hints = mutableListOf<String>()

        // 1. Status-Intents erkennen
        val statusFilter: ReadingStatus? = when {
            listOf("ungelesen", "sub", "tbr", "geplant", "als nächstes", "nächstes highlight", "was lesen", "noch nicht gelesen").any { lower.contains(it) } -> {
                hints += "Ungelesen (SuB)"
                ReadingStatus.PLANNED
            }
            listOf("aktuell", "lesend", "gerade", "lesepult").any { lower.contains(it) } -> {
                hints += "Aktuell lesend"
                ReadingStatus.READING
            }
            listOf("abgebrochen", "dnf").any { lower.contains(it) } -> {
                hints += "Abgebrochen"
                ReadingStatus.ABANDONED
            }
            listOf("gelesen", "fertig", "beendet").any { lower.contains(it) } -> {
                hints += "Gelesen"
                ReadingStatus.READ
            }
            else -> null
        }

        // 2. Bewertungs-Intents
        val minRating: Int? = when {
            listOf("5 sterne", "fünf sterne", "meisterwerk", "top-bewertet").any { lower.contains(it) } -> {
                hints += "5 ★"
                5
            }
            listOf("4 sterne", "vier sterne", "favorit", "lieblings", "gut bewertet", "beste").any { lower.contains(it) } -> {
                hints += "4–5 ★"
                4
            }
            else -> null
        }

        // 2b. „Unbewertet“-Intent (Bücher ohne Sterne-Bewertung)
        var onlyUnrated = false
        if (listOf("unbewertet", "ohne bewertung", "nicht bewertet", "noch nicht bewertet").any { lower.contains(it) }) {
            onlyUnrated = true
            hints += "Ohne Bewertung"
        }

        // 3. Spice-Intents
        var minSpice: Int? = null
        var maxSpice: Int? = null
        when {
            listOf("ohne spice", "kein spice", "clean", "fade to black", "ohne smut").any { lower.contains(it) } -> {
                maxSpice = 1
                hints += "Ohne / wenig Spice"
            }
            listOf("wenig spice", "mild", "sanft").any { lower.contains(it) } -> {
                maxSpice = 2
                hints += "Milder Spice (≤ 2)"
            }
            listOf("sehr spicy", "viel spice", "open door", "smut", "erotisch", "heiß", "explicit").any { lower.contains(it) } -> {
                minSpice = 3
                hints += "Hoher Spice (≥ 3)"
            }
            listOf("spicy", "spice", "knisternd").any { lower.contains(it) } -> {
                minSpice = 2
                hints += "Mit Spice (≥ 2)"
            }
        }

        // 4. Längen-Intents
        var maxPages: Int? = null
        var minPages: Int? = null
        when {
            listOf("kurz", "schnell", "wochenende", "unter 300", "unter 350", "dünn", "zwischendurch").any { lower.contains(it) } -> {
                maxPages = 360
                hints += "Kurz (≤ 360 S.)"
            }
            listOf("dick", "lang", "wälzer", "über 500", "episch lang").any { lower.contains(it) } -> {
                minPages = 480
                hints += "Umfangreich (≥ 480 S.)"
            }
        }

        // 5. Stimmungs-Intents
        val desiredMoods = mutableListOf<String>()
        for ((moodName, keywords) in moodKeywordMap) {
            if (keywords.any { lower.contains(it) }) {
                desiredMoods += moodName
                hints += moodName
            }
        }

        // 6. Spezial-Intent: Profil-Highlight / Empfehlung
        val highlightIntent = listOf("highlight", "empfehlung", "passt zu mir", "was lesen", "als nächstes").any { lower.contains(it) }
        if (highlightIntent) {
            hints += "Höchster Profil-Match"
        }

        // 7. Freitext-Tokens für direkte & semantische Treffer
        val tokens = lower
            .split(Regex("[^a-zäöüß0-9]+"))
            .filter { it.length >= 3 && it !in stopWords }
            .distinct()

        val scored = books.mapNotNull { book ->
            // Harte Filter prüfen (Status, Rating)
            if (statusFilter != null && book.status != statusFilter) return@mapNotNull null
            if (minRating != null && (book.rating ?: 0) < minRating) return@mapNotNull null
            if (onlyUnrated && book.rating != null) return@mapNotNull null

            val chars = ReadingStatsCalculator.analyzeBookCharacteristics(book)
            val effectiveSpice = book.spiceLevel ?: 0
            if (maxSpice != null && effectiveSpice > maxSpice) return@mapNotNull null
            if (minSpice != null && effectiveSpice < minSpice) return@mapNotNull null
            if (maxPages != null && book.pageCount != null && book.pageCount > maxPages) return@mapNotNull null
            if (minPages != null && book.pageCount != null && book.pageCount < minPages) return@mapNotNull null

            var score = 0

            // Mood-Matches
            if (desiredMoods.isNotEmpty()) {
                val moodHits = desiredMoods.count { dm -> chars.moods.any { it.equals(dm, ignoreCase = true) } }
                score += moodHits * 22
            }

            // Pacing & Setting Matches
            if (lower.contains("slow burn") && chars.pacing.contains("Slow Burn", ignoreCase = true)) score += 20
            if (lower.contains("pageturner") && chars.pacing.contains("Pageturner", ignoreCase = true)) score += 20
            chars.settingHints.forEach { setting ->
                if (tokens.any { setting.lowercase().contains(it) }) score += 14
            }

            // Direkte Feld-Matches pro Token
            val titleLower = "${book.title} ${book.subtitle}".lowercase()
            val authorLower = book.author.lowercase()
            val seriesUniLower = "${book.seriesName} ${book.universeName}".lowercase()
            val genreLower = "${book.genre} ${book.subgenre}".lowercase()
            val tropesLower = book.tropes.map { it.name.lowercase() }
            val descNotesLower = "${book.description} ${book.notes}".lowercase()

            for (tok in tokens) {
                if (titleLower.contains(tok)) score += 18
                if (authorLower.contains(tok)) score += 16
                if (seriesUniLower.contains(tok)) score += 15
                if (genreLower.contains(tok)) score += 14
                if (tropesLower.any { it.contains(tok) }) score += 16
                if (descNotesLower.contains(tok)) score += 6
            }

            // Bonus wenn reine Intent-Suche (z.B. "ungelesen", "kurz", "5 sterne", "nächstes highlight")
            val hasHardIntent = statusFilter != null || minRating != null || minSpice != null ||
                maxSpice != null || maxPages != null || minPages != null || highlightIntent || onlyUnrated
            if (hasHardIntent) {
                val fit = book.aiFitScore ?: ReadingStatsCalculator.computeLocalFit(book, profile, books).first
                score += if (highlightIntent) fit else (fit / 4)
                if (maxPages != null && book.pageCount != null) score += 10
                if (minPages != null && book.pageCount != null) score += 10
            }

            if (score > 0) book.id to score else null
        }
            .sortedByDescending { it.second }
            .take(30)

        val ids = scored.map { it.first }
        val explanation = when {
            ids.isEmpty() -> "Keine passenden Bücher für „$raw“ gefunden – probiere andere Stichworte oder Stimmungen."
            hints.isNotEmpty() -> "${ids.size} Treffer · Erkannt: ${hints.distinct().joinToString(" · ")}"
            else -> "${ids.size} passende Bücher in deiner Bibliothek zu „$raw“ gefunden."
        }
        return SmartSearchResult(
            bookIds = ids,
            explanation = explanation,
            activeHints = hints.distinct()
        )
    }

    /**
     * Analysiert alle Buchreihen in der Bibliothek:
     * - Wie viele Bände sind gelesen/lesend/geplant?
     * - Welcher Band liegt als direkte Fortsetzung im Regal bereit?
     * - Welche Bandnummern fehlen dazwischen (Lücken-Erkennung)?
     */
    fun analyzeSeriesProgress(books: List<Book>): List<SeriesProgress> {
        val grouped = books
            .filter { it.seriesName.isNotBlank() }
            .groupBy { it.seriesName.trim().lowercase() }

        return grouped.values.map { group ->
            val sorted = group.sortedWith(
                compareBy<Book> { it.seriesIndex ?: Int.MAX_VALUE }
                    .thenBy { it.publishedYear ?: Int.MAX_VALUE }
                    .thenBy { it.title.lowercase() }
            )
            val canonicalName = sorted.first().seriesName.trim()
            val universe = sorted.firstOrNull { it.universeName.isNotBlank() }?.universeName.orEmpty()
            val author = sorted.firstOrNull { it.author.isNotBlank() }?.author.orEmpty()

            val readCount = sorted.count { it.status == ReadingStatus.READ }
            val readingCount = sorted.count { it.status == ReadingStatus.READING }
            val plannedCount = sorted.count { it.status == ReadingStatus.PLANNED }

            val ownedIndices = sorted.mapNotNull { it.seriesIndex }.filter { it > 0 }.toSet()
            val highestIndex = ownedIndices.maxOrNull()
            val missing = if (highestIndex != null && highestIndex > 1) {
                (1 until highestIndex).filter { it !in ownedIndices }
            } else {
                emptyList()
            }

            // Nächster ungelesener Band in sinnvoller Reihenfolge
            val maxReadOrReadingIdx = sorted
                .filter { it.status == ReadingStatus.READ || it.status == ReadingStatus.READING }
                .mapNotNull { it.seriesIndex }
                .maxOrNull()

            val nextUnread = if (maxReadOrReadingIdx != null) {
                sorted.firstOrNull {
                    it.status == ReadingStatus.PLANNED &&
                        (it.seriesIndex == null || it.seriesIndex > maxReadOrReadingIdx)
                } ?: sorted.firstOrNull { it.status == ReadingStatus.PLANNED }
            } else {
                sorted.firstOrNull { it.status == ReadingStatus.PLANNED }
            }

            SeriesProgress(
                seriesName = canonicalName,
                universeName = universe,
                author = author,
                ownedBooks = sorted,
                readCount = readCount,
                readingCount = readingCount,
                plannedCount = plannedCount,
                nextUnreadBook = nextUnread,
                missingIndices = missing,
                highestOwnedIndex = highestIndex
            )
        }.sortedWith(
            compareByDescending<SeriesProgress> { it.readCount > 0 && it.nextUnreadBook != null }
                .thenByDescending { it.missingIndices.isNotEmpty() }
                .thenByDescending { it.totalOwned }
                .thenBy { it.seriesName.lowercase() }
        )
    }

    /**
     * Findet ähnliche Bücher direkt im eigenen Regal (100 % offline, sofort verfügbar).
     */
    fun findSimilarInLibrary(
        target: Book,
        allBooks: List<Book>,
        limit: Int = 6
    ): List<SimilarLocalBook> {
        val others = allBooks.filter { it.id != target.id }
        if (others.isEmpty()) return emptyList()

        val targetTropes = target.tropes.map { it.name.lowercase() }.toSet()
        val targetChars = ReadingStatsCalculator.analyzeBookCharacteristics(target)
        val targetMoods = targetChars.moods.toSet()

        return others.mapNotNull { candidate ->
            var points = 0
            val reasons = mutableListOf<String>()

            // 1. Gemeinsame Tropes
            val sharedTropes = candidate.tropes.filter { it.name.lowercase() in targetTropes }
            if (sharedTropes.isNotEmpty()) {
                points += (sharedTropes.size * 16).coerceAtMost(42)
                reasons += "Tropes: ${sharedTropes.take(2).joinToString(", ") { it.name }}"
            }

            // 2. Gleiche Autor:in
            if (target.author.isNotBlank() && candidate.author.equals(target.author, ignoreCase = true)) {
                points += 18
                reasons += "Gleiche Autor:in"
            }

            // 3. Gleiches Universum (aber andere Reihe oder alleinstehend)
            if (target.universeName.isNotBlank() &&
                candidate.universeName.equals(target.universeName, ignoreCase = true)
            ) {
                points += 16
                reasons += "Universum: ${target.universeName}"
            }

            // 4. Gleiches Genre / Subgenre
            if (target.genre.isNotBlank() && candidate.genre.equals(target.genre, ignoreCase = true)) {
                points += 14
                reasons += target.genre
            }
            if (target.subgenre.isNotBlank() && candidate.subgenre.equals(target.subgenre, ignoreCase = true)) {
                points += 8
            }

            // 5. Ähnliche Stimmung & Pacing
            val candChars = ReadingStatsCalculator.analyzeBookCharacteristics(candidate)
            val sharedMoods = candChars.moods.filter { it in targetMoods }
            if (sharedMoods.isNotEmpty()) {
                points += (sharedMoods.size * 9).coerceAtMost(18)
                if (reasons.size < 2) reasons += sharedMoods.first()
            }
            if (candChars.pacing == targetChars.pacing) {
                points += 5
            }

            // 6. Ähnlicher Spice-Level
            if (target.spiceLevel != null && candidate.spiceLevel != null &&
                abs(target.spiceLevel - candidate.spiceLevel) <= 1
            ) {
                points += 6
            }

            if (points < 22) return@mapNotNull null
            val percent = (38 + points).coerceIn(45, 99)
            SimilarLocalBook(
                book = candidate,
                matchPercent = percent,
                reasons = reasons.distinct().take(3)
            )
        }
            .sortedWith(
                compareByDescending<SimilarLocalBook> { it.matchPercent }
                    .thenByDescending { it.book.rating ?: 0 }
            )
            .take(limit)
    }

    /**
     * Stimmungsbasierter SuB-Berater: Filtert und priorisiert ungelesene Bücher
     * nach aktueller Leselaune und persönlichem Profil.
     */
    fun filterSubByMood(
        books: List<Book>,
        profile: ReadingProfile,
        mood: ReadingMood,
        limit: Int = 6
    ): List<Pair<Book, Pair<Int, String>>> {
        val planned = books.filter { it.status == ReadingStatus.PLANNED }
        if (planned.isEmpty()) return emptyList()

        if (mood == ReadingMood.ALL) {
            return planned
                .map { b -> b to ReadingStatsCalculator.computeLocalFit(b, profile, books) }
                .sortedByDescending { it.second.first }
                .take(limit)
        }

        if (mood == ReadingMood.NEXT_IN_SERIES) {
            val seriesNextIds = analyzeSeriesProgress(books)
                .filter { it.readCount > 0 || it.readingCount > 0 }
                .mapNotNull { it.nextUnreadBook?.id }
                .toSet()
            return planned
                .filter { it.id in seriesNextIds }
                .map { b ->
                    val (score, reason) = ReadingStatsCalculator.computeLocalFit(b, profile, books)
                    val boosted = (score + 10).coerceAtMost(99)
                    val label = if (b.seriesName.isNotBlank()) {
                        "Nächster Band in „${b.seriesName}“${b.seriesIndex?.let { " (#$it)" } ?: ""} · $reason"
                    } else reason
                    b to (boosted to label)
                }
                .sortedByDescending { it.second.first }
                .take(limit)
        }

        val filtered = planned.filter { book ->
            val chars = ReadingStatsCalculator.analyzeBookCharacteristics(book)
            when (mood) {
                ReadingMood.DARK -> chars.moods.any { it.contains("Düster", ignoreCase = true) }
                ReadingMood.ROMANTIC -> chars.moods.any { it.contains("Romantisch", ignoreCase = true) } || (book.spiceLevel ?: 0) >= 2
                ReadingMood.MAGICAL -> chars.moods.any { it.contains("Magisch", ignoreCase = true) }
                ReadingMood.THRILLING -> chars.moods.any { it.contains("Spannend", ignoreCase = true) }
                ReadingMood.COZY -> chars.moods.any { it.contains("Warmherzig", ignoreCase = true) }
                ReadingMood.SHORT -> (book.pageCount != null && book.pageCount <= 360)
                else -> true
            }
        }

        return filtered
            .map { b -> b to ReadingStatsCalculator.computeLocalFit(b, profile, books) }
            .sortedByDescending { it.second.first }
            .take(limit)
    }

    /**
     * Erstellt die Lese-DNA-Zusammenfassung (Seiten, Lesezeit, SuB-Umfang, Top-Stimmungen, Reihen-Check).
     */
    fun computeReadingDna(
        books: List<Book>,
        seriesProgress: List<SeriesProgress>
    ): ReadingDnaSummary {
        if (books.isEmpty()) return ReadingDnaSummary()
        val readBooks = books.filter { it.status == ReadingStatus.READ }
        val plannedBooks = books.filter { it.status == ReadingStatus.PLANNED }

        val readPages = readBooks.sumOf { it.pageCount ?: 350 }
        val readHours = (readPages / 50.0 * 10.0).toInt() / 10.0

        val subPages = plannedBooks.sumOf { it.pageCount ?: 350 }
        val subHours = (subPages / 50.0 * 10.0).toInt() / 10.0

        val knownPages = books.mapNotNull { it.pageCount?.takeIf { p -> p > 0 } }
        val avgPages = if (knownPages.isNotEmpty()) knownPages.average().toInt() else null

        val moodCounts = mutableMapOf<String, Int>()
        books.forEach { b ->
            val w = if (b.status == ReadingStatus.READ && (b.rating ?: 0) >= 4) 2 else 1
            ReadingStatsCalculator.analyzeBookCharacteristics(b).moods.forEach { m ->
                moodCounts[m] = (moodCounts[m] ?: 0) + w
            }
        }
        val topMoods = moodCounts.entries
            .sortedByDescending { it.value }
            .map { it.key to it.value }
            .take(4)

        val activeSeries = seriesProgress.count { it.readCount > 0 || it.readingCount > 0 }
        val totalMissing = seriesProgress.sumOf { it.missingIndices.size }

        return ReadingDnaSummary(
            totalReadPages = readPages,
            estimatedReadHours = readHours,
            subBookCount = plannedBooks.size,
            estimatedSubHours = subHours,
            avgPageCount = avgPages,
            topMoods = topMoods,
            activeSeriesCount = activeSeries,
            totalMissingVolumes = totalMissing
        )
    }
}
