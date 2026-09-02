package com.example.dummytvapp.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.dummytvapp.data.DummyContentRepository
import com.example.dummytvapp.model.Content
import com.example.dummytvapp.model.ContentSection

/**
 * Holds all mutable state for the home screen: the dummy content, which
 * card currently has "TV focus", and which card (if any) the user selected.
 *
 * This is a plain Kotlin class, not an `androidx.lifecycle.ViewModel`. A real
 * app juggling process death / navigation back-stacks would want the
 * lifecycle-aware version (Compose Multiplatform does now ship one -- see
 * the `org.jetbrains.androidx.lifecycle` artifacts referenced in the
 * Compose Multiplatform release notes) but that is unneeded machinery for a
 * single-screen POC. It is created with `remember { HomeViewModel() }` from
 * [com.example.dummytvapp.ui.App].
 */
class HomeViewModel {

    val sections: List<ContentSection> = DummyContentRepository.getSections()

    var focus by mutableStateOf(TvFocusState())
        private set

    /** Non-null while a card is "selected" (i.e. the user pressed ENTER on it). */
    var selected by mutableStateOf<Content?>(null)
        private set

    val focusedContent: Content
        get() = sections[focus.rowIndex].items[focus.columnIndex]

    fun move(direction: TvDirection) {
        // Ignore movement while the selection overlay is showing, exactly
        // like a real TV UI would not let you drive the grid behind a modal.
        if (selected != null) return

        focus = when (direction) {
            TvDirection.Right -> moveColumn(+1)
            TvDirection.Left -> moveColumn(-1)
            TvDirection.Down -> moveRow(+1)
            TvDirection.Up -> moveRow(-1)
        }
    }

    private fun moveColumn(delta: Int): TvFocusState {
        val row = sections[focus.rowIndex]
        val newColumn = (focus.columnIndex + delta).coerceIn(0, row.items.lastIndex)
        return focus.copy(columnIndex = newColumn)
    }

    private fun moveRow(delta: Int): TvFocusState {
        val newRow = (focus.rowIndex + delta).coerceIn(0, sections.lastIndex)
        val clampedColumn = focus.columnIndex.coerceIn(0, sections[newRow].items.lastIndex)
        return TvFocusState(rowIndex = newRow, columnIndex = clampedColumn)
    }

    /** ENTER: "select" whatever card currently has focus. */
    fun activate() {
        selected = focusedContent
    }

    /**
     * Touch/mouse equivalent of "move focus here, then press ENTER" in one
     * step. Wired from [com.example.dummytvapp.ui.ContentCard]'s `onClick`
     * so tapping a card works on Android/iOS/Web even with no physical
     * arrow keys - see README -> "Why not Modifier.focusable()?" for why
     * touch and remote/keyboard input are handled as two separate, deliberate
     * paths rather than trying to force touch through the same focus-move
     * API remote input uses.
     */
    fun selectAt(rowIndex: Int, columnIndex: Int) {
        if (selected != null) return
        focus = TvFocusState(rowIndex, columnIndex)
        activate()
    }

    /**
     * BACK/Escape: dismiss the selection overlay if one is showing.
     *
     * Returns `true` if it consumed the back press (so the overlay was
     * showing and is now closed), `false` if there was nothing to dismiss --
     * platform code (see [com.example.dummytvapp.platform.PlatformBackHandler])
     * uses that `false` case to decide whether BACK should instead exit the
     * app / pop a real navigation stack.
     */
    fun back(): Boolean {
        if (selected != null) {
            selected = null
            return true
        }
        return false
    }
}
