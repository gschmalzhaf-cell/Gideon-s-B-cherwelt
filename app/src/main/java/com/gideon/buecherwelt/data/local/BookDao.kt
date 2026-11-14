package com.gideon.buecherwelt.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {

    @Transaction
    @Query("SELECT * FROM books ORDER BY title COLLATE NOCASE ASC")
    fun observeBooks(): Flow<List<BookWithTropes>>

    @Transaction
    @Query("SELECT * FROM books WHERE id = :id")
    fun observeBook(id: Long): Flow<BookWithTropes?>

    @Transaction
    @Query("SELECT * FROM books WHERE id = :id")
    suspend fun getBook(id: Long): BookWithTropes?

    @Transaction
    @Query("SELECT * FROM books")
    suspend fun getAllBooks(): List<BookWithTropes>

    @Query("SELECT * FROM books WHERE isbn = :isbn LIMIT 1")
    suspend fun findByIsbn(isbn: String): BookEntity?

    @Query(
        "SELECT * FROM books WHERE lower(trim(title)) = lower(trim(:title)) " +
            "AND lower(trim(author)) = lower(trim(:author)) LIMIT 1"
    )
    suspend fun findByTitleAuthor(title: String, author: String): BookEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(book: BookEntity): Long

    @Update
    suspend fun update(book: BookEntity)

    @Delete
    suspend fun delete(book: BookEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addTrope(bookTrope: BookTropeEntity)

    @Query("DELETE FROM book_tropes WHERE bookId = :bookId")
    suspend fun clearTropes(bookId: Long)
}
