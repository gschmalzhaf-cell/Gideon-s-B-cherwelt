package com.gideon.buecherwelt.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "books",
    indices = [Index(value = ["isbn"], unique = true)]
)
data class BookEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val isbn: String? = null,
    val title: String = "",
    val author: String = "",
    val publisher: String = "",
    val publishedYear: Int? = null,
    val description: String = "",
    val coverUrl: String? = null,
    val coverPath: String? = null,
    val genre: String = "",
    val subgenre: String = "",
    val spiceLevel: Int? = null,
    val rating: Int? = null,
    val status: String = ReadingStatus.PLANNED.name,
    val notes: String = "",
    val seriesName: String = "",
    val seriesIndex: Int? = null,
    val universeName: String = "",
    val subtitle: String = "",
    val pageCount: Int? = null,
    val language: String = "",
    val aiSummary: String? = null,
    val aiFitScore: Int? = null,
    val aiFitReason: String? = null,
    val aiInsightsAt: Long = 0L,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)
