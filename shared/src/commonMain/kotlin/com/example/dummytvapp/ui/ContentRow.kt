package com.example.dummytvapp.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.dummytvapp.model.ContentSection

/**
 * One horizontally-scrolling row: a title (e.g. "Action") above a [LazyRow]
 * of [ContentCard]s. Per the brief's performance requirement, this uses
 * `LazyRow` rather than a plain `Row` so that with ~12 cards per row and 25
 * rows on screen (300 cards total), only the handful of cards actually
 * visible in this particular row are ever composed/measured at once.
 */
@Composable
fun ContentRow(
    section: ContentSection,
    focusedColumn: Int?,
    selectedId: Int?,
    onCardClick: (columnIndex: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()

    // Keep the focused card scrolled into view whenever focus moves onto
    // (or within) this row. `focusedColumn` is null for every row except the
    // one that currently has focus, so this effect is a no-op for the other
    // 24 rows.
    LaunchedEffect(focusedColumn) {
        if (focusedColumn != null) {
            listState.animateScrollToItem(focusedColumn)
        }
    }

    Column(modifier = modifier) {
        Text(
            text = section.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 24.dp, bottom = 8.dp),
        )
        LazyRow(
            state = listState,
            contentPadding = PaddingValues(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            itemsIndexed(section.items, key = { _, item -> item.id }) { index, item ->
                ContentCard(
                    content = item,
                    isFocused = focusedColumn == index,
                    isSelected = selectedId == item.id,
                    onClick = { onCardClick(index) },
                )
            }
        }
    }
}
