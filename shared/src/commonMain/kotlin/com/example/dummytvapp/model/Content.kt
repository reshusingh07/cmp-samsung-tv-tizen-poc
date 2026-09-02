package com.example.dummytvapp.model

/**
 * A single dummy piece of "content" (stands in for a movie/show poster tile).
 *
 * There is deliberately no [imageUrl]/network-backed poster here: the brief
 * for this POC requires it to run with no backend at all, on three very
 * different rendering pipelines (Android View/Compose, iOS UIKit/Compose,
 * and a browser canvas on a TV). Loading real images would mean pulling in
 * an image-loading library (e.g. Coil 3, which does support Compose
 * Multiplatform) and a network/caching story purely for cosmetics -- that's
 * unnecessary surface area for a POC whose only real question is "does the
 * shared UI + input architecture work on all four targets". `paletteIndex`
 * is used instead to deterministically pick a placeholder color per card.
 */
data class Content(
    val id: Int,
    val title: String,
    val category: String,
    val year: Int,
    val paletteIndex: Int,
)

/** A horizontally-scrolling row of [Content], e.g. "Action" or "Trending Now". */
data class ContentSection(
    val id: Int,
    val title: String,
    val items: List<Content>,
)
