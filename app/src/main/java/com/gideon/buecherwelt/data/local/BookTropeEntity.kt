package com.gideon.buecherwelt.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "book_tropes",
    primaryKeys = ["bookId", "tropeId"],
    indices = [Index(value = ["tropeId"])],
    foreignKeys = [
        ForeignKey(
            entity = BookEntity::class,
            parentColumns = ["id"],
            childColumns = ["bookId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = TropeEntity::class,
            parentColumns = ["id"],
            childColumns = ["tropeId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class BookTropeEntity(
    val bookId: Long,
    val tropeId: Long
)
