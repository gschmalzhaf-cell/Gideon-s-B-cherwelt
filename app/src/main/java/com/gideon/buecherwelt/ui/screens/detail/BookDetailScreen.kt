package com.gideon.buecherwelt.ui.screens.detail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.gideon.buecherwelt.data.local.ReadingStatus
import com.gideon.buecherwelt.di.AppContainer
import com.gideon.buecherwelt.domain.recommendation.ScoredCandidate
import com.gideon.buecherwelt.ui.components.AppTextField
import com.gideon.buecherwelt.ui.components.BookCover
import com.gideon.buecherwelt.ui.components.InfoRow
import com.gideon.buecherwelt.ui.components.SectionHeader
import com.gideon.buecherwelt.ui.components.SpiceFlames
import com.gideon.buecherwelt.ui.components.StarRating
import com.gideon.buecherwelt.ui.components.StatusChip
import com.gideon.buecherwelt.ui.navigation.Screen
import com.gideon.buecherwelt.ui.theme.PlayfairDisplay
import com.gideon.buecherwelt.ui.viewModelFactory
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BookDetailScreen(
    container: AppContainer,
    navController: NavController,
    bookId: Long
) {
    val viewModel: BookDetailViewModel = viewModel(
        factory = viewModelFactory {
            BookDetailViewModel(
                container.bookRepository,
                container.recommendationEngine,
                container.llmService,
                container.settingsRepository,
                bookId
            )
        }
    )
    val book by viewModel.book.collectAsStateWithLifecycle()
    val localFit by viewModel.localFit.collectAsStateWithLifecycle()
    val similar by viewModel.similar.collectAsStateWithLifecycle()
    val similarLoading by viewModel.similarLoading.collectAsStateWithLifecycle()
    val similarError by viewModel.similarError.collectAsStateWithLifecycle()
    val llmConfigured by viewModel.llmConfigured.collectAsStateWithLifecycle()
    val insightsLoading by viewModel.insightsLoading.collectAsStateWithLifecycle()
    val insightsError by viewModel.insightsError.collectAsStateWithLifecycle()
    val seriesBooks by viewModel.seriesBooks.collectAsStateWithLifecycle()
    val seriesNames by viewModel.seriesNames.collectAsStateWithLifecycle()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showSeriesDialog by remember { mutableStateOf(false) }
    var similarRequested by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(similarRequested) {
        if (similarRequested) viewModel.loadSimilar()
    }

    LaunchedEffect(book?.id, llmConfigured) {
        if (llmConfigured && book?.aiSummary == null) viewModel.loadInsights()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
                    }
                },
                title = {
                    Text(
                        text = book?.title ?: "Buch",
                        fontFamily = PlayfairDisplay,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                actions = {
                    IconButton(onClick = { navController.navigate(Screen.AddBook.create(bookId)) }) {
                        Icon(Icons.Default.Edit, contentDescription = "Bearbeiten")
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Löschen")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        val current = book
        if (current == null) {
            Column(
                Modifier
                    .padding(padding)
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row {
                            BookCover(
                                coverPath = current.coverPath,
                                coverUrl = current.coverUrl,
                                contentDescription = current.title,
                                title = current.title,
                                author = current.author,
                                cornerRadius = 10.dp,
                                modifier = Modifier
                                    .width(115.dp)
                                    .aspectRatio(2f / 3f)
                            )
                            Column(modifier = Modifier.padding(start = 16.dp)) {
                                StatusChip(status = current.status)
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    current.title,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontFamily = PlayfairDisplay,
                                    fontWeight = FontWeight.Bold
                                )
                                if (current.author.isNotBlank()) {
                                    Text(
                                        current.author,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                if (current.seriesName.isNotBlank()) {
                                    val seriesLabel = current.seriesName +
                                        (current.seriesIndex?.let { " · Band $it" } ?: "")
                                    Text(
                                        seriesLabel,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontStyle = FontStyle.Italic,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }
                                Spacer(Modifier.height(8.dp))
                                StarRating(
                                    rating = current.rating ?: 0,
                                    onRatingChange = { viewModel.setRating(it) },
                                    size = 22
                                )
                                Spacer(Modifier.height(8.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Spice:", style = MaterialTheme.typography.bodyMedium)
                                    Spacer(Modifier.width(4.dp))
                                    SpiceFlames(level = current.spiceLevel)
                                    Text(
                                        text = if (current.spiceLevel != null) " ${current.spiceLevel}/5" else " 0/5",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(14.dp))
                        Text(
                            text = "Lesestatus direkt wechseln:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(4.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            ReadingStatus.entries.forEach { st ->
                                val selected = current.status == st
                                FilterChip(
                                    selected = selected,
                                    onClick = { viewModel.setStatus(st) },
                                    label = { Text(st.label, style = MaterialTheme.typography.labelSmall) },
                                    shape = CircleShape,
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                )
                            }
                        }
                    }
                }
            }
            item {
                SectionHeader("Bibliothekarische Daten")
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        InfoRow("ISBN", current.isbn ?: "–")
                        InfoRow("Verlag", current.publisher.ifBlank { "–" })
                        InfoRow("Erscheinungsjahr", current.publishedYear?.toString() ?: "–")
                        InfoRow("Genre", current.genre.ifBlank { "–" })
                        InfoRow("Subgenre", current.subgenre)
                    }
                }
            }
            item {
                val seriesAll = listOf(current) + seriesBooks
                SectionHeader(if (current.seriesName.isNotBlank()) "Buchreihe: ${current.seriesName}" else "Buchreihe & Fortsetzungen")
                if (current.seriesName.isNotBlank()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (current.seriesIndex != null) {
                                InfoRow("Band in der Serie", "Nr. ${current.seriesIndex}")
                            }
                            val seriesGenres = seriesAll.map { it.genre }.filter { it.isNotBlank() }.distinct()
                            if (seriesGenres.isNotEmpty()) {
                                Text("Genres in der Serie", style = MaterialTheme.typography.labelMedium)
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    seriesGenres.forEach { genre ->
                                        FilterChip(
                                            selected = true,
                                            onClick = {},
                                            label = { Text(genre) },
                                            shape = CircleShape,
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        )
                                    }
                                }
                            }
                            val seriesTropes = seriesAll.flatMap { b -> b.tropes.map { t -> t.name } }.distinct()
                            if (seriesTropes.isNotEmpty()) {
                                Text("Tropes in der Serie", style = MaterialTheme.typography.labelMedium)
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    seriesTropes.forEach { trope ->
                                        FilterChip(
                                            selected = true,
                                            onClick = {},
                                            label = { Text(trope) },
                                            shape = CircleShape,
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        )
                                    }
                                }
                            }
                            if (seriesBooks.isNotEmpty()) {
                                Text("Weitere Bände in deinem Regal", style = MaterialTheme.typography.labelMedium)
                                seriesBooks.forEach { other ->
                                    SeriesBookRow(
                                        book = other,
                                        onClick = { navController.navigate(Screen.BookDetail.create(other.id)) }
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = { showSeriesDialog = true }) {
                    Icon(Icons.Default.Edit, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (current.seriesName.isBlank()) "Serie verknüpfen" else "Serie ändern")
                }
            }
            if (current.tropes.isNotEmpty()) {
                item {
                    SectionHeader("Tropes & Motive")
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        current.tropes.forEach { trope ->
                            FilterChip(
                                selected = true,
                                onClick = {},
                                label = { Text(trope.name) },
                                shape = CircleShape,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }
                }
            }
            if (current.description.isNotBlank()) {
                item {
                    SectionHeader("Klappentext")
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Text(
                            text = current.description,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
            if (current.notes.isNotBlank()) {
                item {
                    SectionHeader("Mein Lese-Exlibris & Notizen")
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.45f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f))
                    ) {
                        Row(modifier = Modifier.padding(16.dp)) {
                            Icon(
                                imageVector = Icons.Default.FormatQuote,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = current.notes,
                                style = MaterialTheme.typography.bodyMedium,
                                fontStyle = FontStyle.Italic
                            )
                        }
                    }
                }
            }
            item {
                SectionHeader("Profil-Passgenauigkeit & KI-Einblicke")
                val effectiveScore = current.aiFitScore ?: localFit?.first
                val effectiveReason = current.aiFitReason?.takeIf { it.isNotBlank() } ?: localFit?.second

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        if (effectiveScore != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = if (current.aiFitScore != null) "KI-Profil-Match" else "Intelligenter Profil-Match",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontFamily = PlayfairDisplay,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = CircleShape
                                ) {
                                    Text(
                                        text = "$effectiveScore / 100",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { (effectiveScore.coerceIn(0, 100)) / 100f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(7.dp)
                                    .clip(CircleShape)
                            )
                            if (!effectiveReason.isNullOrBlank()) {
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = effectiveReason,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (current.aiSummary != null) {
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "Spoilerfreie KI-Zusammenfassung",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(current.aiSummary!!, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                if (llmConfigured) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { viewModel.loadInsights(force = true) },
                        enabled = !insightsLoading
                    ) {
                        if (insightsLoading) {
                            CircularProgressIndicator(modifier = Modifier.height(18.dp))
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(if (current.aiSummary != null) "KI-Analyse neu generieren" else "Tiefe KI-Analyse laden")
                    }
                }
                if (insightsError != null) {
                    Text(
                        insightsError!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
            item {
                SectionHeader("Ähnliche Bücher")
                OutlinedButton(
                    onClick = { similarRequested = true },
                    enabled = !similarLoading
                ) {
                    if (similarLoading) {
                        CircularProgressIndicator(modifier = Modifier.height(18.dp))
                    } else {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                    }
                    Spacer(Modifier.width(8.dp))
                    Text("Ähnliche Bücher laden")
                }
                if (similarError != null) {
                    Text(
                        similarError!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                similar.forEach { candidate ->
                    SimilarBookCard(
                        candidate = candidate,
                        onAdd = {
                            scope.launch {
                                val result = container.addToLibrary.addFromCandidate(candidate)
                                snackbarHostState.showSnackbar(
                                    result.fold(
                                        onSuccess = { "Zur Bibliothek hinzugefügt." },
                                        onFailure = { it.message ?: "Fehler" }
                                    )
                                )
                            }
                        }
                    )
                }
            }
        }
    }

    if (showSeriesDialog) {
        var name by remember { mutableStateOf(book?.seriesName ?: "") }
        var indexText by remember { mutableStateOf(book?.seriesIndex?.toString() ?: "") }
        AlertDialog(
            onDismissRequest = { showSeriesDialog = false },
            title = { Text("Serie verknüpfen", fontFamily = PlayfairDisplay, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Serienname") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    val others = seriesNames.filter { it != (book?.seriesName ?: "") }
                    if (others.isNotEmpty()) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            others.forEach { series ->
                                AssistChip(onClick = { name = series }, label = { Text(series) })
                            }
                        }
                    }
                    AppTextField(
                        value = indexText,
                        onValueChange = { indexText = it.filter { c -> c.isDigit() }.take(3) },
                        label = { Text("Band / Nr. in der Serie") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showSeriesDialog = false
                    viewModel.setSeries(name, indexText.trim().toIntOrNull())
                }) { Text("Übernehmen") }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!book?.seriesName.isNullOrBlank()) {
                        TextButton(onClick = {
                            showSeriesDialog = false
                            viewModel.setSeries("", null)
                        }) { Text("Aus Serie entfernen") }
                    }
                    TextButton(onClick = { showSeriesDialog = false }) { Text("Abbrechen") }
                }
            }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Buch löschen?", fontFamily = PlayfairDisplay, fontWeight = FontWeight.Bold) },
            text = { Text("„${book?.title}“ wird dauerhaft aus deiner Bibliothek entfernt.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    viewModel.delete { navController.popBackStack() }
                }) { Text("Löschen") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Abbrechen") }
            }
        )
    }
}

@Composable
private fun SeriesBookRow(
    book: com.gideon.buecherwelt.domain.model.Book,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            BookCover(
                coverPath = book.coverPath,
                coverUrl = book.coverUrl,
                contentDescription = book.title,
                title = book.title,
                author = book.author,
                cornerRadius = 6.dp,
                modifier = Modifier
                    .width(42.dp)
                    .aspectRatio(2f / 3f)
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
            ) {
                Text(
                    book.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = PlayfairDisplay,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                val meta = buildList {
                    if (book.seriesIndex != null) add("Band ${book.seriesIndex}")
                    if (book.publishedYear != null) add(book.publishedYear.toString())
                    if (book.author.isNotBlank()) add(book.author)
                }.joinToString(" · ")
                if (meta.isNotBlank()) {
                    Text(
                        meta,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(4.dp))
                StatusChip(status = book.status)
            }
        }
    }
}

@Composable
private fun SimilarBookCard(
    candidate: ScoredCandidate,
    onAdd: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
            BookCover(
                coverPath = null,
                coverUrl = candidate.metadata.coverUrl,
                contentDescription = candidate.metadata.title,
                title = candidate.metadata.title,
                author = candidate.metadata.author,
                cornerRadius = 6.dp,
                modifier = Modifier
                    .width(48.dp)
                    .aspectRatio(2f / 3f)
            )
            Column(modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp)) {
                Text(
                    candidate.metadata.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = PlayfairDisplay,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (candidate.metadata.author.isNotBlank()) {
                    Text(
                        candidate.metadata.author,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                candidate.reasons.forEach { reason ->
                    Text(
                        "• $reason",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            IconButton(onClick = onAdd) {
                Icon(Icons.Default.Add, contentDescription = "Zur Bibliothek hinzufügen")
            }
        }
    }
}

