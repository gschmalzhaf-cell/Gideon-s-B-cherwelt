package com.gideon.buecherwelt.ui.screens.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gideon.buecherwelt.domain.analysis.ReadingDnaSummary
import com.gideon.buecherwelt.domain.analysis.ReadingProfile
import com.gideon.buecherwelt.domain.analysis.ReadingStatsCalculator
import com.gideon.buecherwelt.domain.analysis.SeriesProgress
import com.gideon.buecherwelt.domain.analysis.SmartLibraryEngine
import com.gideon.buecherwelt.domain.repository.BookRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class StatsUiState(
    val profile: ReadingProfile = ReadingProfile(),
    val seriesProgress: List<SeriesProgress> = emptyList(),
    val readingDna: ReadingDnaSummary = ReadingDnaSummary()
)

class StatsViewModel(
    bookRepository: BookRepository
) : ViewModel() {

    val uiState: StateFlow<StatsUiState> = bookRepository.observeBooks()
        .map { books ->
            val profile = ReadingStatsCalculator.compute(books)
            val series = SmartLibraryEngine.analyzeSeriesProgress(books)
            val dna = SmartLibraryEngine.computeReadingDna(books, series)
            StatsUiState(
                profile = profile,
                seriesProgress = series,
                readingDna = dna
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StatsUiState())
}
