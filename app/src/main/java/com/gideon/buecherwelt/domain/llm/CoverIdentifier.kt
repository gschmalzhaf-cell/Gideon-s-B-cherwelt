package com.gideon.buecherwelt.domain.llm

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import com.gideon.buecherwelt.data.network.BookMetadataFetcher
import com.gideon.buecherwelt.data.network.FetchResult
import com.gideon.buecherwelt.data.settings.LlmConfig
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.parseToJsonElement
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.coroutines.resume

data class CoverIdentification(
    val title: String = "",
    val author: String = "",
    val isbn: String? = null,
    val publisher: String = "",
    val year: Int? = null
)

class CoverIdentifier(
    private val context: Context,
    private val llmClient: LlmClient,
    private val fetcher: BookMetadataFetcher
) {

    suspend fun identify(imagePath: String, llmConfig: LlmConfig): FetchResult =
        withContext(Dispatchers.IO) {
            if (llmConfig.isConfigured) {
                val b64 = imageToBase64Jpeg(imagePath, maxSize = 1024)
                if (b64 != null) {
                    val raw = llmClient.completeVision(
                        llmConfig,
                        "Du bist ein Bibliotheks-Assistent. Erkenne das Buch auf dem Cover-Foto. " +
                            "Antworte ausschließlich als JSON-Objekt mit den Feldern " +
                            "\"title\", \"author\", \"isbn\" (falls lesbar, sonst null), \"publisher\", \"year\" (4-stellig, falls erkennbar).",
                        "Welches Buch ist auf diesem Cover abgebildet? Gib title, author, isbn (falls sichtbar), publisher und year als JSON zurück.",
                        b64
                    )
                    val parsed = raw?.let { parseCoverJson(it) }
                    if (parsed != null && parsed.title.isNotBlank()) {
                        val isbn = parsed.isbn?.let { BookMetadataFetcher.normalizeIsbn(it) }
                        if (isbn != null) {
                            val byIsbn = fetcher.fetchByIdentifier(isbn)
                            if (byIsbn is FetchResult.Success) return@withContext byIsbn
                        }
                        return@withContext fetcher.findByTitleAuthor(parsed.title, parsed.author)
                    }
                }
            }
            val lines = ocrLines(imagePath)
            val (title, author) = guessTitleAuthor(lines)
            if (title != null) return@withContext fetcher.findByTitleAuthor(title, author ?: "")
            FetchResult.NotFound
        }

    private fun parseCoverJson(raw: String): CoverIdentification? = try {
        val obj = Json.parseToJsonElement(raw).jsonObject
        CoverIdentification(
            title = obj["title"]?.jsonPrimitive?.content?.trim().orEmpty(),
            author = obj["author"]?.jsonPrimitive?.content?.trim().orEmpty(),
            isbn = obj["isbn"]?.jsonPrimitive?.takeIf { it.isString && it.content.lowercase() != "null" }
                ?.content?.ifBlank { null },
            publisher = obj["publisher"]?.jsonPrimitive?.content?.trim().orEmpty(),
            year = obj["year"]?.jsonPrimitive?.content?.trim()?.toIntOrNull()
        )
    } catch (e: Exception) {
        null
    }

    private suspend fun ocrLines(imagePath: String): List<String> = withContext(Dispatchers.IO) {
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        try {
            val image = InputImage.fromFilePath(context, Uri.fromFile(File(imagePath)))
            val result = suspendCancellableCoroutine<Result<com.google.mlkit.vision.text.Text>> { cont ->
                recognizer.process(image)
                    .addOnSuccessListener { cont.resume(Result.success(it)) }
                    .addOnFailureListener { cont.resume(Result.failure(it)) }
            }
            result.getOrNull()
                ?.textBlocks.orEmpty()
                .flatMap { block -> block.lines.map { it.text.trim() } }
                .filter { it.isNotEmpty() }
                ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        } finally {
            recognizer.close()
        }
    }

    private fun guessTitleAuthor(lines: List<String>): Pair<String?, String?> {
        val candidates = lines
            .filter { line ->
                line.any(Char::isLetter) &&
                    line.length in 4..80 &&
                    !line.all { it.isDigit() || it.isWhitespace() || it in "-:;,." }
            }
            .distinct()
            .take(8)
        val title = candidates.firstOrNull()
        val author = candidates.drop(1).firstOrNull { it != title }
        return title to author
    }

    private fun imageToBase64Jpeg(path: String, maxSize: Int): String? = try {
        val src = BitmapFactory.decodeFile(path) ?: return null
        val scale = maxSize.toFloat() / maxOf(src.width, src.height).coerceAtLeast(1)
        val bmp = if (scale < 1f) {
            Bitmap.createScaledBitmap(
                src,
                (src.width * scale).toInt().coerceAtLeast(1),
                (src.height * scale).toInt().coerceAtLeast(1),
                true
            )
        } else src
        val out = ByteArrayOutputStream()
        bmp.compress(Bitmap.CompressFormat.JPEG, 80, out)
        Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
    } catch (e: Exception) {
        null
    }
}
