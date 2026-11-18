package com.gideon.buecherwelt.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.gideon.buecherwelt.data.network.CoverCache
import com.gideon.buecherwelt.ui.theme.PlayfairDisplay
import java.io.File
import kotlin.math.abs

private val BookBindingGradients = listOf(
    listOf(Color(0xFF8D5A33), Color(0xFF4E2F18)), // Lederbraun
    listOf(Color(0xFF5D6B3F), Color(0xFF333B20)), // Bibliotheksgrün
    listOf(Color(0xFF9C4A3A), Color(0xFF5A251B)), // Burgunderrot
    listOf(Color(0xFF3F5D6B), Color(0xFF22353E)), // Mitternachtsblau
    listOf(Color(0xFF8F6B29), Color(0xFF4F3A12)), // Antikgold
    listOf(Color(0xFF6B4E5D), Color(0xFF3D2A34))  // Pflaume / Klassik
)

@Composable
fun BookCover(
    coverPath: String?,
    coverUrl: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    title: String = contentDescription.orEmpty(),
    author: String = "",
    cornerRadius: Dp = 8.dp
) {
    val shape = RoundedCornerShape(
        topStart = 3.dp,
        bottomStart = 3.dp,
        topEnd = cornerRadius,
        bottomEnd = cornerRadius
    )
    val validFile = remember(coverPath) {
        if (CoverCache.isValidCoverFile(coverPath)) {
            File(coverPath!!)
        } else null
    }
    val model: Any? = validFile ?: coverUrl?.takeIf { it.isNotBlank() }
    var imageError by remember(model) { mutableStateOf(false) }

    Box(
        modifier = modifier
            .clip(shape)
            .border(
                width = 0.75.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f),
                shape = shape
            )
    ) {
        val paletteIndex = abs((title + author).hashCode()) % BookBindingGradients.size
        val gradientColors = BookBindingGradients[paletteIndex]
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.linearGradient(gradientColors))
                .padding(horizontal = 8.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(
                        width = 0.8.dp,
                        color = Color(0xFFEBD5B3).copy(alpha = 0.4f),
                        shape = RoundedCornerShape(4.dp)
                    )
                    .padding(6.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoStories,
                        contentDescription = null,
                        tint = Color(0xFFF6E7C1).copy(alpha = 0.85f),
                        modifier = Modifier.size(18.dp)
                    )
                    if (title.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = title,
                            style = MaterialTheme.typography.labelMedium,
                            fontFamily = PlayfairDisplay,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFFBF6),
                            textAlign = TextAlign.Center,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (author.isNotBlank()) {
                        Spacer(Modifier.height(3.dp))
                        HorizontalDivider(
                            modifier = Modifier.width(24.dp),
                            thickness = 0.5.dp,
                            color = Color(0xFFEBD5B3).copy(alpha = 0.5f)
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            text = author,
                            style = MaterialTheme.typography.labelSmall,
                            fontStyle = FontStyle.Italic,
                            color = Color(0xFFF1DEC9).copy(alpha = 0.9f),
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        if (model != null && !imageError) {
            AsyncImage(
                model = model,
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                onError = { imageError = true },
                onSuccess = { imageError = false }
            )
        }

        // Realistischer Buchfalz (Spine-Schatten & Glanzkante links)
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(10.dp)
                .align(Alignment.CenterStart)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.32f),
                            Color.White.copy(alpha = 0.14f),
                            Color.Black.copy(alpha = 0.10f),
                            Color.Transparent
                        )
                    )
                )
        )
        // Dezente Buchseiten-Kante rechts
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(2.dp)
                .align(Alignment.CenterEnd)
                .background(Color(0xFFFFFBF6).copy(alpha = 0.25f))
        )
    }
}

