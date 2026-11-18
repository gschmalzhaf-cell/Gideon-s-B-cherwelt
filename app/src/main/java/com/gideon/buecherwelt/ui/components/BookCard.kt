package com.gideon.buecherwelt.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gideon.buecherwelt.data.local.ReadingStatus
import com.gideon.buecherwelt.domain.model.Book
import com.gideon.buecherwelt.ui.theme.PlayfairDisplay

@Composable
fun BookCard(
    book: Book,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 6.dp, end = 6.dp, top = 6.dp)
            ) {
                BookCover(
                    coverPath = book.coverPath,
                    coverUrl = book.coverUrl,
                    contentDescription = book.title,
                    title = book.title,
                    author = book.author,
                    cornerRadius = 10.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(2f / 3f)
                )

                // Lesezeichen-Bändchen oben rechts bei "Lesend"
                if (book.status == ReadingStatus.READING) {
                    Icon(
                        imageVector = Icons.Default.Bookmark,
                        contentDescription = "Aktuell lesend",
                        tint = Color(0xFFB3261E),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(end = 6.dp)
                            .size(24.dp)
                    )
                }

                // Serien-Band-Plakette unten links auf dem Cover
                if (book.seriesName.isNotBlank() || book.seriesIndex != null) {
                    val badgeText = when {
                        book.seriesIndex != null && book.seriesName.isNotBlank() ->
                            "Bd. ${book.seriesIndex}"
                        book.seriesIndex != null ->
                            "Bd. ${book.seriesIndex}"
                        else ->
                            book.seriesName.take(14)
                    }
                    Surface(
                        color = Color(0xFF211710).copy(alpha = 0.86f),
                        shape = CircleShape,
                        border = BorderStroke(0.5.dp, Color(0xFFDDB968).copy(alpha = 0.75f)),
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(6.dp)
                    ) {
                        Text(
                            text = badgeText,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFF6DFC6),
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }
                } else if (book.universeName.isNotBlank()) {
                    Surface(
                        color = Color(0xFF211710).copy(alpha = 0.84f),
                        shape = CircleShape,
                        border = BorderStroke(0.5.dp, Color(0xFFDDB968).copy(alpha = 0.65f)),
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(6.dp)
                    ) {
                        Text(
                            text = book.universeName.take(14),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFF6DFC6),
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }
                }

                // KI-Fit-Badge oben links, falls vorhanden
                if (book.aiFitScore != null && book.aiFitScore >= 75) {
                    Surface(
                        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.92f),
                        shape = RoundedCornerShape(bottomEnd = 8.dp, topStart = 4.dp),
                        modifier = Modifier.align(Alignment.TopStart)
                    ) {
                        Text(
                            text = "${book.aiFitScore}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Holzregal-Leiste direkt unter dem Buch
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
                    .height(4.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                            )
                        ),
                        shape = RoundedCornerShape(2.dp)
                    )
            )

            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                Text(
                    text = book.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = PlayfairDisplay,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (book.author.isNotBlank()) {
                    Text(
                        text = book.author,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                val seriesOrUniverseLine = buildString {
                    if (book.seriesName.isNotBlank()) {
                        append(book.seriesName)
                        if (book.seriesIndex != null) append(" #${book.seriesIndex}")
                    } else if (book.universeName.isNotBlank()) {
                        append(book.universeName)
                    }
                }
                if (seriesOrUniverseLine.isNotBlank()) {
                    Text(
                        text = seriesOrUniverseLine,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 1.dp)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if ((book.rating ?: 0) > 0) {
                        StarRating(rating = book.rating!!, size = 12)
                    } else if ((book.spiceLevel ?: 0) > 0) {
                        SpiceFlames(level = book.spiceLevel, iconSize = 11)
                    } else {
                        Spacer(modifier = Modifier.width(2.dp))
                    }
                    StatusChip(status = book.status)
                }
            }
        }
    }
}

