package com.gideon.buecherwelt.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WishlistDao {

    @Query("SELECT * FROM wishlist ORDER BY addedAt DESC")
    fun observeAll(): Flow<List<WishlistItemEntity>>

    @Query("SELECT * FROM wishlist WHERE isbn = :isbn LIMIT 1")
    suspend fun findByIsbn(isbn: String): WishlistItemEntity?

    @Query("SELECT * FROM wishlist WHERE lower(title) = lower(:title) LIMIT 1")
    suspend fun findByTitle(title: String): WishlistItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: WishlistItemEntity): Long

    @Delete
    suspend fun delete(item: WishlistItemEntity)
}
