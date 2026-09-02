package com.example.dummytvapp.data

import com.example.dummytvapp.model.Content
import com.example.dummytvapp.model.ContentSection

/**
 * Static, in-memory "repository" for the dummy TV home screen.
 *
 * There is no backend, database, or network call anywhere in this class on
 * purpose (see the project README, section "No backend"). Everything is
 * generated with plain arithmetic from fixed inputs, so the exact same 25
 * rows x 12 items = 300 pieces of content are produced every single time,
 * on every platform (Android/iOS/Wasm all execute this same commonMain
 * code) -- there is no `Random`/`kotlin.random` anywhere in this file.
 */
object DummyContentRepository {

    // 25 row titles, roughly mirroring a real streaming home screen.
    private val rowTitles = listOf(
        "Featured",
        "Trending Now",
        "Popular Movies",
        "Top 10 Today",
        "Continue Watching",
        "New Releases",
        "Action",
        "Comedy",
        "Drama",
        "Sci-Fi & Fantasy",
        "Horror & Thriller",
        "Romance",
        "Documentaries",
        "Animated & Family",
        "Crime & Mystery",
        "Award Winners",
        "Critically Acclaimed",
        "Adventure",
        "Musicals",
        "War & History",
        "Sports",
        "Classics",
        "International Cinema",
        "Because You Watched...",
        "Recommended For You",
    )

    private const val ITEMS_PER_ROW = 12

    /** Number of horizontally-scrolling rows on the home screen (25). */
    val rowCount: Int get() = rowTitles.size

    /** Total number of dummy content items across every row (25 x 12 = 300). */
    val totalItemCount: Int get() = rowCount * ITEMS_PER_ROW

    /**
     * Builds all 25 [ContentSection]s. This is cheap (pure arithmetic over a
     * small fixed list), so it is safe to call directly from a Composable via
     * `remember { DummyContentRepository.getSections() }` without a
     * ViewModel/coroutine round-trip.
     */
    fun getSections(): List<ContentSection> {
        var nextId = 1
        return rowTitles.mapIndexed { rowIndex, rowTitle ->
            val items = (0 until ITEMS_PER_ROW).map { columnIndex ->
                val id = nextId++
                Content(
                    id = id,
                    title = "Movie ${id.toString().padStart(3, '0')}",
                    category = rowTitle,
                    // Deterministic "random-looking" year between 1990 and 2024.
                    year = 1990 + (id * 7 + rowIndex * 11 + columnIndex * 3) % 35,
                    paletteIndex = id,
                )
            }
            ContentSection(id = rowIndex, title = rowTitle, items = items)
        }
    }
}
