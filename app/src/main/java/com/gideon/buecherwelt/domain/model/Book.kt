package com.gideon.buecherwelt.domain.model

import com.gideon.buecherwelt.data.local.ReadingStatus

data class Trope(
    val id: Long,
    val name: String,
    val category: String,
    val isCustom: Boolean
)

data class Book(
    val id: Long = 0,
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
    val status: ReadingStatus = ReadingStatus.PLANNED,
    val notes: String = "",
    val tropes: List<Trope> = emptyList(),
    val createdAt: Long = 0,
    val updatedAt: Long = 0
)

data class WishlistItem(
    val id: Long = 0,
    val isbn: String? = null,
    val title: String,
    val author: String = "",
    val coverUrl: String? = null,
    val coverPath: String? = null,
    val reason: String = "",
    val source: String = "",
    val addedAt: Long = 0
)
