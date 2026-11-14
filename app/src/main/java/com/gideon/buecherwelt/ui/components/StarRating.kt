package com.gideon.buecherwelt.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gideon.buecherwelt.ui.theme.StarColor

@Composable
fun StarRating(
    rating: Int,
    onRatingChange: ((Int) -> Unit)? = null,
    size: Int = 20,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier) {
        for (star in 1..5) {
            val filled = star <= rating
            Icon(
                imageVector = if (filled) Icons.Default.Star else Icons.Outlined.StarBorder,
                contentDescription = "$star Sterne",
                tint = if (filled) StarColor else androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .size(size.dp)
                    .then(
                        if (onRatingChange != null) {
                            Modifier.clickable { onRatingChange(if (star == rating) 0 else star) }
                        } else Modifier
                    )
            )
        }
    }
}
