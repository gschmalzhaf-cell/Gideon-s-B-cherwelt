package com.gideon.buecherwelt.domain.repository

import com.gideon.buecherwelt.data.local.BookDao
import com.gideon.buecherwelt.data.local.BookEntity
import com.gideon.buecherwelt.data.local.BookTropeEntity
import com.gideon.buecherwelt.data.local.BookWithTropes
import com.gideon.buecherwelt.data.local.ReadingStatus
import com.gideon.buecherwelt.data.local.TropeDao
import com.gideon.buecherwelt.domain.model.Book
import com.gideon.buecherwelt.domain.model.Trope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class BookRepository(
    private val bookDao: BookDao,
    private val tropeDao: TropeDao
) {

    fun observeBooks(): Flow<List<Book>> =
        bookDao.observeBooks().map { list -> list.map { it.toDomain() } }

    fun observeBook(id: Long): Flow<Book?> =
        bookDao.observeBook(id).map { it?.toDomain() }

    suspend fun getBook(id: Long): Book? = bookDao.getBook(id)?.toDomain()

    suspend fun getAllBooks(): List<Book> = bookDao.getAllBooks().map { it.toDomain() }

    suspend fun findByIsbn(isbn: String): Book? =
        bookDao.findByIsbn(isbn)?.let { BookWithTropes(it).toDomain() }

    suspend fun findDuplicate(title: String, author: String, isbn: String?): Book? {
        if (!isbn.isNullOrBlank()) {
            findByIsbn(isbn.trim())?.let { return it }
        }
        return bookDao.findByTitleAuthor(title.trim(), author.trim())
            ?.let { BookWithTropes(it).toDomain() }
    }

    suspend fun insert(book: Book, tropeIds: Set<Long>): Long {
        val now = System.currentTimeMillis()
        val entity = book.toEntity(createdAt = now, updatedAt = now)
        val id = bookDao.insert(entity)
        setTropes(id, tropeIds)
        return id
    }

    suspend fun update(book: Book, tropeIds: Set<Long>) {
        val createdAt = if (book.createdAt == 0L) System.currentTimeMillis() else book.createdAt
        bookDao.update(book.toEntity(createdAt = createdAt, updatedAt = System.currentTimeMillis()))
        setTropes(book.id, tropeIds)
    }

    suspend fun delete(book: Book) {
        bookDao.delete(book.toEntity(book.createdAt, book.updatedAt))
    }

    suspend fun setTropes(bookId: Long, tropeIds: Set<Long>) {
        bookDao.clearTropes(bookId)
        tropeIds.forEach { tropeId -> bookDao.addTrope(BookTropeEntity(bookId, tropeId)) }
    }

    suspend fun saveAiInsights(bookId: Long, summary: String, fitScore: Int, fitReason: String) {
        bookDao.updateAiInsights(bookId, summary, fitScore, fitReason, System.currentTimeMillis())
    }

    suspend fun merge(existing: Book, incoming: Book, newTropeIds: Set<Long>): Book {
        val merged = existing.copy(
            title = existing.title.ifBlank { incoming.title },
            author = existing.author.ifBlank { incoming.author },
            publisher = existing.publisher.ifBlank { incoming.publisher },
            publishedYear = existing.publishedYear ?: incoming.publishedYear,
            description = existing.description.ifBlank { incoming.description },
            coverUrl = existing.coverUrl ?: incoming.coverUrl,
            coverPath = existing.coverPath ?: incoming.coverPath,
            genre = existing.genre.ifBlank { incoming.genre },
            subgenre = existing.subgenre.ifBlank { incoming.subgenre },
            spiceLevel = existing.spiceLevel ?: incoming.spiceLevel,
            rating = existing.rating ?: incoming.rating,
            status = if (existing.status == ReadingStatus.PLANNED) incoming.status else existing.status,
            notes = existing.notes.ifBlank { incoming.notes },
            updatedAt = System.currentTimeMillis()
        )
        val tropeIds = (existing.tropes.map { it.id } + newTropeIds).toSet()
        update(merged, tropeIds)
        return merged
    }

    private fun BookWithTropes.toDomain(): Book = Book(
        id = book.id,
        isbn = book.isbn,
        title = book.title,
        author = book.author,
        publisher = book.publisher,
        publishedYear = book.publishedYear,
        description = book.description,
        coverUrl = book.coverUrl,
        coverPath = book.coverPath,
        genre = book.genre,
        subgenre = book.subgenre,
        spiceLevel = book.spiceLevel,
        rating = book.rating,
        status = ReadingStatus.fromString(book.status),
        notes = book.notes,
        aiSummary = book.aiSummary,
        aiFitScore = book.aiFitScore,
        aiFitReason = book.aiFitReason,
        aiInsightsAt = book.aiInsightsAt,
        createdAt = book.createdAt,
        updatedAt = book.updatedAt,
        tropes = tropes.map { Trope(it.id, it.name, it.category, it.isCustom) }
    )

    private fun Book.toEntity(createdAt: Long, updatedAt: Long): BookEntity = BookEntity(
        id = id,
        isbn = isbn?.ifBlank { null },
        title = title,
        author = author,
        publisher = publisher,
        publishedYear = publishedYear,
        description = description,
        coverUrl = coverUrl,
        coverPath = coverPath,
        genre = genre,
        subgenre = subgenre,
        spiceLevel = spiceLevel,
        rating = rating,
        status = status.name,
        notes = notes,
        aiSummary = aiSummary,
        aiFitScore = aiFitScore,
        aiFitReason = aiFitReason,
        aiInsightsAt = aiInsightsAt,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
