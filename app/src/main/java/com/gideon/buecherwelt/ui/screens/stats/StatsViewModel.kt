package com.gideon.buecherwelt.ui.screens.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gideon.buecherwelt.domain.analysis.ReadingProfile
import com.gideon.buecherwelt.domain.analysis.ReadingStatsCalculator
import com.gideon.buecherwelt.domain.repository.BookRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class StatsViewModel(
    bookRepository: BookRepository
) : ViewModel() {

    val profile: StateFlow<ReadingProfile> = bookRepository.observeBooks()
        .map { books -> ReadingStatsCalculator.compute(books) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ReadingProfile())
}
