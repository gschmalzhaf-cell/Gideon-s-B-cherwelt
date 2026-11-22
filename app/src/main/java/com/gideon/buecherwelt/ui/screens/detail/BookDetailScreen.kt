package com.gideon.buecherwelt.ui.screens.detail

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.gideon.buecherwelt.data.local.ReadingStatus
import com.gideon.buecherwelt.data.local.TropesCatalog
import com.gideon.buecherwelt.di.AppContainer
import com.gideon.buecherwelt.domain.analysis.SeriesUniverseDetector
import com.gideon.buecherwelt.domain.model.Book
import com.gideon.buecherwelt.domain.recommendation.ScoredCandidate
import com.gideon.buecherwelt.ui.components.AppTextField
import com.gideon.buecherwelt.ui.components.BookCover
import com.gideon.buecherwelt.ui.components.InfoRow
import com.gideon.buecherwelt.ui.components.SectionHeader
import com.gideon.buecherwelt.ui.components.SpiceFlames
import com.gideon.buecherwelt.ui.components.StarRating
import com.gideon.buecherwelt.ui.components.StatusChip
import com.gideon.buecherwelt.ui.components.TropePicker
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
                bookRepository = container.bookRepository,
                tropeRepository = container.tropeRepository,
                metadataFetcher = container.metadataFetcher,
                coverCache = container.coverCache,
                enrichmentService = container.bookEnrichmentService,
                recommendationEngine = container.recommendationEngine,
                llmService = container.llmService,
                settingsRepository = container.settingsRepository,
                bookId = bookId
            )
        }
    )
    val book by viewModel.book.collectAsStateWithLifecycle()
    val allTropes by viewModel.allTropes.collectAsStateWithLifecycle()
    val localFit by viewModel.localFit.collectAsStateWithLifecycle()
    val localSimilarBooks by viewModel.localSimilarBooks.collectAsStateWithLifecycle()
    val characteristics by viewModel.characteristics.collectAsStateWithLifecycle()
    val similar by viewModel.similar.collectAsStateWithLifecycle()
    val similarLoading by viewModel.similarLoading.collectAsStateWithLifecycle()
    val similarError by viewModel.similarError.collectAsStateWithLifecycle()
    val llmConfigured by viewModel.llmConfigured.collectAsStateWithLifecycle()
    val insightsLoading by viewModel.insightsLoading.collectAsStateWithLifecycle()
    val insightsError by viewModel.insightsError.collectAsStateWithLifecycle()
    val seriesBooks by viewModel.seriesBooks.collectAsStateWithLifecycle()
    val universeBooks by viewModel.universeBooks.collectAsStateWithLifecycle()
    val seriesNames by viewModel.seriesNames.collectAsStateWithLifecycle()
    val universeNames by viewModel.universeNames.collectAsStateWithLifecycle()
    val refreshing by viewModel.refreshing.collectAsStateWithLifecycle()
    val coverCandidates by viewModel.coverCandidates.collectAsStateWithLifecycle()
    val coverCandidatesLoading by viewModel.coverCandidatesLoading.collectAsStateWithLifecycle()

    var showDeleteDialog by remember { mutableStateOf(false) }
    var showSeriesUniverseDialog by remember { mutableStateOf(false) }
    var showTropeDialog by remember { mutableStateOf(false) }
    var showCoverPickerDialog by remember { mutableStateOf(false) }
    var showSpiceSelector by remember { mutableStateOf(false) }
    var similarRequested by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val galleryCoverLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            viewModel.selectLocalCover(context.contentResolver, it) { msg ->
                scope.launch { snackbarHostState.showSnackbar(msg) }
            }
        }
    }

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
                    IconButton(
                        onClick = {
                            viewModel.refreshCoverAndDetails { msg ->
                                scope.launch { snackbarHostState.showSnackbar(msg) }
                            }
                        },
                        enabled = !refreshing
                    ) {
                        if (refreshing) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = "Cover & Details aktualisieren")
                        }
                    }
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
        val chars = characteristics

        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // 1. HEADER CARD (Cover, Titel, Untertitel, Reihe & Band, Universum, Rating, Spice, Status)
            item {
                Spacer(Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.Top) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                BookCover(
                                    coverPath = current.coverPath,
                                    coverUrl = current.coverUrl,
                                    contentDescription = current.title,
                                    title = current.title,
                                    author = current.author,
                                    cornerRadius = 10.dp,
                                    modifier = Modifier
                                        .width(118.dp)
                                        .aspectRatio(2f / 3f)
                                        .clickable {
                                            showCoverPickerDialog = true
                                            viewModel.loadCoverCandidates()
                                        }
                                )
                                Spacer(Modifier.height(6.dp))
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    modifier = Modifier.clickable {
                                        showCoverPickerDialog = true
                                        viewModel.loadCoverCandidates()
                                    }
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Image,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            text = "Cover wählen",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }
                                }
                            }

                            Column(modifier = Modifier.padding(start = 16.dp)) {
                                StatusChip(status = current.status)
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    current.title,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontFamily = PlayfairDisplay,
                                    fontWeight = FontWeight.Bold
                                )
                                if (current.subtitle.isNotBlank()) {
                                    Text(
                                        current.subtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontStyle = FontStyle.Italic,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }
                                if (current.author.isNotBlank()) {
                                    Text(
                                        current.author,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }

                                // Reihe & Band Badge
                                if (current.seriesName.isNotBlank()) {
                                    Spacer(Modifier.height(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.75f)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.AutoStories,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(Modifier.width(5.dp))
                                            val seriesLabel = buildString {
                                                append("Reihe: ${current.seriesName}")
                                                if (current.seriesIndex != null) append(" · Band ${current.seriesIndex}")
                                            }
                                            Text(
                                                text = seriesLabel,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }

                                // Universum Badge (separat!)
                                if (current.universeName.isNotBlank()) {
                                    Spacer(Modifier.height(4.dp))
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.85f)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Public,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(Modifier.width(5.dp))
                                            Text(
                                                text = "Universum: ${current.universeName}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }

                                Spacer(Modifier.height(8.dp))
                                StarRating(
                                    rating = current.rating ?: 0,
                                    onRatingChange = { viewModel.setRating(it) },
                                    size = 22
                                )
                                Spacer(Modifier.height(6.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable { showSpiceSelector = !showSpiceSelector }
                                ) {
                                    Text("Spice:", style = MaterialTheme.typography.bodySmall)
                                    Spacer(Modifier.width(4.dp))
                                    SpiceFlames(level = current.spiceLevel)
                                    Text(
                                        text = " ${current.spiceLevel ?: 0}/5",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                if (chars != null && chars.spiceLabel.isNotBlank()) {
                                    Text(
                                        text = chars.spiceLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        if (showSpiceSelector) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "Spice-Level direkt anpassen:",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                (0..5).forEach { lvl ->
                                    FilterChip(
                                        selected = (current.spiceLevel ?: 0) == lvl,
                                        onClick = {
                                            viewModel.setSpice(lvl)
                                            showSpiceSelector = false
                                        },
                                        label = { Text("$lvl/5") }
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(12.dp))
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

                        Spacer(Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = {
                                viewModel.refreshCoverAndDetails { msg ->
                                    scope.launch { snackbarHostState.showSnackbar(msg) }
                                }
                            },
                            enabled = !refreshing,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (refreshing) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            }
                            Spacer(Modifier.width(8.dp))
                            Text(if (refreshing) "Cover & Buchdetails werden geladen…" else "Cover & Buchdetails online aktualisieren")
                        }
                    }
                }
            }

            // 2. TROPES, MOTIVE & BUCH-CHARAKTERISTIK (Immer sichtbar, detailliert & interaktiv)
            item {
                SectionHeader("Tropes, Motive & Buch-Charakteristik")
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // A) Stimmung, Erzähltempo & Leseumfang
                        if (chars != null) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "Stimmung & Lesegefühl",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    chars.moods.forEach { mood ->
                                        Surface(
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.secondaryContainer
                                        ) {
                                            Text(
                                                text = "✨ $mood",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                            )
                                        }
                                    }
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.tertiaryContainer
                                    ) {
                                        Text(
                                            text = "⏱ ${chars.pacing}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                        )
                                    }
                                    chars.settingHints.forEach { setting ->
                                        Surface(
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.surfaceVariant
                                        ) {
                                            Text(
                                                text = "🏛 $setting",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        // B) Zugeordnete Tropes nach Kategorie gruppiert
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Zugeordnete Tropes (${current.tropes.size})",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                            TextButton(onClick = { showTropeDialog = true }) {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Tropes bearbeiten")
                            }
                        }

                        if (current.tropes.isEmpty()) {
                            Text(
                                text = "Noch keine Tropes hinterlegt. Übernimm unten die automatisch erkannten Vorschläge oder wähle eigene Tropes aus.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            val grouped = current.tropes.groupBy { it.category.ifBlank { "Allgemein" } }
                            grouped.forEach { (category, tropesInCat) ->
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = category,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        tropesInCat.forEach { trope ->
                                            FilterChip(
                                                selected = true,
                                                onClick = { showTropeDialog = true },
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
                        }

                        // C) Automatisch erkannte weitere Trope-Vorschläge zum 1-Klick-Übernehmen
                        val suggestedTropes = chars?.suggestedTropes.orEmpty()
                        if (suggestedTropes.isNotEmpty()) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Erkannte Tropes aus Buchdaten (+ antippen):",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                TextButton(onClick = { viewModel.applyAllSuggestedTropes(suggestedTropes) }) {
                                    Text("Alle übernehmen", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                suggestedTropes.forEach { suggestion ->
                                    AssistChip(
                                        onClick = { viewModel.addSuggestedTrope(suggestion) },
                                        leadingIcon = {
                                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                        },
                                        label = { Text(suggestion, style = MaterialTheme.typography.labelSmall) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. BUCHREIHE & UNIVERSUM (Beides separat erfasst & automatisch erkannt!)
            item {
                SectionHeader("Buchreihe, Band & Buch-Universum")
                val detected = chars?.detectedSeriesUniverse
                val canSuggestSeriesOrUni = detected != null && detected.hasAny && (
                    (current.seriesName.isBlank() && detected.seriesName.isNotBlank()) ||
                        (current.seriesIndex == null && detected.seriesIndex != null) ||
                        (current.universeName.isBlank() && detected.universeName.isNotBlank())
                    )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Automatischer Erkennungs-Banner falls noch nicht übernommen
                        if (canSuggestSeriesOrUni && detected != null) {
                            Surface(
                                shape = MaterialTheme.shapes.medium,
                                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.65f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.AutoAwesome,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            text = "Automatisch erkannt:",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    val detectedDesc = buildList {
                                        if (detected.seriesName.isNotBlank()) {
                                            add("Reihe: „${detected.seriesName}“" + (detected.seriesIndex?.let { " (Band $it)" } ?: ""))
                                        } else if (detected.seriesIndex != null) {
                                            add("Band ${detected.seriesIndex}")
                                        }
                                        if (detected.universeName.isNotBlank()) {
                                            add("Universum: „${detected.universeName}“")
                                        }
                                    }.joinToString(" · ")
                                    Text(
                                        text = detectedDesc,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Spacer(Modifier.height(6.dp))
                                    Button(
                                        onClick = { viewModel.applyDetectedSeriesUniverse() }
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Erkannte Reihe & Universum übernehmen")
                                    }
                                }
                            }
                        }

                        // A) Buchreihe & Band
                        InfoRow("Buchreihe (Serie)", current.seriesName.ifBlank { "Einzelband / Keine Reihe" })
                        InfoRow(
                            "Band in der Reihe",
                            current.seriesIndex?.let { "Band $it" } ?: "–"
                        )
                        // B) Übergeordnetes Buch-Universum (separat!)
                        InfoRow("Buch-Universum / Welt", current.universeName.ifBlank { "–" })

                        // Weitere Bände derselben Reihe im Regal
                        if (current.seriesName.isNotBlank() && seriesBooks.isNotEmpty()) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            Text(
                                text = "Weitere Bände der Reihe „${current.seriesName}“ im Regal (${seriesBooks.size})",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                            seriesBooks.forEach { other ->
                                SeriesBookRow(
                                    book = other,
                                    onClick = { navController.navigate(Screen.BookDetail.create(other.id)) }
                                )
                            }
                        }

                        // Weitere Bücher im selben Universum (auch aus anderen Reihen desselben Universums!)
                        val extraUniverseBooks = universeBooks.filter { uBook ->
                            seriesBooks.none { it.id == uBook.id }
                        }
                        if (current.universeName.isNotBlank() && (extraUniverseBooks.isNotEmpty() || universeBooks.isNotEmpty())) {
                            val listToShow = if (extraUniverseBooks.isNotEmpty()) extraUniverseBooks else universeBooks
                            val allSeriesInUni = (listOf(current) + universeBooks)
                                .map { it.seriesName }
                                .filter { it.isNotBlank() }
                                .distinct()
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            Text(
                                text = "Universum „${current.universeName}“",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (allSeriesInUni.size > 1) {
                                Text(
                                    text = "Verbundene Reihen in diesem Universum: ${allSeriesInUni.joinToString(" · ")}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (extraUniverseBooks.isNotEmpty()) {
                                Text(
                                    text = "Weitere Werke aus dem „${current.universeName}“ in deinem Regal:",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                listToShow.forEach { other ->
                                    SeriesBookRow(
                                        book = other,
                                        showSeriesName = true,
                                        onClick = { navController.navigate(Screen.BookDetail.create(other.id)) }
                                    )
                                }
                            }
                        }

                        OutlinedButton(
                            onClick = { showSeriesUniverseDialog = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Reihe, Band & Universum bearbeiten")
                        }
                    }
                }
            }

            // 4. BIBLIOTHEKARISCHE DATEN & AUSGABEN-DETAILS
            item {
                SectionHeader("Bibliothekarische Daten & Ausgaben-Details")
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        if (current.subtitle.isNotBlank()) {
                            InfoRow("Untertitel", current.subtitle)
                        }
                        InfoRow("Autor:in", current.author.ifBlank { "–" })
                        InfoRow("Genre", current.genre.ifBlank { "–" })
                        InfoRow("Subgenre", current.subgenre.ifBlank { "–" })
                        val pagesText = if (current.pageCount != null && current.pageCount > 0) {
                            val hrs = chars?.estimatedHours
                            if (hrs != null) "${current.pageCount} Seiten (ca. $hrs Std. Lesezeit)"
                            else "${current.pageCount} Seiten"
                        } else "–"
                        InfoRow("Umfang & Lesezeit", pagesText)
                        InfoRow("Verlag", current.publisher.ifBlank { "–" })
                        InfoRow("Erscheinungsjahr", current.publishedYear?.toString() ?: "–")
                        InfoRow("Sprache", formatLanguage(current.language))
                        InfoRow("ISBN / Kennung", current.isbn ?: "–")
                        InfoRow("Spice-Einstufung", "${current.spiceLevel ?: 0}/5 · ${chars?.spiceLabel.orEmpty()}")
                    }
                }
            }

            // 5. KLAPPENTEXT
            item {
                SectionHeader("Klappentext & Inhalt")
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    if (current.description.isNotBlank()) {
                        Text(
                            text = current.description,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(16.dp)
                        )
                    } else {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Noch kein Klappentext hinterlegt.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = {
                                    viewModel.refreshCoverAndDetails { msg ->
                                        scope.launch { snackbarHostState.showSnackbar(msg) }
                                    }
                                },
                                enabled = !refreshing
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Klappentext & Details online nachladen")
                            }
                        }
                    }
                }
            }

            // 6. NOTIZEN / EXLIBRIS
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

            // 7. PROFIL-PASSGENAUIGKEIT & KI-EINBLICKE
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

            // 8. ÄHNLICHE BÜCHER (Offline im eigenen Regal + Online entdecken)
            item {
                if (localSimilarBooks.isNotEmpty()) {
                    SectionHeader("Ähnliche Bücher in deinem Regal")
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        items(localSimilarBooks, key = { "local_sim_${it.book.id}" }) { sim ->
                            Card(
                                modifier = Modifier
                                    .width(210.dp)
                                    .clickable { navController.navigate(Screen.BookDetail.create(sim.book.id)) },
                                shape = MaterialTheme.shapes.large,
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                                border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    BookCover(
                                        coverPath = sim.book.coverPath,
                                        coverUrl = sim.book.coverUrl,
                                        contentDescription = sim.book.title,
                                        title = sim.book.title,
                                        author = sim.book.author,
                                        cornerRadius = 6.dp,
                                        modifier = Modifier
                                            .width(44.dp)
                                            .aspectRatio(2f / 3f)
                                    )
                                    Column(modifier = Modifier.padding(start = 10.dp)) {
                                        Surface(
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                            shape = CircleShape
                                        ) {
                                            Text(
                                                text = "${sim.matchPercent}% Match",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                            )
                                        }
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            text = sim.book.title,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontFamily = PlayfairDisplay,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (sim.reasons.isNotEmpty()) {
                                            Text(
                                                text = sim.reasons.joinToString(" · "),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                SectionHeader("Ähnliche Bücher (Online entdecken)")
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
                    Text("Ähnliche Bücher online laden")
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
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    // DIALOG 1: Cover auswählen / aktualisieren
    if (showCoverPickerDialog) {
        AlertDialog(
            onDismissRequest = { showCoverPickerDialog = false },
            title = {
                Text(
                    "Buchcover wählen & aktualisieren",
                    fontFamily = PlayfairDisplay,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "Wähle ein passendes Ausgaben-Cover aus den Online-Katalogen (Google Books / Open Library) oder lade ein eigenes Bild aus deiner Galerie:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (coverCandidatesLoading) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            Text("Suche passende Cover-Ausgaben…", style = MaterialTheme.typography.bodySmall)
                        }
                    } else if (coverCandidates.isEmpty()) {
                        Text(
                            text = "Keine alternativen Online-Cover gefunden.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(coverCandidates) { candidate ->
                                val url = candidate.coverUrl ?: return@items
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .width(96.dp)
                                        .clickable {
                                            showCoverPickerDialog = false
                                            viewModel.selectCoverCandidate(url) { msg ->
                                                scope.launch { snackbarHostState.showSnackbar(msg) }
                                            }
                                        }
                                ) {
                                    BookCover(
                                        coverPath = null,
                                        coverUrl = url,
                                        contentDescription = candidate.title,
                                        title = candidate.title,
                                        author = candidate.author,
                                        cornerRadius = 8.dp,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .aspectRatio(2f / 3f)
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = candidate.publisher.ifBlank {
                                            candidate.publishedYear?.toString() ?: "Ausgabe wählen"
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            showCoverPickerDialog = false
                            viewModel.refreshCoverAndDetails { msg ->
                                scope.launch { snackbarHostState.showSnackbar(msg) }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Bestes Cover automatisch neu laden")
                    }

                    OutlinedButton(
                        onClick = {
                            showCoverPickerDialog = false
                            galleryCoverLauncher.launch("image/*")
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Eigenes Bild aus Galerie wählen")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCoverPickerDialog = false }) { Text("Schließen") }
            }
        )
    }

    // DIALOG 2: Tropes direkt auf der Detailseite bearbeiten
    if (showTropeDialog) {
        var selectedNames by remember(book) {
            mutableStateOf(book?.tropes?.map { it.name }?.toSet() ?: emptySet())
        }
        AlertDialog(
            onDismissRequest = { showTropeDialog = false },
            title = {
                Text(
                    "Tropes & Motive verwalten",
                    fontFamily = PlayfairDisplay,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TropePicker(
                        allTropes = allTropes,
                        selected = selectedNames,
                        onSelectionChange = { selectedNames = it }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showTropeDialog = false
                    viewModel.updateTropes(selectedNames)
                }) { Text("Speichern") }
            },
            dismissButton = {
                TextButton(onClick = { showTropeDialog = false }) { Text("Abbrechen") }
            }
        )
    }

    // DIALOG 3: Buchreihe, Band & Universum separat bearbeiten + automatisch erkennen
    if (showSeriesUniverseDialog) {
        var sName by remember(book) { mutableStateOf(book?.seriesName ?: "") }
        var sIndexText by remember(book) { mutableStateOf(book?.seriesIndex?.toString() ?: "") }
        var uName by remember(book) { mutableStateOf(book?.universeName ?: "") }

        AlertDialog(
            onDismissRequest = { showSeriesUniverseDialog = false },
            title = {
                Text(
                    "Buchreihe & Universum",
                    fontFamily = PlayfairDisplay,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    OutlinedButton(
                        onClick = {
                            val current = book
                            if (current != null) {
                                val detected = SeriesUniverseDetector.detect(
                                    title = current.title,
                                    subtitle = current.subtitle,
                                    author = current.author,
                                    description = current.description,
                                    existingSeries = seriesNames,
                                    existingUniverses = universeNames
                                )
                                if (detected.seriesName.isNotBlank()) sName = detected.seriesName
                                if (detected.seriesIndex != null) sIndexText = detected.seriesIndex.toString()
                                if (detected.universeName.isNotBlank()) uName = detected.universeName
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Automatisch erkennen")
                    }

                    AppTextField(
                        value = sName,
                        onValueChange = { sName = it },
                        label = { Text("Buchreihe / Serie (z. B. „Krähen-Dilogie“)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    val otherSeries = seriesNames.filter { it != sName }
                    if (otherSeries.isNotEmpty()) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            otherSeries.take(8).forEach { series ->
                                AssistChip(onClick = { sName = series }, label = { Text(series) })
                            }
                        }
                    }

                    AppTextField(
                        value = sIndexText,
                        onValueChange = { sIndexText = it.filter { c -> c.isDigit() }.take(3) },
                        label = { Text("Band / Nr. in der Reihe (z. B. 1)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    HorizontalDivider()

                    AppTextField(
                        value = uName,
                        onValueChange = { uName = it },
                        label = { Text("Buch-Universum / Welt (z. B. „Grishaverse“, „Maasverse“)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    val otherUniverses = universeNames.filter { it != uName }
                    if (otherUniverses.isNotEmpty()) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            otherUniverses.take(8).forEach { uni ->
                                AssistChip(onClick = { uName = uni }, label = { Text(uni) })
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showSeriesUniverseDialog = false
                    viewModel.setSeriesAndUniverse(
                        seriesName = sName,
                        seriesIndex = sIndexText.trim().toIntOrNull(),
                        universeName = uName
                    )
                }) { Text("Übernehmen") }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!book?.seriesName.isNullOrBlank() || !book?.universeName.isNullOrBlank()) {
                        TextButton(onClick = {
                            showSeriesUniverseDialog = false
                            viewModel.setSeriesAndUniverse("", null, "")
                        }) { Text("Zurücksetzen") }
                    }
                    TextButton(onClick = { showSeriesUniverseDialog = false }) { Text("Abbrechen") }
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

private fun formatLanguage(code: String): String = when (code.trim().lowercase()) {
    "de", "ger", "deu", "deutsch" -> "Deutsch"
    "en", "eng", "english" -> "Englisch"
    "fr", "fre", "fra" -> "Französisch"
    "es", "spa" -> "Spanisch"
    "it", "ita" -> "Italienisch"
    "" -> "–"
    else -> code.uppercase()
}

@Composable
private fun SeriesBookRow(
    book: Book,
    showSeriesName: Boolean = false,
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
                    if (showSeriesName && book.seriesName.isNotBlank()) add(book.seriesName)
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
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
            ) {
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
