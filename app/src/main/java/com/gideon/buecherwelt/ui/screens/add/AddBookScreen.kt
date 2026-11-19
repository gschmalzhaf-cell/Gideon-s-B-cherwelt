package com.gideon.buecherwelt.ui.screens.add

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.gideon.buecherwelt.data.network.CoverCache
import kotlinx.coroutines.launch
import java.io.File
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.gideon.buecherwelt.data.local.ReadingStatus
import com.gideon.buecherwelt.data.local.TropesCatalog
import com.gideon.buecherwelt.di.AppContainer
import com.gideon.buecherwelt.ui.components.AppTextField
import com.gideon.buecherwelt.ui.components.BookCover
import com.gideon.buecherwelt.ui.components.ErrorBanner
import com.gideon.buecherwelt.ui.components.StarRating
import com.gideon.buecherwelt.ui.components.SuggestionChips
import com.gideon.buecherwelt.ui.components.TropePicker
import com.gideon.buecherwelt.ui.navigation.Screen
import com.gideon.buecherwelt.ui.viewModelFactory

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddBookScreen(
    container: AppContainer,
    navController: NavController,
    editBookId: Long?,
    scannedIsbn: MutableState<String?>,
    coverImagePath: MutableState<String?>
) {
    val viewModel: AddBookViewModel = viewModel(
        factory = viewModelFactory {
            AddBookViewModel(
                bookRepository = container.bookRepository,
                tropeRepository = container.tropeRepository,
                metadataFetcher = container.metadataFetcher,
                coverCache = container.coverCache,
                settingsRepository = container.settingsRepository,
                llmService = container.llmService,
                coverIdentifier = container.coverIdentifier,
                editBookId = editBookId
            )
        }
    )
    val state by viewModel.state.collectAsStateWithLifecycle()

    var isbn by remember { mutableStateOf("") }
    val formInit = state.formInit
    var title by remember(formInit) { mutableStateOf(formInit?.title ?: "") }
    var subtitle by remember(formInit) { mutableStateOf(formInit?.subtitle ?: "") }
    var author by remember(formInit) { mutableStateOf(formInit?.author ?: "") }
    var publisher by remember(formInit) { mutableStateOf(formInit?.publisher ?: "") }
    var yearText by remember(formInit) { mutableStateOf(formInit?.publishedYear?.toString() ?: "") }
    var pageCountText by remember(formInit) { mutableStateOf(formInit?.pageCount?.toString() ?: "") }
    var language by remember(formInit) { mutableStateOf(formInit?.language ?: "") }
    var description by remember(formInit) { mutableStateOf(formInit?.description ?: "") }
    var genre by remember(formInit) { mutableStateOf(formInit?.genre ?: "") }
    var subgenre by remember(formInit) { mutableStateOf(formInit?.subgenre ?: "") }
    var rating by remember(formInit) { mutableIntStateOf(formInit?.rating ?: 0) }
    var status by remember(formInit) { mutableStateOf(formInit?.status ?: ReadingStatus.PLANNED) }
    var spice by remember(formInit) { mutableIntStateOf(formInit?.spice ?: 0) }
    var notes by remember(formInit) { mutableStateOf(formInit?.notes ?: "") }
    var seriesName by remember(formInit) { mutableStateOf(formInit?.seriesName ?: "") }
    var seriesIndexText by remember(formInit) { mutableStateOf(formInit?.seriesIndex?.toString() ?: "") }
    var universeName by remember(formInit) { mutableStateOf(formInit?.universeName ?: "") }
    var coverUrl by remember(formInit) { mutableStateOf(formInit?.coverUrl) }
    var coverPath by remember(formInit) { mutableStateOf(formInit?.coverPath) }
    var selectedTropes by remember(formInit) { mutableStateOf(formInit?.tropes ?: emptySet()) }
    var selectedGenreSuggestions by remember(formInit) { mutableStateOf(emptySet<String>()) }
    var saveResult by remember { mutableStateOf<SaveResult?>(null) }
    var pendingForm by remember { mutableStateOf<BookForm?>(null) }

    LaunchedEffect(scannedIsbn.value) {
        val scanned = scannedIsbn.value
        if (!scanned.isNullOrBlank()) {
            scannedIsbn.value = null
            isbn = scanned
            viewModel.onIsbnSubmitted(scanned)
        }
    }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showCoverDialog by remember { mutableStateOf(false) }
    val coverPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            scope.launch {
                val path = container.coverCache.savePicked(
                    context.contentResolver,
                    it,
                    CoverCache.keyFor(formInit?.isbn, title.ifBlank { "manual" }, author)
                )
                if (path != null) {
                    coverPath = path
                    coverUrl = null
                }
            }
        }
    }
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            try {
                val file = File(context.cacheDir, "cover_gallery_${System.currentTimeMillis()}.jpg")
                context.contentResolver.openInputStream(it)?.use { input ->
                    file.outputStream().use { out -> input.copyTo(out) }
                }
                viewModel.onCoverImage(file.absolutePath)
            } catch (e: Exception) {
                viewModel.onCoverImageFailed()
            }
        }
    }

    LaunchedEffect(coverImagePath.value) {
        val path = coverImagePath.value
        if (!path.isNullOrBlank()) {
            coverImagePath.value = null
            viewModel.onCoverImage(path)
        }
    }

    fun currentForm(): BookForm = BookForm(
        title = title,
        subtitle = subtitle,
        author = author,
        publisher = publisher,
        publishedYear = yearText.trim().toIntOrNull(),
        pageCount = pageCountText.trim().toIntOrNull(),
        language = language,
        description = description,
        genre = genre,
        subgenre = subgenre,
        spice = spice,
        rating = if (rating == 0) null else rating,
        status = status,
        notes = notes,
        seriesName = seriesName,
        seriesIndex = seriesIndexText.trim().toIntOrNull(),
        universeName = universeName,
        selectedTropes = selectedTropes
    )

    fun doSave() {
        pendingForm = currentForm()
        viewModel.save(
            form = pendingForm!!,
            isbn = formInit?.isbn ?: isbn.ifBlank { null },
            coverUrl = coverUrl,
            coverPath = coverPath
        ) { result -> saveResult = result }
    }

    LaunchedEffect(saveResult) {
        when (val result = saveResult) {
            is SaveResult.Saved -> {
                navController.navigate(Screen.BookDetail.create(result.bookId)) {
                    popUpTo(Screen.Library.route) { inclusive = false }
                }
            }
            else -> Unit
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(if (editBookId == null) "Buch erfassen" else "Buch bearbeiten") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (state.error != null) {
                ErrorBanner(message = state.error!!, onDismiss = viewModel::dismissError)
            }
            if (state.statusInfo != null) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = state.statusInfo!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = viewModel::dismissStatusInfo,
                            modifier = Modifier.size(22.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Schließen", modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            if (editBookId == null) {
                var searchMode by remember { mutableIntStateOf(0) } // 0 = ISBN/ASIN, 1 = Titel-Suche
                var titleSearchQuery by remember { mutableStateOf("") }
                var authorSearchQuery by remember { mutableStateOf("") }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            listOf("ISBN / ASIN", "Titel-Suche").forEachIndexed { index, label ->
                                SegmentedButton(
                                    selected = searchMode == index,
                                    onClick = { searchMode = index },
                                    shape = SegmentedButtonDefaults.itemShape(index, 2)
                                ) {
                                    Text(label)
                                }
                            }
                        }

                        if (searchMode == 0) {
                            AppTextField(
                                value = isbn,
                                onValueChange = { isbn = it },
                                label = { Text("ISBN / ASIN / Amazon-Link / Goodreads-ID") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { viewModel.onIsbnSubmitted(isbn) },
                                    enabled = !state.loading && !state.aiCompletionLoading && isbn.isNotBlank()
                                ) {
                                    if (state.loading) {
                                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                    } else {
                                        Icon(Icons.Default.Search, contentDescription = null)
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    Text("Daten laden")
                                }
                                OutlinedButton(onClick = { navController.navigate(Screen.Scanner.route) }) {
                                    Icon(Icons.Default.QrCodeScanner, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("ISBN scannen")
                                }
                            }
                            if (state.llmConfigured && isbn.isNotBlank()) {
                                OutlinedButton(
                                    onClick = { viewModel.completeWithAi(identifier = isbn) },
                                    enabled = !state.loading && !state.aiCompletionLoading,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    if (state.aiCompletionLoading) {
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                    } else {
                                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    Text("Kennung / ASIN mit KI suchen & vervollständigen")
                                }
                            }
                        } else {
                            AppTextField(
                                value = titleSearchQuery,
                                onValueChange = { titleSearchQuery = it },
                                label = { Text("Buchtitel suchen *") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            AppTextField(
                                value = authorSearchQuery,
                                onValueChange = { authorSearchQuery = it },
                                label = { Text("Autor:in (optional)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Button(
                                onClick = { viewModel.searchByTitle(titleSearchQuery, authorSearchQuery) },
                                enabled = !state.loading && !state.aiCompletionLoading && titleSearchQuery.isNotBlank(),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                if (state.loading) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                } else {
                                    Icon(Icons.Default.Search, contentDescription = null)
                                }
                                Spacer(Modifier.width(8.dp))
                                Text("Nach Titel suchen")
                            }
                            if (state.llmConfigured) {
                                OutlinedButton(
                                    onClick = {
                                        viewModel.completeWithAi(
                                            title = titleSearchQuery.ifBlank { title },
                                            author = authorSearchQuery.ifBlank { author }
                                        )
                                    },
                                    enabled = !state.loading && !state.aiCompletionLoading && (titleSearchQuery.isNotBlank() || title.isNotBlank()),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    if (state.aiCompletionLoading) {
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                    } else {
                                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    Text("Mit KI suchen & Felder vervollständigen")
                                }
                            }
                        }

                        if (state.searchResults.isNotEmpty() || state.searchResultsEmpty) {
                            HorizontalDivider()
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (state.searchResultsEmpty) {
                                        "Keine Treffer im Katalog gefunden."
                                    } else {
                                        "Treffer auswählen (${state.searchResults.size})"
                                    },
                                    style = MaterialTheme.typography.labelLarge
                                )
                                TextButton(onClick = viewModel::clearSearchResults) {
                                    Text("Schließen")
                                }
                            }
                            state.searchResults.forEach { cand ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.selectSearchResult(cand) }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        BookCover(
                                            coverPath = null,
                                            coverUrl = cand.coverUrl,
                                            contentDescription = cand.title,
                                            title = cand.title,
                                            author = cand.author,
                                            cornerRadius = 6.dp,
                                            modifier = Modifier
                                                .width(44.dp)
                                                .aspectRatio(2f / 3f)
                                        )
                                        Column(
                                            modifier = Modifier
                                                .padding(start = 10.dp)
                                                .weight(1f)
                                        ) {
                                            Text(
                                                text = cand.title,
                                                style = MaterialTheme.typography.bodyMedium,
                                                maxLines = 2
                                            )
                                            if (cand.author.isNotBlank()) {
                                                Text(
                                                    text = cand.author,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1
                                                )
                                            }
                                            val metaLine = listOfNotNull(
                                                cand.publisher.takeIf { it.isNotBlank() },
                                                cand.publishedYear?.toString(),
                                                cand.pageCount?.let { "$it S." }
                                            ).joinToString(" · ")
                                            if (metaLine.isNotBlank()) {
                                                Text(
                                                    text = metaLine,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            if (state.llmConfigured) {
                                OutlinedButton(
                                    onClick = {
                                        viewModel.completeWithAi(
                                            title = titleSearchQuery.ifBlank { title },
                                            author = authorSearchQuery.ifBlank { author }
                                        )
                                    },
                                    enabled = !state.aiCompletionLoading && (titleSearchQuery.isNotBlank() || title.isNotBlank()),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Nicht dabei? Mit KI vervollständigen")
                                }
                            }
                        }

                        OutlinedButton(
                            onClick = { showCoverDialog = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.PhotoCamera, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Cover scannen (Foto/Galerie)")
                        }
                        Text(
                            text = "Ohne Kennung: Titel-Suche nutzen oder Felder unten manuell ausfüllen.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            formInit?.let { init ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.width(84.dp)) {
                        BookCover(
                            coverPath = coverPath,
                            coverUrl = coverUrl,
                            contentDescription = init.title,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Column(modifier = Modifier.padding(start = 12.dp)) {
                        Text("Cover", style = MaterialTheme.typography.labelMedium)
                        if (coverPath != null) {
                            Text(
                                "lokal gecacht (offline verfügbar)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else if (coverUrl != null) {
                            Text(
                                "Remote-Cover",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Text(
                                "kein Cover",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        OutlinedButton(onClick = { coverPickerLauncher.launch("image/*") }) {
                            Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Cover ändern")
                        }
                    }
                }
            }

            AppTextField(value = title, onValueChange = { title = it }, label = { Text("Titel *") }, modifier = Modifier.fillMaxWidth())
            AppTextField(value = subtitle, onValueChange = { subtitle = it }, label = { Text("Untertitel") }, modifier = Modifier.fillMaxWidth())
            AppTextField(value = author, onValueChange = { author = it }, label = { Text("Autor:in") }, modifier = Modifier.fillMaxWidth())
            if (state.llmConfigured && (title.isNotBlank() || isbn.isNotBlank())) {
                OutlinedButton(
                    onClick = {
                        viewModel.completeWithAi(
                            identifier = formInit?.isbn ?: isbn,
                            title = title,
                            subtitle = subtitle,
                            author = author,
                            publisher = publisher,
                            publishedYear = yearText.trim().toIntOrNull(),
                            pageCount = pageCountText.trim().toIntOrNull(),
                            language = language,
                            description = description,
                            genre = genre,
                            subgenre = subgenre,
                            seriesName = seriesName,
                            seriesIndex = seriesIndexText.trim().toIntOrNull(),
                            universeName = universeName,
                            spice = spice,
                            selectedTropes = selectedTropes,
                            coverUrl = coverUrl,
                            coverPath = coverPath
                        )
                    },
                    enabled = !state.aiCompletionLoading && !state.loading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (state.aiCompletionLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(8.dp))
                    Text("Fehlende Buchdetails & Cover mit KI vervollständigen")
                }
            }
            AppTextField(value = publisher, onValueChange = { publisher = it }, label = { Text("Verlag") }, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                AppTextField(
                    value = yearText,
                    onValueChange = { yearText = it.filter { c -> c.isDigit() }.take(4) },
                    label = { Text("Erscheinungsjahr") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                AppTextField(
                    value = pageCountText,
                    onValueChange = { pageCountText = it.filter { c -> c.isDigit() }.take(5) },
                    label = { Text("Seitenzahl") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }
            AppTextField(value = genre, onValueChange = { genre = it }, label = { Text("Genre") }, modifier = Modifier.fillMaxWidth())
            AppTextField(value = subgenre, onValueChange = { subgenre = it }, label = { Text("Subgenre") }, modifier = Modifier.fillMaxWidth())

            // Buchreihe, Band & Universum (separat erfasst + automatische Erkennung)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Buchreihe, Band & Universum", style = MaterialTheme.typography.labelLarge)
                        TextButton(
                            onClick = {
                                val detected = com.gideon.buecherwelt.domain.analysis.SeriesUniverseDetector.detect(
                                    title = title,
                                    subtitle = subtitle,
                                    author = author,
                                    description = description,
                                    existingSeries = state.seriesNames,
                                    existingUniverses = state.universeNames
                                )
                                if (detected.seriesName.isNotBlank()) seriesName = detected.seriesName
                                if (detected.seriesIndex != null) seriesIndexText = detected.seriesIndex.toString()
                                if (detected.universeName.isNotBlank()) universeName = detected.universeName
                            },
                            enabled = title.isNotBlank() || description.isNotBlank()
                        ) {
                            Text("Automatisch erkennen")
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        AppTextField(
                            value = seriesName,
                            onValueChange = { seriesName = it },
                            label = { Text("Buchreihe / Serie") },
                            singleLine = true,
                            modifier = Modifier.weight(2f)
                        )
                        AppTextField(
                            value = seriesIndexText,
                            onValueChange = { seriesIndexText = it.filter { c -> c.isDigit() }.take(3) },
                            label = { Text("Band Nr.") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (state.seriesNames.isNotEmpty()) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            state.seriesNames.take(8).forEach { name ->
                                AssistChip(
                                    onClick = { seriesName = name },
                                    label = { Text(name) }
                                )
                            }
                        }
                    }

                    AppTextField(
                        value = universeName,
                        onValueChange = { universeName = it },
                        label = { Text("Buch-Universum / Welt (z. B. „Grishaverse“, „Maasverse“)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (state.universeNames.isNotEmpty()) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            state.universeNames.take(8).forEach { uni ->
                                AssistChip(
                                    onClick = { universeName = uni },
                                    label = { Text(uni) }
                                )
                            }
                        }
                    }
                }
            }

            AppTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Beschreibung / Klappentext") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth()
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Bewertung", style = MaterialTheme.typography.labelLarge)
                    StarRating(rating = rating, onRatingChange = { rating = it }, size = 28)

                    StatusDropdown(status = status, onChange = { status = it })

                    Text("Spice-Level: $spice", style = MaterialTheme.typography.labelLarge)
                    Text(
                        TropesCatalog.spiceLevels.first { it.first == spice }.second,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Slider(
                        value = spice.toFloat(),
                        onValueChange = { spice = it.toInt() },
                        valueRange = 0f..5f,
                        steps = 4,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Text("Tropes / Tags", style = MaterialTheme.typography.labelLarge)
            OutlinedButton(
                onClick = {
                    viewModel.suggestTropes(
                        currentTitle = title,
                        currentSubtitle = subtitle,
                        currentAuthor = author,
                        currentDescription = description,
                        currentGenre = genre,
                        currentSubgenre = subgenre
                    )
                },
                enabled = !state.suggestionLoading && (title.isNotBlank() || description.isNotBlank())
            ) {
                if (state.suggestionLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    if (state.llmConfigured) "Tropes, Reihe & Spice intelligent erkennen (KI + Klappentext)"
                    else "Tropes, Reihe & Spice intelligent aus Klappentext erkennen"
                )
            }
            if (state.suggestions != null) {
                val sug = state.suggestions!!
                LaunchedEffect(sug) {
                    if (seriesName.isBlank() && sug.seriesName.isNotBlank()) seriesName = sug.seriesName
                    if (seriesIndexText.isBlank() && sug.seriesIndex != null) seriesIndexText = sug.seriesIndex.toString()
                    if (universeName.isBlank() && sug.universeName.isNotBlank()) universeName = sug.universeName
                }
                SuggestionChips(
                    suggestions = sug,
                    selectedTropes = selectedTropes,
                    onTropeToggle = { t ->
                        selectedTropes = if (t in selectedTropes) selectedTropes - t else selectedTropes + t
                    },
                    selectedSpice = spice,
                    onSpiceSelected = { spice = it },
                    selectedGenres = selectedGenreSuggestions,
                    onGenreToggle = { g ->
                        selectedGenreSuggestions = if (g in selectedGenreSuggestions) {
                            selectedGenreSuggestions - g
                        } else {
                            selectedGenreSuggestions + g
                        }
                        if (g in selectedGenreSuggestions && genre.isBlank()) genre = g
                    }
                )
            }
            TropePicker(
                allTropes = state.allTropes,
                selected = selectedTropes,
                onSelectionChange = { selectedTropes = it }
            )

            AppTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notizen") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { doSave() },
                enabled = !state.saving && title.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (state.saving) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                }
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (editBookId == null) "Zur Bibliothek hinzufügen" else "Änderungen speichern")
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showCoverDialog) {
        AlertDialog(
            onDismissRequest = { showCoverDialog = false },
            title = { Text("Cover scannen") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Foto aufnehmen oder aus der Galerie wählen. Die App erkennt das Buch " +
                            "per LLM (falls in den Einstellungen konfiguriert) oder Texterkennung (OCR) " +
                            "und lädt die Metadaten über Google Books / Open Library / DNB."
                    )
                    TextButton(onClick = {
                        showCoverDialog = false
                        navController.navigate(Screen.CoverScanner.route)
                    }) { Text("Foto aufnehmen") }
                    TextButton(onClick = {
                        showCoverDialog = false
                        galleryLauncher.launch("image/*")
                    }) { Text("Aus Galerie wählen") }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCoverDialog = false }) { Text("Abbrechen") }
            }
        )
    }

    when (val result = saveResult) {
        is SaveResult.Duplicate -> {
            AlertDialog(
                onDismissRequest = { saveResult = null },
                title = { Text("Buch bereits vorhanden") },
                text = {
                    Text("„${result.existing.title}“ von ${result.existing.author.ifBlank { "unbekannt" }} ist bereits in deiner Bibliothek. Zusammenführen (leere Felder ergänzen, Tropes vereinen)?")
                },
                confirmButton = {
                    TextButton(onClick = {
                        val form = pendingForm
                        if (form != null) {
                            viewModel.mergeDuplicate(
                                existing = result.existing,
                                form = form,
                                isbn = formInit?.isbn ?: isbn.ifBlank { null },
                                coverUrl = coverUrl,
                                coverPath = coverPath
                            ) { r -> saveResult = r }
                        }
                    }) { Text("Zusammenführen") }
                },
                dismissButton = {
                    TextButton(onClick = { saveResult = null }) { Text("Abbrechen") }
                }
            )
        }
        is SaveResult.Error -> {
            AlertDialog(
                onDismissRequest = { saveResult = null },
                title = { Text("Fehler") },
                text = { Text(result.message) },
                confirmButton = {
                    TextButton(onClick = { saveResult = null }) { Text("OK") }
                }
            )
        }
        else -> Unit
    }
}

@Composable
private fun StatusDropdown(
    status: ReadingStatus,
    onChange: (ReadingStatus) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }) {
            Text("Status: ${status.label}")
            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            ReadingStatus.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label) },
                    onClick = {
                        onChange(option)
                        expanded = false
                    }
                )
            }
        }
    }
}
