package com.gideon.buecherwelt.di

import android.content.Context
import com.gideon.buecherwelt.BuildConfig
import com.gideon.buecherwelt.data.csv.StoryGraphCsvImporter
import com.gideon.buecherwelt.data.local.AppDatabase
import com.gideon.buecherwelt.data.network.BookMetadataFetcher
import com.gideon.buecherwelt.data.network.CoverCache
import com.gideon.buecherwelt.data.network.GoogleBooksApi
import com.gideon.buecherwelt.data.network.OpenLibraryApi
import com.gideon.buecherwelt.data.settings.SettingsRepository
import com.gideon.buecherwelt.domain.llm.CoverIdentifier
import com.gideon.buecherwelt.domain.llm.LlmClient
import com.gideon.buecherwelt.domain.llm.LlmService
import com.gideon.buecherwelt.domain.recommendation.RecommendationEngine
import com.gideon.buecherwelt.domain.repository.BookRepository
import com.gideon.buecherwelt.domain.repository.TropeRepository
import com.gideon.buecherwelt.domain.repository.WishlistRepository
import com.gideon.buecherwelt.domain.usecase.AddToLibraryUseCase
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import okhttp3.MediaType.Companion.toMediaType

class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    val json: Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
    }

    val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val request = chain.request()
            val apiKey = BuildConfig.GOOGLE_BOOKS_API_KEY
            val builder = request.newBuilder()
                .header("User-Agent", "GideonsBuecherwelt/1.7 (Android)")
            val newRequest = if (apiKey.isNotBlank() && request.url.host == "www.googleapis.com") {
                builder
                    .url(request.url.newBuilder().addQueryParameter("key", apiKey).build())
                    .build()
            } else {
                builder.build()
            }
            chain.proceed(newRequest)
        }
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .writeTimeout(25, TimeUnit.SECONDS)
        .build()

    private inline fun <reified T> retrofitApi(baseUrl: String): T {
        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
        return retrofit.create(T::class.java)
    }

    val googleBooksApi: GoogleBooksApi = retrofitApi("https://www.googleapis.com/")
    val openLibraryApi: OpenLibraryApi = retrofitApi("https://openlibrary.org/")

    val database: AppDatabase = AppDatabase.getInstance(appContext)
    val bookDao = database.bookDao()
    val tropeDao = database.tropeDao()
    val wishlistDao = database.wishlistDao()

    val settingsRepository = SettingsRepository(appContext)
    val coverCache = CoverCache(appContext, okHttpClient)
    val metadataFetcher = BookMetadataFetcher(googleBooksApi, openLibraryApi, okHttpClient)

    val bookRepository = BookRepository(bookDao, tropeDao)
    val tropeRepository = TropeRepository(tropeDao)
    val wishlistRepository = WishlistRepository(wishlistDao, coverCache)

    val llmClient = LlmClient(okHttpClient, json)
    val llmService = LlmService(llmClient, tropeRepository)
    val coverIdentifier = CoverIdentifier(appContext, llmClient, metadataFetcher)
    val recommendationEngine = RecommendationEngine(metadataFetcher)
    val addToLibrary = AddToLibraryUseCase(bookRepository, tropeRepository, metadataFetcher, coverCache)
    val bookEnrichmentService = com.gideon.buecherwelt.domain.usecase.BookEnrichmentService(
        bookRepository = bookRepository,
        tropeRepository = tropeRepository,
        metadataFetcher = metadataFetcher,
        coverCache = coverCache
    )

    val csvImporter = StoryGraphCsvImporter(
        appContext, metadataFetcher, coverCache, bookRepository, tropeRepository
    )
}
