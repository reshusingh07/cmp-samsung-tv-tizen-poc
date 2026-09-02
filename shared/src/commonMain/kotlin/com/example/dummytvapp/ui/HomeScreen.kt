package com.example.dummytvapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dummytvapp.viewmodel.HomeViewModel

/**
 * The single "TV home" screen: a vertically-scrolling [LazyColumn] of the 25
 * [ContentRow]s, a small header, and a selection overlay shown after ENTER.
 *
 * This is the one screen the brief asks for -- there is no navigation
 * beyond it (see README -> "No backend" for why there's nowhere to navigate
 * *to*: no player, no details page, no login).
 */
@Composable
fun HomeScreen(viewModel: HomeViewModel, modifier: Modifier = Modifier) {
    val listState = rememberLazyListState()

    LaunchedEffect(viewModel.focus.rowIndex) {
        listState.animateScrollToItem(viewModel.focus.rowIndex)
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column {
            Text(
                text = "Dummy TV App",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 24.dp, top = 20.dp, bottom = 12.dp),
            )
            LazyColumn(
                state = listState,
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                itemsIndexed(viewModel.sections, key = { _, section -> section.id }) { index, section ->
                    ContentRow(
                        section = section,
                        focusedColumn = if (index == viewModel.focus.rowIndex) viewModel.focus.columnIndex else null,
                        selectedId = viewModel.selected?.id,
                        onCardClick = { columnIndex -> viewModel.selectAt(index, columnIndex) },
                    )
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
            // all on a touch-only device with no keyboard/remote, since iOS
            // has no system back button (see PlatformBackHandler.ios.kt).
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss,
            ),
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
