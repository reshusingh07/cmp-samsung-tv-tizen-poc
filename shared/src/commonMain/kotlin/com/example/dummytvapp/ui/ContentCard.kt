package com.example.dummytvapp.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.dummytvapp.model.Content

/**
 * A small, fixed, non-network color palette used as a stand-in for a poster
 * image (see [Content] doc comment for why there is no real image loading).
 * Picked to look reasonably like distinct "movie poster" tones without
 * pulling in any color-science library.
 */
private val PosterPalette = listOf(
    Color(0xFF6A1B9A), Color(0xFF1565C0), Color(0xFF00695C),
    Color(0xFF2E7D32), Color(0xFFEF6C00), Color(0xFFC62828),
    Color(0xFFAD1457), Color(0xFF4527A0), Color(0xFF283593),
    Color(0xFF00838F), Color(0xFF558B2F), Color(0xFF8D6E63),
)

private val CardWidth = 160.dp
private val CardHeight = 210.dp
private val PosterHeight = 150.dp

/**
 * One "movie" tile: poster placeholder + title + category/year, with a
 * distinct visual state for normal / focused / selected (per the brief's
 * ASCII-art example of a double-bordered focused card).
 */
@Composable
fun ContentCard(
    content: Content,
    isFocused: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val borderWidth by animateDpAsState(if (isFocused) 4.dp else 0.dp)
    val poster = PosterPalette[content.paletteIndex % PosterPalette.size]

    Column(
        modifier = modifier
            .size(width = CardWidth, height = CardHeight)
            .zIndex(if (isFocused) 1f else 0f)
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
            .border(
                width = borderWidth,
                color = if (isSelected) MaterialTheme.colorScheme.tertiary
                        else MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(8.dp),
            )
            // Let's touch (Android/iOS) and mouse (desktop-browser/Tizen-with-
            // pointer) users tap a card directly, instead of only being able
            // to drive it with arrow keys + Enter. See HomeViewModel.selectAt.
            .clickable(onClick = onClick)
            .padding(6.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .size(width = CardWidth, height = PosterHeight)
                .background(poster, RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = content.title.substringAfterLast(' '),
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
            )
            if (isSelected) {
                SelectedBadge(modifier = Modifier.align(Alignment.TopEnd).padding(6.dp))
            }
        }
        Column(modifier = Modifier.padding(top = 6.dp, start = 2.dp, end = 2.dp)) {
            Text(
                text = content.title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${content.category} - ${content.year}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Small round badge shown on a card's poster once it has been "selected" (ENTER pressed). */
@Composable
private fun SelectedBadge(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(22.dp)
            .background(MaterialTheme.colorScheme.tertiary, RoundedCornerShape(50)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "\u2713", // check mark - plain Unicode text, no icon-library dependency needed
            color = MaterialTheme.colorScheme.onTertiary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}
