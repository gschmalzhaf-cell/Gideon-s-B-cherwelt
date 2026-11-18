package com.gideon.buecherwelt.data.network

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

class CoverCache(
    context: Context,
    private val client: OkHttpClient
) {
    private val dir: File = File(context.filesDir, "covers").apply { mkdirs() }

    suspend fun download(
        url: String,
        key: String,
        forceRefresh: Boolean = false
    ): String? = withContext(Dispatchers.IO) {
        try {
            val cleanUrl = url.trim().replace("http://", "https://")
            if (cleanUrl.isBlank()) return@withContext null
            val file = File(dir, "${key.replace(Regex("[^A-Za-z0-9_-]"), "_")}.jpg")
            if (!forceRefresh && isValidCoverFile(file)) return@withContext file.absolutePath
            val request = Request.Builder()
                .url(cleanUrl)
                .header("User-Agent", "GideonsBuecherwelt/1.5 (Android)")
                .build()
            val response = client.newCall(request).execute()
            response.use { resp ->
                if (!resp.isSuccessful) return@withContext null
                val bytes = resp.body?.bytes() ?: return@withContext null
                // Validate that we received a real image (> 1 KB, not a 1x1 placeholder GIF or HTML error)
                if (!isValidImageBytes(bytes)) return@withContext null
                file.writeBytes(bytes)
            }
            if (isValidCoverFile(file)) file.absolutePath else null
        } catch (e: Exception) {
            null
        }
    }

    suspend fun downloadWithFallback(
        primaryUrl: String?,
        isbn: String?,
        key: String,
        forceRefresh: Boolean = false
    ): String? {
        if (!primaryUrl.isNullOrBlank()) {
            val path = download(primaryUrl, key, forceRefresh)
            if (path != null) return path
        }
        val cleanIsbn = isbn?.let { BookMetadataFetcher.normalizeIsbn(it) }
        if (!cleanIsbn.isNullOrBlank()) {
            for (v in BookMetadataFetcher.isbnVariants(cleanIsbn)) {
                val olIsbnUrl = "https://covers.openlibrary.org/b/isbn/$v-L.jpg?default=false"
                val path = download(olIsbnUrl, key, forceRefresh)
                if (path != null) return path
            }
        }
        return null
    }

    suspend fun savePicked(contentResolver: ContentResolver, uri: Uri, key: String): String? =
        withContext(Dispatchers.IO) {
            try {
                val file = File(dir, "${key.replace(Regex("[^A-Za-z0-9_-]"), "_")}.jpg")
                contentResolver.openInputStream(uri)?.use { input ->
                    file.outputStream().use { out -> input.copyTo(out) }
                }
                if (isValidCoverFile(file)) file.absolutePath else null
            } catch (e: Exception) {
                null
            }
        }

    companion object {
        fun keyFor(isbn: String?, title: String, author: String): String =
            (isbn?.takeIf { it.isNotBlank() } ?: "${title.trim().lowercase()}|${author.trim().lowercase()}")
                .hashCode()
                .toString()

        fun isValidCoverFile(path: String?): Boolean {
            if (path.isNullOrBlank()) return false
            return isValidCoverFile(File(path))
        }

        fun isValidCoverFile(file: File): Boolean =
            file.exists() && file.isFile && file.length() > 1000L

        private fun isValidImageBytes(bytes: ByteArray): Boolean {
            if (bytes.size < 1000) return false
            // JPEG: FF D8
            val isJpeg = bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte()
            // PNG: 89 50 4E 47
            val isPng = bytes[0] == 0x89.toByte() && bytes[1] == 0x50.toByte() &&
                bytes[2] == 0x4E.toByte() && bytes[3] == 0x47.toByte()
            // WebP: RIFF....WEBP
            val isWebp = bytes.size > 12 &&
                bytes[0] == 'R'.code.toByte() && bytes[1] == 'I'.code.toByte() &&
                bytes[2] == 'F'.code.toByte() && bytes[3] == 'F'.code.toByte()
            // GIF: GIF8 (only if > 1000 bytes, avoiding 43-byte 1x1 pixel OpenLibrary blank GIF)
            val isGif = bytes[0] == 'G'.code.toByte() && bytes[1] == 'I'.code.toByte() &&
                bytes[2] == 'F'.code.toByte()
            return isJpeg || isPng || isWebp || isGif
        }
    }
}
