package com.gideon.buecherwelt.domain.llm

import com.gideon.buecherwelt.data.settings.LlmConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class LlmClient(
    private val client: OkHttpClient,
    private val json: Json
) {

    suspend fun complete(config: LlmConfig, systemPrompt: String, userPrompt: String): String? =
        withContext(Dispatchers.IO) {
            if (!config.isConfigured) return@withContext null
            post(config, buildJsonObject {
                put("model", config.model)
                put("temperature", 0.3)
                put("response_format", buildJsonObject { put("type", "json_object") })
                put("messages", buildJsonArray {
                    add(buildJsonObject {
                        put("role", "system")
                        put("content", systemPrompt)
                    })
                    add(buildJsonObject {
                        put("role", "user")
                        put("content", userPrompt)
                    })
                })
            })
        }

    suspend fun completeVision(
        config: LlmConfig,
        systemPrompt: String,
        userPrompt: String,
        imageBase64: String,
        mimeType: String = "image/jpeg"
    ): String? = withContext(Dispatchers.IO) {
        if (!config.isConfigured) return@withContext null
        post(config, buildJsonObject {
            put("model", config.model)
            put("temperature", 0.2)
            put("response_format", buildJsonObject { put("type", "json_object") })
            put("messages", buildJsonArray {
                add(buildJsonObject {
                    put("role", "system")
                    put("content", systemPrompt)
                })
                add(buildJsonObject {
                    put("role", "user")
                    put("content", buildJsonArray {
                        add(buildJsonObject {
                            put("type", "text")
                            put("text", userPrompt)
                        })
                        add(buildJsonObject {
                            put("type", "image_url")
                            put("image_url", buildJsonObject {
                                put("url", "data:$mimeType;base64,$imageBase64")
                            })
                        })
                    })
                })
            })
        })
    }

    private suspend fun post(config: LlmConfig, payload: JsonObject): String? = try {
        val body = payload.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(config.baseUrl.trimEnd('/') + "/chat/completions")
            .header("Authorization", "Bearer ${config.apiKey}")
            .header("Content-Type", "application/json")
            .post(body)
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val text = response.body?.string() ?: return null
            val root = json.parseToJsonElement(text).jsonObject
            root["choices"]?.jsonArray?.firstOrNull()?.jsonObject
                ?.get("message")?.jsonObject
                ?.get("content")?.jsonPrimitive?.takeIf { it.isString }?.content
        }
    } catch (e: Exception) {
        null
    }
}
