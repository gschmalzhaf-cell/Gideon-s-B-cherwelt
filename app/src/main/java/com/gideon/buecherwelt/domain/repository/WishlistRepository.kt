package com.gideon.buecherwelt.domain.repository

import com.gideon.buecherwelt.data.local.WishlistDao
import com.gideon.buecherwelt.data.local.WishlistItemEntity
import com.gideon.buecherwelt.data.network.BookMetadata
import com.gideon.buecherwelt.data.network.CoverCache
import com.gideon.buecherwelt.domain.model.WishlistItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class WishlistRepository(
    private val wishlistDao: WishlistDao,
    private val coverCache: CoverCache
) {

    fun observeAll(): Flow<List<WishlistItem>> =
        wishlistDao.observeAll().map { list -> list.map { it.toDomain() } }

    suspend fun contains(isbn: String?, title: String): Boolean {
        if (!isbn.isNullOrBlank() && wishlistDao.findByIsbn(isbn) != null) return true
        return wishlistDao.findByTitle(title) != null
    }

    suspend fun add(metadata: BookMetadata, reason: String, source: String): WishlistItem {
        val coverPath = metadata.coverUrl?.let {
            coverCache.download(it, CoverCache.keyFor(metadata.isbn, metadata.title, metadata.author))
        }
        val entity = WishlistItemEntity(
            isbn = metadata.isbn?.ifBlank { null },
            title = metadata.title,
            author = metadata.author,
            coverUrl = metadata.coverUrl,
            coverPath = coverPath,
            reason = reason,
            source = source,
            addedAt = System.currentTimeMillis()
        )
        val id = wishlistDao.insert(entity)
        return entity.copy(id = id).toDomain()
    }

    suspend fun remove(item: WishlistItem) {
        wishlistDao.delete(
            WishlistItemEntity(
                id = item.id,
                isbn = item.isbn,
                title = item.title,
                author = item.author,
                coverUrl = item.coverUrl,
                coverPath = item.coverPath,
                reason = item.reason,
                source = item.source,
                addedAt = item.addedAt
            )
        )
    }

    private fun WishlistItemEntity.toDomain(): WishlistItem = WishlistItem(
        id = id,
        isbn = isbn,
        title = title,
        author = author,
        coverUrl = coverUrl,
        coverPath = coverPath,
        reason = reason,
        source = source,
        addedAt = addedAt
    )
}
