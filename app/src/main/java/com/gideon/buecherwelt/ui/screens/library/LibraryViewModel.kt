package com.gideon.buecherwelt.ui.screens.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gideon.buecherwelt.data.local.ReadingStatus
import com.gideon.buecherwelt.domain.model.Book
import com.gideon.buecherwelt.domain.repository.BookRepository
import com.gideon.buecherwelt.domain.repository.TropeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LibraryUiState(
    val books: List<Book> = emptyList(),
    val query: String = "",
    val genre: String? = null,
    val status: ReadingStatus? = null,
    val minRating: Int = 0,
    val trope: String? = null,
    val genres: List<String> = emptyList(),
    val tropes: List<String> = emptyList()
) {
    val filtered: List<Book>
        get() = books.filter { book ->
            (query.isBlank() ||
                book.title.contains(query, ignoreCase = true) ||
                book.author.contains(query, ignoreCase = true) ||
                isbnComparable(book.isbn ?: "").contains(isbnComparable(query))) &&
                (genre == null || book.genre == genre) &&
                (status == null || book.status == status) &&
                (minRating == 0 || (book.rating ?: 0) >= minRating) &&
                (trope == null || book.tropes.any { it.name == trope })
        }
}

class LibraryViewModel(
    private val bookRepository: BookRepository,
    tropeRepository: TropeRepository
) : ViewModel() {

    private val _state = MutableStateFlow(LibraryUiState())
    val state: StateFlow<LibraryUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            bookRepository.observeBooks().collect { books ->
                _state.update {
                    it.copy(
                        books = books,
                        genres = books.map { b -> b.genre }
                            .filter { g -> g.isNotBlank() }
                            .distinct()
                            .sorted()
                    )
                }
            }
        }
        viewModelScope.launch {
            tropeRepository.observeAll().collect { all ->
                _state.update {
                    val fromBooks = it.books.flatMap { b -> b.tropes.map { t -> t.name } }
                    it.copy(tropes = (fromBooks + all.map { t -> t.name }).distinct().sorted())
                }
            }
        }
    }

    fun setQuery(query: String) = _state.update { it.copy(query = query) }
    fun setGenre(genre: String?) = _state.update { it.copy(genre = genre) }
    fun setStatus(status: ReadingStatus?) = _state.update { it.copy(status = status) }
    fun setMinRating(rating: Int) = _state.update { it.copy(minRating = rating) }
    fun setTrope(trope: String?) = _state.update { it.copy(trope = trope) }
}

private fun isbnComparable(s: String): String =
    s.uppercase().filter { it != '-' && !it.isWhitespace() }
