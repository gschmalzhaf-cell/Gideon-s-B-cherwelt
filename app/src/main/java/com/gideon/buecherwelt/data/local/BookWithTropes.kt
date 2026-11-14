package com.gideon.buecherwelt.data.local

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

data class BookWithTropes(
    @Embedded val book: BookEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = BookTropeEntity::class,
            parentColumn = "bookId",
            entityColumn = "tropeId"
        )
    )
    val tropes: List<TropeEntity> = emptyList()
)
