package com.example.dummytvapp.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.dummytvapp.data.DummyContentRepository
import com.example.dummytvapp.model.Content
import com.example.dummytvapp.model.ContentSection

/**
 * Holds the dummy content and which card (if any) the user selected.
 *
 * It deliberately does **not** hold the navigation position any more. That now
 * lives in `roku-focus-list`'s own state objects -- a `RokuColumnState` for
 * which row is current, and one `RokuFocusListState` per row for which card is
 * current within it. Giving every row its own state is what makes focus
 * *remembered*: leave a row at card 8, come back, and you are still on card 8,
 * instead of carrying one column index across every row the way the earlier
 * hand-rolled `TvFocusState` did.
 *
 * This is a plain Kotlin class, not an `androidx.lifecycle.ViewModel`: a real
 * app juggling process death would want the lifecycle-aware version, but that
 * is unneeded machinery for a single-screen POC. It is created with
 * `remember { HomeViewModel() }` from [com.example.dummytvapp.ui.App].
 */
class HomeViewModel {

    val sections: List<ContentSection> = DummyContentRepository.getSections()

    /** Non-null while a card is "selected" (i.e. the user pressed ENTER/OK on it). */
    var selected by mutableStateOf<Content?>(null)
        private set

    /**
     * ENTER/OK on the card at [rowIndex]/[columnIndex], or a tap on it.
     *
     * Coordinates come straight from `RokuLazyColumn`'s `onItemClicked`, so the
     * library's notion of what is focused and this app's notion of what was
     * selected cannot drift apart.
     */
    fun select(rowIndex: Int, columnIndex: Int) {
        if (selected != null) return
        selected = sections.getOrNull(rowIndex)?.items?.getOrNull(columnIndex)
    }

    /**
     * BACK/Menu/Escape: dismiss the selection overlay if one is showing.
     *
     * Returns `true` if it consumed the back press, `false` if there was nothing
     * to dismiss. That `false` matters on every platform: Android uses it to let
     * the system back gesture through, and on tvOS an unconsumed Menu press is
     * forwarded up the UIKit responder chain so the system can suspend the app,
     * which is what Apple's HIG requires at a root screen.
     */
    fun back(): Boolean {
        if (selected != null) {
            selected = null
            return true
        }
        return false
    }
}
