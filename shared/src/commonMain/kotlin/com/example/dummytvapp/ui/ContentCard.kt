package com.example.dummytvapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

/**
 * Card dimensions. `internal` because `HomeScreen` declares them to
 * `RokuLazyColumn`: the library places the focus highlight from the row's
 * declared item size, so the size it is told and the size the card draws have
 * to be the same number.
 */
internal val CardWidth = 160.dp
internal val CardHeight = 210.dp
private val PosterHeight = 150.dp

/**
 * One "movie" tile: poster placeholder + title + category/year.
 *
 * It draws no focus ring of its own. The focused card is ringed by
 * `roku-focus-list`, which renders a single highlight overlay at the row's fixed
 * focus slot (see `HomeScreen`'s `focusHighlight`) rather than per card -- so a
 * border here would simply double it. [isFocused] is still used, for the subtler
 * cue of lifting the focused card's surface.
 *
 * The tap handler is a raw `pointerInput` gesture rather than
 * `Modifier.clickable` on purpose: `clickable` makes a node focusable, which
 * would put a second, competing focus target inside a row that the library's
 * column already owns focus for. Keyboard/remote users go through the library's
 * `onItemClicked`; this is only for touch and mouse.
 */
@Composable
fun ContentCard(
    content: Content,
    isFocused: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val poster = PosterPalette[content.paletteIndex % PosterPalette.size]
    val surface =
        if (isFocused) MaterialTheme.colorScheme.surfaceVariant
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)

    Column(
        modifier = modifier
            .size(width = CardWidth, height = CardHeight)
            .background(surface, RoundedCornerShape(8.dp))
            .border(
                width = if (isSelected) 3.dp else 0.dp,
                color = if (isSelected) MaterialTheme.colorScheme.tertiary else Color.Transparent,
                shape = RoundedCornerShape(8.dp),
            )
            .pointerInput(onClick) { detectTapGestures { onClick() } }
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
            text = "✓", // check mark - plain Unicode text, no icon-library dependency needed
            color = MaterialTheme.colorScheme.onTertiary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}
