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

    suspend fun download(url: String, key: String): String? = withContext(Dispatchers.IO) {
        try {
            val file = File(dir, "${key.replace(Regex("[^A-Za-z0-9_-]"), "_")}.jpg")
            if (file.exists() && file.length() > 0L) return@withContext file.absolutePath
            val response = client.newCall(Request.Builder().url(url).build()).execute()
            response.use { resp ->
                if (!resp.isSuccessful) return@withContext null
                val bytes = resp.body?.bytes() ?: return@withContext null
                file.writeBytes(bytes)
            }
            file.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    suspend fun savePicked(contentResolver: ContentResolver, uri: Uri, key: String): String? =
        withContext(Dispatchers.IO) {
            try {
                val file = File(dir, "${key.replace(Regex("[^A-Za-z0-9_-]"), "_")}.jpg")
                contentResolver.openInputStream(uri)?.use { input ->
                    file.outputStream().use { out -> input.copyTo(out) }
                }
                if (file.exists() && file.length() > 0L) file.absolutePath else null
            } catch (e: Exception) {
                null
            }
        }

    companion object {
        fun keyFor(isbn: String?, title: String, author: String): String =
            (isbn ?: "${title.lowercase()}|${author.lowercase()}").hashCode().toString()
    }
}
