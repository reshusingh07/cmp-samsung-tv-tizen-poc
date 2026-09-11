package com.example.dummytvapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dummytvapp.viewmodel.HomeViewModel
import com.rokufocus.DefaultFocusHighlight
import com.rokufocus.RokuAnimationSpec
import com.rokufocus.RokuColumnState
import com.rokufocus.RokuFocusConfig
import com.rokufocus.RokuFocusEscape
import com.rokufocus.RokuLazyColumn

/**
 * The single "TV home" screen: 25 rows of dummy cards, navigated Roku-style.
 *
 * Rendering and navigation both belong to `RokuLazyColumn` (see the vendored
 * `:roku-focus-list` module). What that buys over the plain
 * `LazyColumn`-of-`LazyRow` this screen used before:
 *
 * - **The highlight is parked, the content moves.** Each row declares
 *   `focusSlot = 1`, so the focused card sits in the second visible slot and
 *   the row scrolls underneath it. Previously the focused card was scrolled to
 *   the screen edge, which on a TV reads as the content lurching sideways.
 * - **Focus is remembered per row.** The library keeps one state object per
 *   row, so leaving a row at card 8 and coming back lands on card 8 again.
 * - **Held directions are throttled, then accelerated.** A D-pad repeat fires
 *   far faster than a scroll animation settles; the library drops repeats that
 *   arrive inside `keyRepeatDelayMs` and speeds up to `keyRepeatFastDelayMs`
 *   once the user is clearly holding the key.
 *
 * There is no navigation beyond this screen -- see README -> "No backend" for
 * why there is nowhere to navigate *to*.
 */
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    columnState: RokuColumnState,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        Column {
            Text(
                text = "Dummy TV App",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 24.dp, top = 20.dp, bottom = 12.dp),
            )

            RokuLazyColumn(
                state = columnState,
                config = RokuFocusConfig(
                    highlightAnimationSpec = RokuAnimationSpec.Smooth,
                    // A remote's auto-repeat is quicker than a scroll settles;
                    // these are the library's defaults, nudged slightly faster
                    // because every row here is a uniform, cheap card.
                    keyRepeatDelayMs = 120L,
                    keyRepeatAccelAfter = 3,
                    keyRepeatFastDelayMs = 45L,
                    // No haptics: a TV remote has none, and on tvOS the call
                    // would be a no-op anyway.
                    hapticFeedback = false,
                    // This grid is the whole screen. There is no sidebar or nav
                    // rail to hand focus to, so an edge press should stay put
                    // rather than letting Compose's focus search wander off.
                    focusEscape = RokuFocusEscape.None,
                ),
                contentPadding = PaddingValues(bottom = 32.dp),
                rowSpacing = 20.dp,
                focusHighlight = { isFocused ->
                    DefaultFocusHighlight(
                        isFocused = isFocused,
                        borderColor = MaterialTheme.colorScheme.primary,
                        borderWidth = 3.dp,
                        cornerRadius = 8.dp,
                        overflow = 5.dp,
                        animateScale = true,
                    )
                },
                onItemClicked = { rowIndex, itemIndex -> viewModel.select(rowIndex, itemIndex) },
            ) {
                viewModel.sections.forEachIndexed { rowIndex, section ->
                    row(
                        // Keyed by section id so a row keeps its own remembered
                        // card if the row list is ever reordered.
                        key = section.id,
                        itemWidth = CardWidth,
                        itemHeight = CardHeight,
                        itemSpacing = 12.dp,
                        contentPadding = PaddingValues(horizontal = 24.dp),
                        // Park the highlight one card in, so there is always a
                        // card of context to its left.
                        focusSlot = 1,
                        // headerHeight is left out on purpose: the library
                        // measures the header itself, so the highlight lands
                        // correctly without this screen hard-coding a text height.
                        header = { isRowFocused -> RowHeader(section.title, isRowFocused) },
                    ) {
                        items(
                            count = section.items.size,
                            key = { index -> section.items[index].id },
                            contentDescription = { index -> section.items[index].title },
                        ) { itemIndex, isFocused ->
                            val item = section.items[itemIndex]
                            ContentCard(
                                content = item,
                                isFocused = isFocused,
                                isSelected = viewModel.selected?.id == item.id,
                                onClick = { viewModel.select(rowIndex, itemIndex) },
                            )
                        }
                    }
                }
            }
        }

        viewModel.selected?.let { selected ->
            SelectionOverlay(
                title = selected.title,
                subtitle = "${selected.category} - ${selected.year}",
                onDismiss = { viewModel.back() },
            )
        }
    }
}

/** Row title, dimmed while the row is not the focused one. */
@Composable
private fun RowHeader(title: String, isRowFocused: Boolean) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color =
            if (isRowFocused) MaterialTheme.colorScheme.onSurface
            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        modifier = Modifier.padding(start = 24.dp, bottom = 8.dp),
    )
}

/**
 * A simple full-screen overlay standing in for "you pressed ENTER on a
 * card". There is intentionally no video player / details page behind it
 * (see README -> "No backend"): this exists purely to make ENTER-to-select
 * and BACK-to-dismiss visibly demonstrable, per the brief's verification
 * checklist ("ENTER selection works" / "BACK behavior is handled").
 */
@Composable
private fun SelectionOverlay(title: String, subtitle: String, onDismiss: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.85f))
            // Tapping anywhere dismisses it too - the only way to dismiss at
            // all on a touch-only device with no keyboard/remote. A raw gesture
            // rather than `clickable` for the same reason as ContentCard: this
            // must not become a focus target competing with the grid.
            .pointerInput(onDismiss) { detectTapGestures { onDismiss() } },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Selected",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            )
            Text(
                text = title,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            )
            Text(
                text = "Press BACK to return",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                modifier = Modifier.padding(top = 16.dp),
            )
        }
    }
}
