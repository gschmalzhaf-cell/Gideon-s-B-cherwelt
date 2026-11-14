package com.gideon.buecherwelt.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TropeDao {

    @Query("SELECT * FROM tropes ORDER BY category COLLATE NOCASE ASC, name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<TropeEntity>>

    @Query("SELECT * FROM tropes")
    suspend fun getAll(): List<TropeEntity>

    @Query("SELECT * FROM tropes WHERE name LIKE '%' || :query || '%' ORDER BY name COLLATE NOCASE ASC LIMIT 30")
    suspend fun search(query: String): List<TropeEntity>

    @Query("SELECT * FROM tropes WHERE lower(name) = lower(:name) LIMIT 1")
    suspend fun findByName(name: String): TropeEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(trope: TropeEntity): Long

    @Delete
    suspend fun delete(trope: TropeEntity)
}
