package com.gideon.buecherwelt.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "wishlist",
    indices = [Index(value = ["isbn"])]
)
data class WishlistItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val isbn: String? = null,
    val title: String,
    val author: String = "",
    val coverUrl: String? = null,
    val coverPath: String? = null,
    val reason: String = "",
    val source: String = "",
    val addedAt: Long = 0L
)
