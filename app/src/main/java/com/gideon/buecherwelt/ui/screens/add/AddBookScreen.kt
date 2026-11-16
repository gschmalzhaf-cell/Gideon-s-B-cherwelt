package com.gideon.buecherwelt.ui.screens.add

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
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
    var author by remember(formInit) { mutableStateOf(formInit?.author ?: "") }
    var publisher by remember(formInit) { mutableStateOf(formInit?.publisher ?: "") }
    var yearText by remember(formInit) { mutableStateOf(formInit?.publishedYear?.toString() ?: "") }
    var description by remember(formInit) { mutableStateOf(formInit?.description ?: "") }
    var genre by remember(formInit) { mutableStateOf(formInit?.genre ?: "") }
    var subgenre by remember(formInit) { mutableStateOf(formInit?.subgenre ?: "") }
    var rating by remember(formInit) { mutableIntStateOf(formInit?.rating ?: 0) }
    var status by remember(formInit) { mutableStateOf(formInit?.status ?: ReadingStatus.PLANNED) }
    var spice by remember(formInit) { mutableIntStateOf(formInit?.spice ?: 0) }
    var notes by remember(formInit) { mutableStateOf(formInit?.notes ?: "") }
    var seriesName by remember(formInit) { mutableStateOf(formInit?.seriesName ?: "") }
    var seriesIndexText by remember(formInit) { mutableStateOf(formInit?.seriesIndex?.toString() ?: "") }
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
        author = author,
        publisher = publisher,
        publishedYear = yearText.trim().toIntOrNull(),
        description = description,
        genre = genre,
        subgenre = subgenre,
        spice = spice,
        rating = if (rating == 0) null else rating,
        status = status,
        notes = notes,
        seriesName = seriesName,
        seriesIndex = seriesIndexText.trim().toIntOrNull(),
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

            if (editBookId == null) {
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
                        AppTextField(
                            value = isbn,
                            onValueChange = { isbn = it },
                            label = { Text("ISBN / ASIN / Goodreads-ID") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { viewModel.onIsbnSubmitted(isbn) },
                                enabled = !state.loading && isbn.isNotBlank()
                            ) {
                                if (state.loading) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp))
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
                        OutlinedButton(
                            onClick = { showCoverDialog = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.PhotoCamera, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Cover scannen (Foto/Galerie)")
                        }
                        Text(
                            text = "Ohne Kennung: Felder unten manuell ausfüllen und speichern.",
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
                        if (editBookId != null) {
                            Spacer(Modifier.height(4.dp))
                            OutlinedButton(onClick = { coverPickerLauncher.launch("image/*") }) {
                                Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Cover ändern")
                            }
                        }
                    }
                }
            }

            AppTextField(value = title, onValueChange = { title = it }, label = { Text("Titel *") }, modifier = Modifier.fillMaxWidth())
            AppTextField(value = author, onValueChange = { author = it }, label = { Text("Autor:in") }, modifier = Modifier.fillMaxWidth())
            AppTextField(value = publisher, onValueChange = { publisher = it }, label = { Text("Verlag") }, modifier = Modifier.fillMaxWidth())
            AppTextField(
                value = yearText,
                onValueChange = { yearText = it.filter { c -> c.isDigit() }.take(4) },
                label = { Text("Erscheinungsjahr") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            AppTextField(value = genre, onValueChange = { genre = it }, label = { Text("Genre") }, modifier = Modifier.fillMaxWidth())
            AppTextField(value = subgenre, onValueChange = { subgenre = it }, label = { Text("Subgenre") }, modifier = Modifier.fillMaxWidth())
            AppTextField(
                value = seriesName,
                onValueChange = { seriesName = it },
                label = { Text("Serie / Reihe (z. B. „Fourth-Wing-Reihe“)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            if (state.seriesNames.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    state.seriesNames.forEach { name ->
                        AssistChip(
                            onClick = { seriesName = name },
                            label = { Text(name) }
                        )
                    }
                }
            }
            AppTextField(
                value = seriesIndexText,
                onValueChange = { seriesIndexText = it.filter { c -> c.isDigit() }.take(3) },
                label = { Text("Band / Nr. in der Serie") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
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
            if (state.llmConfigured && formInit != null && editBookId == null) {
                OutlinedButton(
                    onClick = viewModel::suggestTropes,
                    enabled = !state.suggestionLoading
                ) {
                    if (state.suggestionLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(8.dp))
                    Text("Tropes per LLM vorschlagen")
                }
            }
            if (state.suggestions != null) {
                SuggestionChips(
                    suggestions = state.suggestions!!,
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
