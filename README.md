# Dummy TV App

A minimal streaming-app-shaped UI — 25 rows x 12 dummy "movie" cards, with
TV-style remote/keyboard navigation — built **once** in Kotlin + Compose
Multiplatform (CMP) and run on:

- **Android** (phone/tablet/Android TV)
- **iOS**
- **Samsung Smart TV**, via a Kotlin/Wasm web build wrapped as a Tizen Web
  App

There is no backend, no network calls, and no video playback — see
[Known simplifications](#known-simplifications). The point of this project
is the shared UI + input architecture, not the content.

> **New to this project?** Read
> [`docs/CMP_SAMSUNG_TV_GUIDE.md`](docs/CMP_SAMSUNG_TV_GUIDE.md) for a full,
> beginner-friendly walkthrough of how the Compose Multiplatform code
> becomes a Samsung TV app, how remote/keyboard navigation works, and how to
> repeat this setup in your own project.

---

## Table of contents

- [Status at a glance](#status-at-a-glance)
- [The most important caveat: Samsung TV compatibility](#the-most-important-caveat-samsung-tv-compatibility)
- [Architecture](#architecture)
- [Project structure](#project-structure)
- [Prerequisites](#prerequisites)
- [Getting started](#getting-started)
  - [Android](#android)
  - [iOS](#ios)
  - [Web / Kotlin-Wasm](#web--kotlin-wasm)
  - [Samsung TV (Tizen)](#samsung-tv-tizen)
- [TV remote / keyboard navigation](#tv-remote--keyboard-navigation)
  - [Design: explicit state, not Compose's built-in focus system](#design-explicit-state-not-composes-built-in-focus-system)
  - [The complete input flow](#the-complete-input-flow)
  - [Step by step, with the actual code](#step-by-step-with-the-actual-code)
  - [The Web/Wasm-specific wrinkle](#the-webwasm-specific-wrinkle)
  - [Verified vs. not verified](#verified-vs-not-verified)
- [Toolchain versions](#toolchain-versions)
- [Documentation](#documentation)
- [Known simplifications](#known-simplifications)
- [Verification status](#verification-status)
- [License](#license)

---

## Status at a glance

| Area | Status |
|---|---|
| Project structure, Gradle config, all source code | ✅ Written and building |
| Android — builds (`./gradlew :androidApp:assembleDebug`) | ✅ **Verified** — produces `androidApp-debug.apk` |
| Android — runs on a device/emulator | ⬜ Not verified (no device/emulator exercised) |
| iOS — builds and runs | ⬜ Not verified (no macOS/Xcode available in development) |
| Web/Wasm — builds (`./gradlew :shared:wasmJsBrowserDistribution`) | ✅ **Verified** |
| Web/Wasm — runs in a desktop browser | ✅ **Verified in real Chrome**, including full keyboard navigation (see [Documentation](#documentation)) |
| Tizen packaging, signing, install on a TV | ⬜ Not verified (no Tizen Studio / device / emulator available) |
| Samsung TV compatibility (real hardware) | ⬜ **Not tested on real hardware.** Researched and reasoned about only — see below. |

This project follows a "verify, don't assume" rule throughout: anything
above marked ✅ was actually built and/or run and observed; anything marked
⬜ is explicitly called out as unverified rather than assumed to work.

---

## The most important caveat: Samsung TV compatibility

**This app cannot run on 2024-or-earlier Samsung TVs at all.** That is not a
bug to fix — it is a hard requirement of Kotlin/Wasm + Compose Multiplatform
that no amount of app-level code can work around.

Compose Multiplatform's web target requires a browser engine with
**WebAssembly GC (WasmGC)** support, which shipped in Chromium 119.
Samsung publishes exactly which Chromium version each Tizen version
bundles:

| TV model year | Tizen version | Chromium version |
|---|---|---|
| 2025+ | Tizen 9.0+ | M120+ (WasmGC ✅) |
| 2024 | Tizen 8.0 | M108 (too old ❌) |
| 2023 and earlier | Tizen 7.0 and older | M94 and older (too old ❌) |

`tizen-app/config.xml` sets `required_version="9.0"` deliberately, so Tizen
refuses to install this app on hardware that cannot run it, rather than
installing something that would only fail silently on screen.

**What this means in practice:** only a 2025-or-newer Samsung TV, or the
Tizen 9.0+ emulator, can even attempt to run this app — and even that has
only been reasoned about from Samsung's own published specs, never
confirmed on real hardware or the emulator. See
[`docs/CMP_SAMSUNG_TV_GUIDE.md`](docs/CMP_SAMSUNG_TV_GUIDE.md#6-samsung-tv-execution-flow)
for the full sourced explanation.

---

## Architecture

```
                    Compose Multiplatform ("shared" module)
                                  |
             +--------------------+--------------------+
             |                    |                     |
        androidTarget        iosArm64 /            wasmJs (browser)
             |                iosSimulatorArm64          |
   +---------+---------+          |             shared/build/dist/wasmJs/
   |                   |          |             productionExecutable/
androidApp        (consumed        |             (index.html + shared.js + .wasm)
(real Android      directly by     |                      |
 application       iosApp's        |                      v
 module)           Xcode project) iosApp              tizen-app/
   |                   |         (Xcode project,      (config.xml + icon.png,
   v                   v          Swift shell)         wraps the copied dist)
Android app         iOS app                                  |
                                                               v
                                                         Samsung Smart TV
                                                    (Tizen 9.0+ / 2025+ models only)
```

For the full explanation of every arrow in this diagram — what Kotlin/Wasm
actually is, why WebAssembly is required, what a `.wgt` file is, and what
the TV executes at runtime — see
[`docs/CMP_SAMSUNG_TV_GUIDE.md`, Section 1](docs/CMP_SAMSUNG_TV_GUIDE.md#1-overall-architecture).

### Why not `Modifier.focusable()`?

Compose has a built-in 2D focus-traversal system, and it is deliberately
**not** used here for the row/column card grid. Two reasons:

1. **Uneven grids.** Compose's default focus search picks the "nearest"
   focusable node geometrically — it has no concept of "row 4, column 7" the
   way this app's `TvFocusState` does, and doesn't reliably keep a
   focused-but-scrolled-out-of-view row's item in view.
2. **Consistency across three different focus implementations.** Android's,
   iOS's, and the browser's default focus-traversal behaviors are not
   guaranteed to agree with each other in nested-lazy-list situations.

Instead, `TvFocusState` (`rowIndex`, `columnIndex`) is a plain, explicit,
framework-free piece of state that `HomeViewModel` moves directly, and every
`ContentRow`/`ContentCard` just reads whether its own coordinates match it.
See [TV remote / keyboard navigation](#tv-remote--keyboard-navigation) below
and [the full guide](docs/CMP_SAMSUNG_TV_GUIDE.md#8-focus-vs-selection) for
the complete story, including the Web/Wasm-specific focus fix.

---

## Project structure

| Source set | What lives here | Used by |
|---|---|---|
| `shared/src/commonMain` | Everything: models, dummy-data generator, all UI (`App`, `HomeScreen`, `ContentRow`, `ContentCard`), the view model, focus state, key-event mapping | Android, iOS, Web — zero UI duplication |
| `shared/src/androidMain` | `PlatformBackHandler.android.kt`, `PlatformInputBridge.android.kt`, `PlatformFocusBridge.android.kt` | Android only |
| `shared/src/iosMain` | `main.ios.kt` (Swift-callable entry point), iOS actuals of the platform files | iOS only |
| `shared/src/wasmJsMain` | `main.kt` (`ComposeViewport` entry point), `resources/index.html`, wasmJs actuals of the platform files | Web / Tizen |
| `androidApp/` | `MainActivity.kt` (one `setContent { App() }` call), manifest, theme, launcher icon | Real, installable Android app module |
| `iosApp/` | A plain Xcode project (`iosApp.xcodeproj`), `iOSApp.swift`, `ContentView.swift` | Real, installable iOS app; Xcode invokes Gradle itself via a build phase |
| `tizen-app/` | `config.xml` (Tizen's manifest), `icon.png`, plus a copy of the wasmJs build output | Tizen / Samsung TV wrapper — see `tizen-app/README-TIZEN.md` |
| `scripts/copy-web-dist-to-tizen.sh` | Builds the wasmJs distribution and copies it into `tizen-app/` | Web → Tizen step |
| `docs/CMP_SAMSUNG_TV_GUIDE.md` | Full developer guide to the CMP → Wasm → Tizen → Samsung TV pipeline | Onboarding / reference |

For the "what and why" of every file listed above, see
[`docs/CMP_SAMSUNG_TV_GUIDE.md`, Section 2](docs/CMP_SAMSUNG_TV_GUIDE.md#2-project-structure).

---

## Prerequisites

| Target | Requires |
|---|---|
| Android | JDK 17, Android SDK (compileSdk 37 / minSdk 24) |
| iOS | A Mac with Xcode installed |
| Web/Wasm | Nothing beyond the JDK — Gradle downloads Node.js/webpack itself |
| Samsung TV / Tizen | [Tizen Studio](https://developer.tizen.org/development/tizen-studio/download) + its TV extension, a Samsung developer account for a device-deployable certificate |

This project ships its own Gradle wrapper (`./gradlew` / `gradlew.bat`), so
no separate Gradle install is required for Android or Web/Wasm.

---

## Getting started

### Android

```bash
./gradlew :androidApp:assembleDebug
# APK: androidApp/build/outputs/apk/debug/androidApp-debug.apk
```

Or open the project root in Android Studio and run the `androidApp`
configuration on any device/emulator (phone, tablet, or Android TV — the
manifest declares both a `LAUNCHER` and a `LEANBACK_LAUNCHER` entry).

### iOS

Requires a Mac with Xcode.

```bash
open iosApp/iosApp.xcodeproj
```

Pick a simulator/device and hit Run. The project's "Run Script" build phase
automatically runs `./gradlew :shared:embedAndSignAppleFrameworkForXcode`
for you.

### Web / Kotlin-Wasm

Development server, with hot-reload:

```bash
./gradlew :shared:wasmJsBrowserDevelopmentRun
```

Production build:

```bash
./gradlew :shared:wasmJsBrowserDistribution
# output: shared/build/dist/wasmJs/productionExecutable/
```

You can serve that output with any static file server, e.g.:

```bash
npx serve shared/build/dist/wasmJs/productionExecutable
```

then open it in a current desktop browser (Chrome/Edge 119+, Firefox 120+,
Safari 18.2+ — older browsers lack the WebAssembly GC support Compose
Multiplatform's web target requires).

### Samsung TV (Tizen)

```bash
./scripts/copy-web-dist-to-tizen.sh
```

This runs the Web/Wasm production build above and copies its output into
`tizen-app/`, leaving `config.xml`/`icon.png` untouched. Packaging, signing,
and installing onto a TV require Tizen Studio and are **not** scriptable
from this repo alone — see
**[`tizen-app/README-TIZEN.md`](tizen-app/README-TIZEN.md)** for the exact
commands, and
**[`docs/CMP_SAMSUNG_TV_GUIDE.md`, Section 5](docs/CMP_SAMSUNG_TV_GUIDE.md#5-tizen-studio-the-full-workflow)**
for a step-by-step explanation of what each step does and why.

Per the compatibility caveat above, this will only succeed on a
**2025-or-newer** Samsung TV or the Tizen 9.0+ emulator —
`config.xml`'s `required_version="9.0"` refuses installation on anything
older.

---

## TV remote / keyboard navigation

A Samsung TV remote has no pointer and no touch screen — only a D-pad and
an OK/Enter button — so every card on screen must be reachable purely by
directional key presses, and the app must always know exactly one card is
"current." This section explains the actual implementation and the
complete flow from a key press to the screen updating.

### Design: explicit state, not Compose's built-in focus system

Compose ships a built-in 2D focus-traversal system
(`Modifier.focusable()` + arrow-key-driven `FocusManager` movement), but
this project **deliberately does not use it** for the card grid. Instead,
navigation position is a plain, framework-free piece of state:

```kotlin
// shared/src/commonMain/kotlin/com/example/dummytvapp/viewmodel/TvFocusState.kt
data class TvFocusState(
    val rowIndex: Int = 0,
    val columnIndex: Int = 0,
)

enum class TvDirection { Up, Down, Left, Right }
```

`HomeViewModel` moves this state directly in response to key presses, and
every `ContentCard` simply checks "does my (row, column) match this state?"
to decide whether to draw itself as focused. Why not the built-in system:
Compose's default focus search picks the *geometrically nearest* focusable
node, which has no concept of "row 4, column 7" the way `TvFocusState`
does, and doesn't reliably keep a focused item's row scrolled into view
when rows have independent horizontal scroll positions (a `LazyRow` nested
inside a `LazyColumn`, exactly this app's layout).

### The complete input flow

```
Key press (physical keyboard in Chrome, or a real Tizen remote)
        │
Browser / Tizen web engine turns it into a KeyboardEvent
        │
Compose Multiplatform's Web/Wasm layer delivers it to whatever
currently holds real focus
        │
App.kt's Modifier.onKeyEvent { viewModel.handleKeyEvent(event) }
        │
TvKeyHandling.kt maps the Key to an action
        │
HomeViewModel.move(direction) / .activate() / .back()
        │
HomeViewModel.focus (a TvFocusState) is reassigned
        │
Compose recomposition: HomeScreen → ContentRow → ContentCard re-read `focus`
        │
ContentCard draws its animated border; ContentRow/HomeScreen auto-scroll
        ▼
User sees the newly-focused card, scrolled into view
```

Every one of those steps is ordinary `commonMain` Kotlin, so it runs
identically on Android, iOS, and Web/Wasm — nothing above is
platform-specific.

### Step by step, with the actual code

**1. Where key events are captured — `App.kt`.** One `Modifier.focusable()`
sits on the whole screen (not per-card), with a single `FocusRequester`.
This is the *only* place Compose's own focus API is used at all:

```kotlin
HomeScreen(
    viewModel = viewModel,
    modifier = Modifier
        .fillMaxSize()
        .focusRequester(rootFocusRequester)
        .focusable()
        .onKeyEvent { event -> viewModel.handleKeyEvent(event) },
)
```

**2. Mapping keys to actions — `TvKeyHandling.kt`:**

```kotlin
internal fun HomeViewModel.handleKeyEvent(event: KeyEvent): Boolean {
    if (event.type != KeyEventType.KeyDown) return false
    return when (event.key) {
        Key.DirectionLeft -> { move(TvDirection.Left); true }
        Key.DirectionRight -> { move(TvDirection.Right); true }
        Key.DirectionUp -> { move(TvDirection.Up); true }
        Key.DirectionDown -> { move(TvDirection.Down); true }
        Key.Enter, Key.NumPadEnter, Key.DirectionCenter -> { activate(); true }
        Key.Back, Key.Escape -> back()
        else -> false
    }
}
```

`Key.DirectionLeft/Right/Up/Down` and `Key.Enter` correspond to the exact
same DOM key codes a real Tizen remote's D-pad and OK button send
(`ArrowLeft=37, ArrowUp=38, ArrowRight=39, ArrowDown=40, Enter=13`, per
Samsung's Remote Control docs) — which is why pressing physical arrow keys
in a desktop browser exercises this same code path.

**3. Moving focus, with boundary handling — `HomeViewModel.kt`:**

```kotlin
fun move(direction: TvDirection) {
    if (selected != null) return   // don't drive the grid behind the "selected" overlay
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

fun activate() {
    selected = focusedContent   // ENTER/OK
}
```

`coerceIn(0, ...)` is what makes the boundaries behave correctly:

- **First/last card in a row:** pressing Left on column 0, or Right on the
  last column, leaves `columnIndex` exactly where it was — no wraparound,
  no crash.
- **First/last row:** the same clamp on `rowIndex`.
- **Moving between rows of different lengths:** `moveRow` re-clamps the
  *existing* column into the new row's valid range, so moving down from
  column 9 of a 12-item row into a shorter row lands on that row's last
  item instead of an out-of-bounds index.

**4. Displaying which card is focused — `HomeScreen.kt` → `ContentRow.kt` →
`ContentCard.kt`.** Each row is told whether it's the currently-focused row
(and if so, which column):

```kotlin
// HomeScreen.kt
ContentRow(
    section = section,
    focusedColumn = if (index == viewModel.focus.rowIndex) viewModel.focus.columnIndex else null,
    ...
)
```

Each card just compares its own index:

```kotlin
// ContentRow.kt
ContentCard(
    content = item,
    isFocused = focusedColumn == index,
    ...
)
```

```kotlin
// ContentCard.kt
val borderWidth by animateDpAsState(if (isFocused) 4.dp else 0.dp)
...
.border(width = borderWidth, color = ..., shape = RoundedCornerShape(8.dp))
```

**5. Keeping focus on screen.** Two separate `LaunchedEffect`s auto-scroll
the vertical row list and the focused row's own horizontal list whenever
`focus` changes, so the user never loses sight of the focused card:

```kotlin
// HomeScreen.kt — vertical
LaunchedEffect(viewModel.focus.rowIndex) {
    listState.animateScrollToItem(viewModel.focus.rowIndex)
}

// ContentRow.kt — horizontal (only the focused row gets a non-null focusedColumn)
LaunchedEffect(focusedColumn) {
    if (focusedColumn != null) listState.animateScrollToItem(focusedColumn)
}
```

### The Web/Wasm-specific wrinkle

Everything above runs the same on all three platforms — but on Web/Wasm,
getting a `KeyboardEvent` to reach `App.kt`'s `onKeyEvent` at all turned out
to need one extra piece. Compose Multiplatform attaches its keyboard
listeners directly to the `<canvas>` element it renders into, and that
canvas must have real browser focus before any key event reaches it.
`PlatformFocusBridge.wasmJs.kt` grabs that focus automatically when the app
starts (no click needed) — see
[`docs/CMP_SAMSUNG_TV_GUIDE.md`, Section 7](docs/CMP_SAMSUNG_TV_GUIDE.md#7-keyboard--tv-remote-navigation)
and [Section 12](docs/CMP_SAMSUNG_TV_GUIDE.md#12-common-problems) for the
full story of why this was needed and how it was diagnosed.

The one genuinely Tizen-specific input code in the whole project handles
the remote's proprietary Back button (DOM keycode `10009`, which no desktop
browser produces), independently of the standard key handling above:

```kotlin
// shared/src/wasmJsMain/kotlin/com/example/dummytvapp/platform/PlatformInputBridge.wasmJs.kt
private const val TIZEN_REMOTE_BACK_KEYCODE = 10009

@Composable
actual fun InstallPlatformInputBridge(onBack: () -> Unit) {
    DisposableEffect(Unit) {
        val listener: (Event) -> Unit = { event ->
            val keyboardEvent = event as? KeyboardEvent
            if (keyboardEvent != null && keyboardEvent.keyCode == TIZEN_REMOTE_BACK_KEYCODE) {
                onBack()
            }
        }
        window.addEventListener("keydown", listener)
        onDispose { window.removeEventListener("keydown", listener) }
    }
}
```

> ⚠️ This keycode is documented by Samsung, but has only been exercised by
> simulating the keycode in a test script — **not verified against a real
> Tizen remote.**

### Verified vs. not verified

- **Verified, in real Chrome:** every step above — Left/Right/Up/Down,
  first/last card and row boundaries, ENTER selecting a card, BACK/Escape
  dismissing the selection overlay, auto-scroll keeping focus in view — all
  driven with real keyboard input and observed directly.
- **Not verified:** any of this on a real Samsung TV or the Tizen emulator.
  Chrome and Tizen's web engine are both Chromium-based, but that is not
  the same as testing on the device itself.

---

## Toolchain versions

| Component | Version used |
|---|---|
| Kotlin | 2.4.10 |
| Compose Multiplatform | 1.12.0 |
| Gradle (via `./gradlew`) | 9.3.1 |
| Android Gradle Plugin | 9.1.0 |
| Kotlin/Wasm | Beta |
| Minimum Android version | API 24 (Android 7.0) |
| Minimum iOS version | iOS 14 |
| Minimum browser (desktop) | Chrome/Edge 119+, Firefox 120+, Safari 18.2+ |
| Minimum Samsung TV | 2025 models / Tizen 9.0 |

**Note on Android Gradle Plugin 9:** since AGP 9.0, Kotlin compilation for
Android is built into AGP itself, so `androidApp/build.gradle.kts` does
**not** apply the `org.jetbrains.kotlin.android` plugin. For the same
reason, `shared/build.gradle.kts` applies
`com.android.kotlin.multiplatform.library` instead of `com.android.library`
— AGP 9 does not allow combining `org.jetbrains.kotlin.multiplatform` with
`com.android.library` in the same module.

If you're reading this significantly later, re-check
[kotlinlang.org/docs/releases.html](https://kotlinlang.org/docs/releases.html)
and the
[Compose Multiplatform releases page](https://github.com/JetBrains/compose-multiplatform/releases)
before trusting these numbers — Kotlin/Wasm is still Beta and could change.

---

## Documentation

| Document | Covers |
|---|---|
| [`docs/CMP_SAMSUNG_TV_GUIDE.md`](docs/CMP_SAMSUNG_TV_GUIDE.md) | The complete CMP → Kotlin/Wasm → Tizen → Samsung TV pipeline, project structure, Gradle configuration, remote/keyboard navigation internals, focus vs. selection, common problems, and a step-by-step guide to repeating this setup in another CMP project |
| [`tizen-app/README-TIZEN.md`](tizen-app/README-TIZEN.md) | The detailed Tizen packaging/signing/install walkthrough |
| This file | Project overview, setup, and status |

---

## Known simplifications

Deliberate simplifications for this proof of concept — not bugs:

- **No real poster images.** Cards render a small fixed color palette
  instead of loading network images, to keep the "no backend" requirement
  honest. See `Content.kt`'s doc comment.
- **Selecting a card shows a text overlay, not a player/details screen.**
  There is intentionally nowhere further to navigate to — no video
  playback, no details page, no login.
- **The Android launcher icon and Tizen `icon.png` are simple
  placeholders**, not real app art.
- **`minSdk = 24`** means the adaptive-icon-only launcher icon (API 26+)
  won't resolve on API 24-25 devices specifically.
- **`tizen-app/config.xml`'s author/package ID is a placeholder**
  (`XXXXXXXXXX`) — it must be replaced with your real Tizen certificate
  profile's ID before packaging (see `tizen-app/README-TIZEN.md`).

---

## Verification status

```
[x] Android builds successfully           - VERIFIED (./gradlew :androidApp:assembleDebug)
[ ] Android home screen works on a device  - NOT VERIFIED (no device/emulator exercised)
[ ] iOS target builds successfully         - NOT VERIFIED (no macOS/Xcode available)
[ ] iOS home screen works                  - NOT VERIFIED
[x] wasmJs builds successfully             - VERIFIED
[x] Web app opens in a desktop browser     - VERIFIED (real Chrome)
[x] 25 rows render correctly               - VERIFIED (real Chrome)
[x] Horizontal scrolling works             - VERIFIED (real Chrome)
[x] Vertical scrolling works               - VERIFIED (real Chrome)
[x] Focus navigation works (Left/Right/Up/Down + boundaries) - VERIFIED (real Chrome)
[x] ENTER selection works                  - VERIFIED (real Chrome)
[x] BACK behavior is handled               - VERIFIED (real Chrome; Tizen back-keycode path NOT verified on a real remote)
[ ] Tizen project packages correctly       - NOT VERIFIED (no Tizen Studio available)
[ ] Tizen app launches successfully        - NOT VERIFIED (no Tizen device/emulator available)
[x] Samsung TV compatibility explicitly identified as unverified, with reasoning:
      2025+ TVs (Tizen 9.0+) should work; 2024-and-earlier TVs cannot run
      this app at all (missing WasmGC support in their bundled Chromium).
      See "The most important caveat" above for sources.
```

"Verified (real Chrome)" means driven and observed in an actual Chrome
browser during development — not merely assumed to work because the code
compiles. It explicitly does **not** mean verified on a real Samsung TV or
the Tizen emulator; see
[`docs/CMP_SAMSUNG_TV_GUIDE.md`](docs/CMP_SAMSUNG_TV_GUIDE.md) for exactly
what that distinction means for this project.

---

## License

No license file is currently included in this repository.
