package com.gideon.buecherwelt.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import java.io.File

@Composable
fun bookCoverPlaceholder(): Painter =
    androidx.compose.ui.res.painterResource(android.R.drawable.ic_menu_agenda)

@Composable
fun BookCover(
    coverPath: String?,
    coverUrl: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier
) {
    val model: Any? = coverPath?.let { File(it) } ?: coverUrl
    if (model != null) {
        AsyncImage(
            model = model,
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = ContentScale.Crop,
            error = androidx.compose.ui.graphics.vector.rememberVectorPainter(Icons.Default.MenuBook),
            placeholder = androidx.compose.ui.graphics.vector.rememberVectorPainter(Icons.Default.MenuBook)
        )
    } else {
        Box(
            modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.MenuBook,
                contentDescription = contentDescription,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
