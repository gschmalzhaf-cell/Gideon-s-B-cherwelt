package com.gideon.buecherwelt.ui.screens.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gideon.buecherwelt.domain.model.Book
import com.gideon.buecherwelt.domain.recommendation.RecommendationEngine
import com.gideon.buecherwelt.domain.recommendation.ScoredCandidate
import com.gideon.buecherwelt.domain.repository.BookRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BookDetailViewModel(
    private val bookRepository: BookRepository,
    private val recommendationEngine: RecommendationEngine,
    private val bookId: Long
) : ViewModel() {

    val book: StateFlow<Book?> = bookRepository.observeBook(bookId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _similar = MutableStateFlow<List<ScoredCandidate>>(emptyList())
    val similar: StateFlow<List<ScoredCandidate>> = _similar.asStateFlow()

    private val _similarLoading = MutableStateFlow(false)
    val similarLoading: StateFlow<Boolean> = _similarLoading.asStateFlow()

    private val _similarError = MutableStateFlow<String?>(null)
    val similarError: StateFlow<String?> = _similarError.asStateFlow()

    fun loadSimilar() {
        viewModelScope.launch {
            val current = book.value ?: return@launch
            _similarLoading.value = true
            _similarError.value = null
            try {
                val exclude = bookRepository.getAllBooks()
                    .mapNotNull { b -> RecommendationEngine.keyOf(
                        com.gideon.buecherwelt.data.network.BookMetadata(
                            isbn = b.isbn, title = b.title, author = b.author
                        )
                    ) }
                    .toSet() + RecommendationEngine.keyOf(
                        com.gideon.buecherwelt.data.network.BookMetadata(
                            isbn = current.isbn, title = current.title, author = current.author
                        )
                    )
                _similar.value = recommendationEngine.similarTo(current, exclude)
            } catch (e: Exception) {
                _similarError.value = "Ähnliche Bücher konnten nicht geladen werden (offline?)."
            } finally {
                _similarLoading.value = false
            }
        }
    }

    fun delete(onDone: () -> Unit) {
        viewModelScope.launch {
            book.value?.let { bookRepository.delete(it) }
            onDone()
        }
    }
}
