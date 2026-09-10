# Compose Multiplatform on Samsung Smart TV — Developer Guide

This document explains how this project (`DummyTvApp`) takes one Kotlin +
Compose Multiplatform (CMP) codebase and runs it on a Samsung Smart TV, using
Kotlin/Wasm and a Tizen Web App wrapper.

It is written for a developer who has **never touched CMP or Tizen before**.
Every technical term is explained the first time it appears. Every file name,
command, and code snippet below is copied from the actual project — nothing
is invented. Where something has **not** been tested for real (e.g. on an
actual Samsung TV), this document says so explicitly instead of guessing.

> **Read this before anything else:** this app has only ever been run and
> tested in a **desktop Chrome browser**, never on a real Samsung TV or the
> Tizen emulator. Chrome results are strong evidence, not proof. Section 5
> and Section 12 explain exactly what is and isn't verified.

---

## 1. Overall architecture

**What CMP is doing.** Compose Multiplatform (CMP) is a UI framework that
lets you write one Kotlin UI (using `@Composable` functions) and run it on
several platforms — Android, iOS, desktop, and web — without rewriting the
UI per platform. In this project, `App()`, `HomeScreen()`, `ContentRow()`,
and `ContentCard()` are written **once**, in `shared/src/commonMain`, and
every platform calls the exact same `App()` function.

**What Kotlin/Wasm is doing.** Kotlin/Wasm is a Kotlin compiler backend
(a compiler mode) that turns Kotlin code into **WebAssembly** instead of
into JVM bytecode (like the Android backend does) or native machine code
(like the iOS backend does). It's how the *same* Kotlin UI code becomes
something a web browser can execute.

**Why WebAssembly is required.** WebAssembly (Wasm) is a binary format that
lets code compiled from languages like Kotlin, C++, or Rust run inside a web
engine (a browser, or — in this project's case — the browser built into a
Samsung TV), at speeds much closer to native code than plain JavaScript.
Compose Multiplatform's renderer needs this speed to draw a full UI (text,
shapes, animations) every frame; it isn't a "nice to have," it's a hard
requirement of Compose Multiplatform's web target (more on this in
Section 5 and Section 13).

**Why HTML/JavaScript are present even though the app is written in
Kotlin.** A browser (or a TV's web engine) cannot load a `.wasm` file on its
own — it needs an HTML page to open, and a small amount of JavaScript
"glue code" to load and start the `.wasm` binary and connect it to the page
(the DOM, the keyboard, the mouse). The Kotlin/Wasm compiler generates that
glue code for you; you never hand-write it. In this project that generated
file is called `shared.js`.

**What Tizen is doing.** Tizen is Samsung's own operating system for its
Smart TVs (and some other devices). A "Tizen Web App" is simply a **web
page** (HTML/CSS/JS/Wasm) that Tizen runs full-screen, using the same web
engine technology as a browser, but packaged and installed like a native
app.

**What Tizen Studio is doing.** Tizen Studio is Samsung's official desktop
IDE/toolset for building, signing, packaging, and deploying Tizen apps
(including Web Apps) to a real TV or the Tizen emulator. This project does
not use Tizen Studio's IDE features for writing code — all the code is
plain Kotlin/CMP — but Tizen Studio (or its command-line tools) is required
for the packaging/signing/install steps in Section 5, because those steps
need Samsung's own tooling.

**What the `.wgt` file is.** A `.wgt` ("widget") file is Tizen's installable
package format — literally a zip file containing your web app's HTML/JS/
Wasm/assets plus `config.xml` (see below), signed with a certificate. It's
the Tizen equivalent of an Android `.apk` or an iOS `.ipa`.

**Why the `.wgt` needs to be signed.** Tizen (like Android and iOS) refuses
to install unsigned apps. Signing proves who built the app and lets Tizen
enforce permissions. For a real Samsung TV, this requires a certificate
tied to a Samsung developer account (Section 5 explains this step by step).

### The complete flow

```
Kotlin source code (shared/src/commonMain)
        │  written once: App(), HomeScreen(), ContentRow(), ContentCard(), ...
        ▼
Compose Multiplatform compiler
        │  compiles the SAME commonMain code differently per target
        ▼
   ┌────────────┬─────────────┬──────────────┐
   ▼            ▼             ▼
Android      iOS            Kotlin/Wasm (wasmJs target)
(JVM          (native            │
 bytecode)     machine code)     ▼
                              HTML + JavaScript + WebAssembly
                              (shared/build/dist/wasmJs/productionExecutable/)
                                  │
                                  ▼
                              Copied into tizen-app/
                              (config.xml + icon.png + the files above)
                                  │
                                  ▼
                              Packaged + signed → DummyTvApp.wgt
                                  │
                                  ▼
                              Installed on a Samsung Smart TV
                              (Tizen OS runs it as a Tizen Web App)
```

**What the Samsung TV actually executes**, at the very bottom of that chain,
is: Tizen OS opens `index.html` in its built-in web engine, which downloads
`shared.js`, which downloads and starts the `.wasm` binary, which *is* your
compiled Kotlin/Compose UI, running frame by frame, drawing onto an HTML
`<canvas>` element.

---

## 2. Project structure

| Path | What's in it | Why it exists | Used by |
|---|---|---|---|
| `shared/src/commonMain` | Almost the entire app: `App.kt`, `HomeScreen.kt`, `ContentRow.kt`, `ContentCard.kt`, the `HomeViewModel`, `TvFocusState`, `TvKeyHandling.kt`, the `Content`/`ContentSection` models, `DummyContentRepository` | This is the "write once" code CMP is for. Zero UI is duplicated per platform. | Android, iOS, Web — all three call the same `App()` |
| `shared/src/androidMain` | `PlatformBackHandler.android.kt`, `PlatformInputBridge.android.kt`, `PlatformFocusBridge.android.kt` | Small per-platform pieces (`expect`/`actual` "actuals") for things Android needs that other platforms don't (or vice versa) | Android only |
| `shared/src/appleMain` | `MainViewController.kt` (the Swift-callable entry point), the Apple actuals of the same three platform files | Same idea as above, shared by iOS and tvOS (see `docs/CMP_TVOS_GUIDE.md`) | iOS + tvOS |
| `shared/src/wasmJsMain` | `main.kt` (the browser entry point), `resources/index.html`, and the wasmJs actuals of the three platform files | The web-specific code: how the app boots in a browser, and the one genuinely Tizen-specific input quirk (Section 7) | Web / Tizen |
| `tizen-app/` | `config.xml`, `icon.png`, and (after a build) a copy of the compiled web output | The Tizen Web App project itself — see Section 4 | Tizen / Samsung TV only |
| `tizen-app/config.xml` | Tizen's app manifest (a W3C "widget" format) | Tells Tizen the app's id, name, icon, minimum Tizen version, and settings | Tizen Studio / the TV, at install time |
| `shared/src/wasmJsMain/resources/index.html` | The **source** HTML page, before any Kotlin/Wasm build has run | The template the Kotlin/Wasm Gradle plugin copies into the build output and injects `<script src="shared.js">` into | Web / Tizen |
| `shared/build/dist/wasmJs/productionExecutable/` | **Generated** output: a copy of `index.html`, plus `shared.js` and one or more `.wasm` files | This is the actual thing a browser (or Tizen) loads. It's regenerated every time you run the build command in Section 3 — never edit it by hand. | Web / Tizen |
| `androidApp/` | `MainActivity.kt` (one `setContent { App() }` call), `AndroidManifest.xml`, launcher icon resources | The real, installable Android app. Kept as its own Gradle module so `shared` stays a pure library. | Android only |
| `iosApp/` | A plain Xcode project (`iosApp.xcodeproj`), `iOSApp.swift`, `ContentView.swift` | The real, installable iOS app. Xcode invokes Gradle itself via a build phase to produce the Kotlin framework. | iOS only |
| Root `build.gradle.kts`, `settings.gradle.kts`, `gradle/libs.versions.toml` | Gradle project setup: which plugins exist, which modules exist, and pinned dependency versions | Standard Gradle/Kotlin Multiplatform project wiring | The whole build |
| `shared/build.gradle.kts` | Declares the three targets (`android`, `iosArm64`/`iosSimulatorArm64`, `wasmJs`) and their dependencies | This is where "Web/Wasm support" is actually turned on — see Section 3 | The whole build |
| `scripts/copy-web-dist-to-tizen.sh` | A shell script | Automates "build the web output, then copy it into `tizen-app/`" (Section 4) | Web → Tizen step |

A quick mental model: **`shared/src/wasmJsMain` produces a normal website.
`tizen-app/` is a thin wrapper around that website that makes Tizen willing
to install it as an app.** Nothing in `tizen-app/` is Kotlin, and nothing in
`shared` is Tizen-specific, except one small file described in Section 7.

---

## 3. CMP → Web/Wasm

This is the Gradle configuration that turns on the web target, from
`shared/build.gradle.kts`:

```kotlin
@OptIn(ExperimentalWasmDsl::class)
wasmJs {
    outputModuleName = "shared"
    browser {
        commonWebpackConfig {
            outputFileName = "shared.js"
        }
    }
    binaries.executable()
}
```

What each part means:

- **`wasmJs { ... }`** — declares a new compile target: "also compile this
  module to Kotlin/Wasm, aimed at a browser." `@OptIn(ExperimentalWasmDsl::class)`
  is required because, as of this project's Kotlin version (2.4.10),
  Kotlin/Wasm itself is still **Beta** — an official label meaning the API
  can still change before it's finalized.
- **`outputModuleName = "shared"`** — the base name used for the generated
  JavaScript module.
- **`browser { ... }`** — says the output is meant to run in a browser (as
  opposed to, say, Node.js).
- **`commonWebpackConfig { outputFileName = "shared.js" }`** — Kotlin/Wasm
  uses [webpack](https://webpack.js.org/) (a JavaScript bundler) under the
  hood to produce the final JavaScript file; this line just names that file
  `shared.js` instead of a default name.
- **`binaries.executable()`** — tells the compiler "this should produce a
  runnable application," not just a library other Kotlin code can depend on.

The build command:

```bash
./gradlew :shared:wasmJsBrowserDistribution
```

This produces the folder `shared/build/dist/wasmJs/productionExecutable/`,
containing:

| File | What it is |
|---|---|
| `index.html` | Copied from `shared/src/wasmJsMain/resources/index.html`, with `<script src="shared.js">` present |
| `shared.js` | The JavaScript "glue" that loads the `.wasm` binary and wires it to the browser's DOM, keyboard, mouse, etc. |
| One or more `.wasm` files | Your compiled Kotlin/Compose code, plus Skia (the graphics engine Compose uses), as WebAssembly binaries |
| `composeResources/` | Any Compose Multiplatform resource files the app declares (this project doesn't currently use any) |

All four are required together — `index.html` alone does nothing without
`shared.js`, and `shared.js` does nothing without the `.wasm` files next to
it. **This whole step has no Tizen-specific content.** The exact same output
would run if you uploaded it to any normal web host and opened it in a
desktop browser — Tizen only enters the picture in the next section.

For local development (not production), there's also:

```bash
./gradlew :shared:wasmJsBrowserDevelopmentRun
```

which starts a local dev server with hot-reload, instead of producing the
static files above.

---

## 4. Web output → Tizen

Once you have the folder from Section 3, `scripts/copy-web-dist-to-tizen.sh`
does two things:

```bash
./gradlew :shared:wasmJsBrowserDistribution   # (re-runs the build above)
# then: deletes everything in tizen-app/ except config.xml and icon.png,
# and copies shared/build/dist/wasmJs/productionExecutable/* into tizen-app/
```

You can also do the copy by hand:

```bash
cp -r shared/build/dist/wasmJs/productionExecutable/* tizen-app/
```

**Why a Tizen wrapper is needed, instead of just installing the raw web
output on the TV.** A Samsung TV doesn't have a generic "open this website"
installer — Tizen needs to know an app's identity (a unique id), its name,
its icon, which Tizen versions it supports, and it needs the whole package
signed, before it will install anything at all. `config.xml` is exactly
that missing information. Wrapping the web output with `config.xml` is what
turns "a folder of HTML/JS/Wasm" into "a Tizen Web App Tizen is willing to
install."

Roles of each piece inside `tizen-app/` after the copy step:

| File | Role |
|---|---|
| `index.html` | The page Tizen opens when the app launches — copied in from the CMP build, unmodified |
| `shared.js` / `.wasm` files | Your compiled app — copied in from the CMP build, unmodified |
| `config.xml` | Tizen-specific: app id, name, icon reference, minimum Tizen version, screen settings. **Not** touched by the copy script. |
| `icon.png` | The app's icon shown in Tizen's app list. **Not** touched by the copy script. |

Here is the project's actual `tizen-app/config.xml`:

```xml
<widget xmlns="http://www.w3.org/ns/widgets"
        xmlns:tizen="http://tizen.org/ns/widgets"
        id="http://example.com/DummyTvApp"
        version="1.0.0"
        viewmodes="maximized">

    <tizen:application id="XXXXXXXXXX.DummyTvApp"
                        package="XXXXXXXXXX"
                        required_version="9.0"/>

    <tizen:profile name="tv"/>

    <icon src="icon.png"/>
    <name>Dummy TV App</name>
    <content src="index.html"/>

    <tizen:setting screen-orientation="landscape"
                   context-menu="disable"
                   background-support="disable"
                   encryption="disable"
                   install-location="auto"/>
</widget>
```

Two things worth calling out:

- **`id="XXXXXXXXXX.DummyTvApp"` / `package="XXXXXXXXXX"` are placeholders.**
  The 10-character prefix must match your own Tizen/Samsung certificate
  profile (Section 5, step "Create a Samsung certificate"). Tizen Studio can
  rewrite this for you automatically when you package the app through its
  UI.
- **`required_version="9.0"` is deliberate, not a placeholder** — see
  Section 6 and Section 13 for exactly why.

---

## 5. Tizen Studio: the full workflow

> ⚠️ **Not verified in this project.** Nobody working on this project has
> had access to Tizen Studio, a Tizen emulator, or a real Samsung TV. Every
> step below is the standard, documented Tizen workflow (also written down
> in `tizen-app/README-TIZEN.md`), not something that has been personally
> run start-to-finish here. Treat the commands as a strong starting point,
> and confirm current flags with `tizen --help` / `tizen package --help` /
> `tizen install --help` on your own machine.

| Step | What it does |
|---|---|
| **1. Install Tizen Studio** | Samsung's desktop IDE/CLI toolkit for building and packaging Tizen apps. Download from [Tizen Studio's site](https://developer.tizen.org/development/tizen-studio/download). |
| **2. Install the TV extension** | Tizen Studio ships with a Package Manager; under Extension SDK, install the **TV** extension. This adds TV-specific tools (TV emulator images, TV device profile) that aren't installed by default. |
| **3. Enable Developer Mode on the Samsung TV** | On the TV itself: Apps → search/enter a specific developer-mode code (Samsung's TV menus, not this project) → toggle Developer Mode on, and enter your PC's IP address so the TV knows where to accept a connection from. |
| **4. Connect the PC and TV** | Both need to be on the same network. Tizen's `sdb` (Smart Development Bridge — Tizen's device-connection tool, conceptually like Android's `adb`) is used to connect: `sdb connect <tv-ip-address>`. |
| **5. Create a Samsung certificate** | Tools → **Certificate Manager** in Tizen Studio → create a new Tizen/Samsung certificate profile. A real Samsung account is required for a certificate that can install onto **physical** hardware; Tizen Studio's default "Tizen" profile only works for the emulator. |
| **6. Configure the certificate in the project** | Either let Tizen Studio rewrite `config.xml`'s package id automatically when you package through its UI, or open the config.xml editor's "Package" tab and pick your profile manually. |
| **7. Import the Tizen project** | File → Import → Tizen → "Tizen Project," and point it at the `tizen-app/` folder. |
| **8. Build the Tizen application** | `tizen build-web -- tizen-app` — validates and stages the web app, producing `tizen-app/.buildResult`. |
| **9. Sign the application** | Happens automatically as part of packaging (next step), using the certificate profile from step 5. |
| **10. Generate the `.wgt`** | `tizen package -t wgt -s <your-certificate-profile> -- tizen-app/.buildResult` — packages the staged output into a signed `.wgt` file. |
| **11. Install/run on the TV** | `sdb devices` (list connected/emulated TVs) → `tizen install -n DummyTvApp.wgt -t <device-id>` → `tizen run -p XXXXXXXXXX.DummyTvApp -t <device-id>`. |

Tizen Studio's GUI wraps steps 8–11 into a single "Run As → Tizen Web
Application" button, which is the easier path for interactive development.

---

## 6. Samsung TV execution flow

What happens after you launch the installed app on the TV, in simple terms:

```
Samsung TV powered on
        │
Tizen OS (the TV's operating system)
        │  user selects the app from the Tizen app list
        ▼
Tizen launches the Tizen Web App
        │  opens the app's web engine (a Chromium-based browser, built into Tizen)
        ▼
index.html loads
        │  <script src="shared.js"> starts downloading and running
        ▼
JavaScript (shared.js) starts and loads the .wasm binary
        ▼
Kotlin/Wasm application starts running
        │  this IS your compiled Kotlin code
        ▼
Compose Multiplatform draws the UI onto an HTML <canvas>
        ▼
Our App() → HomeScreen() composables run
        ▼
Movie rows and cards appear on screen
```

**The critical caveat (see also Section 13):** Compose Multiplatform's web
target needs a browser engine with **WebAssembly GC** (WasmGC) support — a
specific WebAssembly feature that lets Wasm code work with a garbage
collector, which Kotlin/Wasm's generated code relies on. WasmGC shipped
enabled-by-default in Chromium 119. Samsung publishes exactly which
Chromium version each Tizen version bundles:

| TV model year | Tizen version | Chromium version |
|---|---|---|
| 2026 | Tizen 10.0 | M130 |
| **2025** | **Tizen 9.0** | **M120** |
| **2024** | **Tizen 8.0** | **M108** |
| 2023 | Tizen 7.0 | M94 |
| 2022 and older | Tizen 6.5 and older | M85 and older |

**2025 is the first Samsung TV model year new enough to run this app at
all.** Every 2024-or-earlier Samsung TV's bundled Chromium is too old to
execute the WebAssembly instructions Compose Multiplatform's output
requires — this isn't a slowness or polish issue, the TV's browser engine
simply cannot run the code, at all. That's exactly why `config.xml` sets
`required_version="9.0"`: it tells Tizen to refuse installation on anything
older, rather than installing something that can only fail silently. See
`README.md`'s "Tizen/Samsung TV compatibility" section for the full sourced
write-up.

---

## 7. Keyboard / TV remote navigation

**Why keyboard navigation is needed.** A Samsung TV remote has no mouse
pointer and no touch screen — only directional (D-pad) buttons and an
OK/Enter button. Every interactive element on screen must be reachable by
pressing Left/Right/Up/Down and confirmed with Enter.

**Why mouse/touch navigation is different.** A mouse or touch interaction
tells you exactly *which* element the user is interacting with (via
coordinates). Remote/keyboard navigation instead has to track *one current
position* ("focus") and move it explicitly in response to a direction —
there's no coordinate to hit-test against.

**How Chrome keyboard input simulates the TV remote during development.**
Per Samsung's Remote Control developer docs, the TV remote's arrow keys and
Enter button are delivered to a Tizen Web App as ordinary browser
`KeyboardEvent`s, with the exact same DOM `keyCode` values a PC keyboard's
arrow keys and Enter key produce (`ArrowLeft=37, ArrowUp=38, ArrowRight=39,
ArrowDown=40, Enter=13`). That means pressing the physical arrow keys on a
PC keyboard in Chrome, while developing, exercises the **exact same** code
path a real remote press would — no special "TV remote emulator" is needed
for those five keys. (The one Tizen remote key that is *not* a standard PC
keyboard key — the remote's Back button, DOM keycode `10009` — is handled
separately; see below.)

### The actual code

All of this lives in `shared/src/commonMain`, so it's identical on Android,
iOS, and Web.

**1. Where key presses are captured — `App.kt`:**

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

One `Modifier.focusable()` sits on the whole screen (not per-card — see
Section 8), and every key event that reaches it is forwarded to
`viewModel.handleKeyEvent(event)`.

**2. How Arrow Left/Right/Up/Down and Enter/OK are mapped —
`TvKeyHandling.kt`:**

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

**3. How focus position is tracked — `TvFocusState.kt`:**

```kotlin
data class TvFocusState(
    val rowIndex: Int = 0,
    val columnIndex: Int = 0,
)

enum class TvDirection { Up, Down, Left, Right }
```

This is a plain Kotlin data class — not a Compose focus API. `rowIndex` is
which horizontal row is active, `columnIndex` is which card within that row.

**4. How movement (and boundaries) work — `HomeViewModel.kt`:**

```kotlin
fun move(direction: TvDirection) {
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
```

- **First/last card in a row:** `coerceIn(0, row.items.lastIndex)` simply
  refuses to move past 0 or the last index — pressing Left on the first
  card, or Right on the last card, leaves focus exactly where it was.
- **First/last row:** the same `coerceIn` pattern on `sections.lastIndex`.
- **Moving between rows preserves column when possible:** `moveRow` clamps
  the *existing* column into the new row's valid range, so moving down from
  column 5 of a 12-item row into a shorter row lands on that row's last
  item instead of crashing or resetting to column 0.
- **While the selection overlay is showing** (`selected != null`), `move()`
  does nothing at all — exactly like a real TV UI not letting you drive the
  grid behind a modal.

**5. Enter/OK — same file:**

```kotlin
fun activate() {
    selected = focusedContent
}
```

**6. How the currently focused card is displayed — `ContentRow.kt` and
`ContentCard.kt`:**

`HomeScreen.kt` passes each row whether it currently holds focus:

```kotlin
ContentRow(
    section = section,
    focusedColumn = if (index == viewModel.focus.rowIndex) viewModel.focus.columnIndex else null,
    ...
)
```

Inside `ContentRow`, each card just checks if its own index matches:

```kotlin
ContentCard(
    content = item,
    isFocused = focusedColumn == index,
    ...
)
```

`ContentCard` renders `isFocused` as an animated border:

```kotlin
val borderWidth by animateDpAsState(if (isFocused) 4.dp else 0.dp)
...
.border(
    width = borderWidth,
    color = if (isSelected) MaterialTheme.colorScheme.tertiary
            else MaterialTheme.colorScheme.primary,
    shape = RoundedCornerShape(8.dp),
)
```

**7. Keeping the focused card/row scrolled into view** — two separate
`LaunchedEffect`s auto-scroll the vertical list (`HomeScreen.kt`) and the
focused row's horizontal list (`ContentRow.kt`) whenever focus moves:

```kotlin
// HomeScreen.kt — vertical
LaunchedEffect(viewModel.focus.rowIndex) {
    listState.animateScrollToItem(viewModel.focus.rowIndex)
}

// ContentRow.kt — horizontal, only runs for the currently-focused row
// (every other row receives focusedColumn = null)
LaunchedEffect(focusedColumn) {
    if (focusedColumn != null) {
        listState.animateScrollToItem(focusedColumn)
    }
}
```

**8. Samsung remote input mapping.** Per Samsung's Remote Control developer
docs, the remote's directional pad and OK button arrive as the exact same
`Key.DirectionLeft/Right/Up/Down` / `Key.Enter` values shown above — no
Tizen-specific code is needed for those. The one exception is the remote's
**Back** button, which Tizen sends as DOM keycode `10009` — a
Samsung-proprietary code no desktop browser produces. Since it was unclear
whether Compose's built-in key-code translation would recognize it,
`PlatformInputBridge.wasmJs.kt` listens for it directly:

```kotlin
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

> ⚠️ **Not verified on a real Tizen device.** This is the standard,
> documented keycode from Samsung's own docs, wired up defensively — but it
> has only ever been exercised by manually constructing that keycode in a
> test script, never by an actual TV remote.

**Getting keyboard input working at all in a browser turned out to be the
hardest part** — see `PlatformFocusBridge.wasmJs.kt` and Section 12
("Keyboard doesn't work") for the full story of why, and how it was solved.

---

## 8. Focus vs. selection

This distinction trips up a lot of people building TV UIs, so it's worth
being precise. This project uses **five** different things that all sound
like "focus," and only two of them are related to each other:

| Term | What it means here | Where it lives |
|---|---|---|
| **App-level "focus" (`TvFocusState`)** | Which row/column is logically "current" for TV navigation purposes | `HomeViewModel.focus`, a plain `TvFocusState(rowIndex, columnIndex)` data class — **not** a Compose API at all |
| **Selected card (`HomeViewModel.selected`)** | The card the user pressed Enter/OK on (or clicked). Shows the full-screen "Selected" overlay. | `HomeViewModel.selected: Content?` |
| **Clicked card** | A mouse/touch click on a specific `ContentCard`, which immediately moves focus there *and* selects it in one step | `ContentCard`'s `onClick`, wired to `HomeViewModel.selectAt(rowIndex, columnIndex)` |
| **DOM focus** | The browser's own concept of "which HTML element currently receives keyboard events" (`document.activeElement`, or — as this project discovered — `shadowRoot.activeElement`) | Web/Wasm only; see `PlatformFocusBridge.wasmJs.kt` |
| **Compose focus (`FocusRequester`)** | Compose's own internal focus-tree API | Exactly **one** `FocusRequester`, on the whole-screen root in `App.kt` — never on individual cards |

**Which one this project uses for navigation, and why:** almost entirely
the first one, `TvFocusState`. The project's own `TvFocusState.kt` doc
comment explains the reasoning directly:

> "We do NOT rely on Compose's built-in 2D focus traversal
> (`Modifier.focusable()` + `FocusManager.moveFocus(...)`) to move between a
> `LazyColumn` of independently-scrolling `LazyRow`s, because Compose's
> default focus search has no concept of 'the grid of rows has different
> lengths per row' [...] Instead, `rowIndex`/`columnIndex` are moved
> explicitly by `HomeViewModel`, and each card simply reads whether its own
> (row, column) matches this state."

So: **`TvFocusState` decides which card is visually "focused"** (the
animated border). **Compose's real `FocusRequester` API is used exactly
once**, on the root of the whole screen — its only job is to make sure
*something* holds real keyboard focus so key events arrive in the app at
all. **DOM focus** is a separate, lower-level, Web/Wasm-only concern: even
with Compose's root `FocusRequester` successfully claimed, the browser
still won't deliver real keyboard events unless the actual `<canvas>`
element Compose draws to also has genuine browser focus (see Section 12).
**Selection** (`HomeViewModel.selected`) is unrelated to any of the above —
it's just "which card did the user confirm," shown as an overlay.

---

## 9. Input flow

**On a real Samsung TV (not personally verified end-to-end):**

```
Samsung Remote (D-pad / OK / Back)
        │
Tizen's web engine turns the button press into a KeyboardEvent
        │
Compose Multiplatform's Web/Wasm keyboard handling
        │  (attached to the <canvas> Compose renders into)
        ▼
App.kt's onKeyEvent → HomeViewModel.handleKeyEvent(event)
        │
Navigation logic: HomeViewModel.move(direction) / activate() / back()
        │
Focused row/card updates: HomeViewModel.focus (TvFocusState)
        │
Compose recomposition: ContentRow/ContentCard re-read `focus`
        ▼
Visual focus indicator: the animated border in ContentCard
```

**The equivalent Chrome development flow (this is what has actually been
tested):**

```
Physical PC keyboard, Arrow key or Enter pressed
        │
Chrome generates a real, OS-level KeyboardEvent
        │
The event reaches the <canvas> Compose Multiplatform created
        │  (only if that canvas has real browser focus — see Section 12)
        ▼
Same code as above: App.kt → HomeViewModel.handleKeyEvent → move()/activate()
        │
Focused card updates, UI recomposes, border animates
```

The two flows are identical from `App.kt`'s `onKeyEvent` downward — the
only thing that differs between "real TV remote" and "PC keyboard in
Chrome" is what generates the `KeyboardEvent` in the first place, which is
entirely outside this app's code.

---

## 10. Mouse/click input

```
Mouse click or touch tap on a card
        │
Browser/Compose pointer event (Compose Multiplatform's own pointerdown/
pointerup handling, attached to the <canvas>)
        │
ContentCard's Modifier.clickable(onClick = onClick)
        │
ContentRow's onCardClick(columnIndex) callback
        ▼
HomeViewModel.selectAt(rowIndex, columnIndex)
```

`ContentCard.kt`:

```kotlin
.clickable(onClick = onClick)
```

`HomeViewModel.kt`:

```kotlin
/**
 * Touch/mouse equivalent of "move focus here, then press ENTER" in one
 * step.
 */
fun selectAt(rowIndex: Int, columnIndex: Int) {
    if (selected != null) return
    focus = TvFocusState(rowIndex, columnIndex)
    activate()
}
```

Clicking a card does two things at once that a remote needs two separate
button presses for: it moves logical focus to that card **and** selects it,
in a single call.

**Special handling required for Web/Wasm:** clicking a card itself needed
**no special code at all** — Compose Multiplatform's `clickable()` modifier
works the same way on Web/Wasm as on Android/iOS, and this was confirmed
directly during development (a real click was always able to select a
card). The thing that *did* need special, Web/Wasm-only handling was
completely different and easy to conflate with clicking: making
**keyboard** input work *without* first requiring a click. That is
`PlatformFocusBridge.wasmJs.kt`, covered in Section 7 and Section 12.

---

## 11. How another developer can do this for their own CMP project

Starting from an existing Compose Multiplatform project (with Android and
possibly iOS targets already working):

**Step 1 — Add the `wasmJs` target.** In your shared module's
`build.gradle.kts`, add the `wasmJs { browser { ... }; binaries.executable() }`
block from Section 3. *Why:* this is what turns on Kotlin/Wasm compilation
at all — without it, there's no web output to eventually put on a TV.

**Step 2 — Make `commonMain` Web-compatible.** Make sure nothing in your
shared UI code depends on an Android-only or iOS-only API (see Section 13).
*Why:* Kotlin/Wasm can only compile code that has no such platform-specific
dependencies; anything platform-specific needs an `expect`/`actual` split
like this project's `PlatformBackHandler`/`PlatformInputBridge`/
`PlatformFocusBridge`.

**Step 3 — Build the Wasm distribution.**
`./gradlew :shared:wasmJsBrowserDistribution`. *Why:* confirms your app
actually compiles to Wasm and produces real output, before you involve
Tizen at all. Test this output in a **desktop browser first** — it's much
faster to iterate on than a TV.

**Step 4 — Create a Tizen Web App project.** A folder containing a
`config.xml` like Section 4's, plus an icon. *Why:* this is the minimum
Tizen needs to recognize something as an installable app.

**Step 5 — Copy the Web output in.**
`cp -r shared/build/dist/wasmJs/productionExecutable/* tizen-app/` (or write
a script like this project's `copy-web-dist-to-tizen.sh`). *Why:* this is
the step that actually joins your CMP build to the Tizen wrapper.

**Step 6 — Configure `config.xml`.** Set your own app id, name, icon, and
**check what `required_version` your Compose Multiplatform version actually
needs** (Section 6/13 — do not skip this; get it wrong and your app either
installs somewhere it can't run, or refuses to install where it could).
*Why:* this is your app's actual manifest — wrong values here cause
install-time failures or silent runtime failures on real hardware.

**Step 7 — Create a Samsung certificate.** Tizen Studio → Certificate
Manager (Section 5, steps 1–2 and 5). *Why:* Tizen refuses to install
unsigned apps.

**Step 8 — Enable TV Developer Mode.** On the TV itself (Section 5, step
3). *Why:* a retail Samsung TV does not accept unsigned/development app
installs by default; Developer Mode is what allows it.

**Step 9 — Connect the TV.** `sdb connect <tv-ip>` (Section 5, step 4).
*Why:* Tizen Studio/CLI needs a live connection to install or debug on the
device.

**Step 10 — Build and sign the `.wgt`.**
`tizen build-web` → `tizen package -t wgt -s <profile>` (Section 5, steps
8–10). *Why:* this produces the actual installable artifact.

**Step 11 — Install and run on the TV.**
`tizen install` → `tizen run` (Section 5, step 11). *Why:* this is the
actual deployment step — the point where you find out if everything above
worked.

**Step 12 — Implement remote navigation.** Build your own equivalent of
`TvFocusState` + `HomeViewModel.move()`/`activate()` + a single root
`onKeyEvent`/`FocusRequester`, following Section 7 and Section 8. *Why:*
without this, your app might *display* correctly on a TV but be completely
unusable with a remote control — which, for a TV app, defeats the purpose.
**And apply the fix in `PlatformFocusBridge.wasmJs.kt`** (or your own
version of it) — without it, arrow keys may not respond on Web/Wasm at all
until a mouse click happens (Section 12).

---

## 12. Common problems

| Problem | Why it happens | How to diagnose | How to fix |
|---|---|---|---|
| **Wasm app works in Chrome but not on the TV** | The TV's Tizen version bundles a Chromium build too old to support WebAssembly GC (Section 6) — this project's own finding, not a hypothetical | Check the TV's model year against the table in Section 6; a 2024-or-earlier Samsung TV cannot run this class of app at all | Only a **2025+ TV (Tizen 9.0+)** — or the Tizen 9.0+ emulator — can run it. There is no code fix; this is a hardware/firmware ceiling. |
| **Blank screen on load** | Either the `.wasm`/`shared.js` files are missing/mismatched with `index.html`, or the Tizen web engine can't execute the Wasm at all (see above) | Open the browser/Tizen dev console and look for a 404 (missing file) vs. a WebAssembly compile error | If files are missing: re-run `copy-web-dist-to-tizen.sh` and confirm `tizen-app/` has `index.html`, `shared.js`, and the `.wasm` file(s) together. If it's a Wasm compile error: it's the compatibility issue above, not a missing-file issue. |
| **Keyboard/arrow keys don't work at all** | On Web/Wasm, Compose attaches its keyboard listeners directly to the `<canvas>` element it renders into (confirmed by reading Compose Multiplatform's own source, `ComposeWindowInternal.web.kt`). If that canvas never receives real browser focus, no key event ever reaches it, regardless of what your Kotlin code does. | In Chrome DevTools, check `document.activeElement` — if it's `<body>`, the canvas never got focus | This project's fix: `PlatformFocusBridge.wasmJs.kt` locates the actual `<canvas>` (it's nested inside a Shadow DOM — see that file's doc comment) and calls `.focus()` on it directly when the app starts, combined with calling Compose's `FocusRequester.requestFocus()`. See Section 7. |
| **Click doesn't work** | Not actually observed as a real problem in this project — `ContentCard`'s `clickable()` worked correctly on Web/Wasm without any special code, confirmed directly. If you hit this in your own project, first make sure you haven't confused it with the keyboard-focus problem above (it's an easy mix-up — a UI that "does nothing" on load can look like either). | Try clicking directly on a card vs. pressing an arrow key — if clicking selects the card but arrow keys do nothing, it's the focus issue above, not a click issue | If a genuine click issue exists, check for another DOM element unintentionally overlapping/covering the canvas (`z-index`/`position` CSS), or a `pointer-events: none` style. Not needed in this project. |
| **Focus is lost / disappears unexpectedly** | Something (a dismissed overlay, a recomposition) resets `HomeViewModel.focus` unexpectedly, or the canvas loses real DOM focus (e.g. the user clicked outside the app) | Add a temporary log of `HomeViewModel.focus` on every change | In this project, the selection overlay does **not** touch `focus` when dismissed (`back()` only clears `selected`), so focus position survives. If you add new overlays/dialogs of your own, make sure they don't reset `focus`. |
| **Arrow keys don't navigate correctly at row/column boundaries** | Missing or wrong `coerceIn(...)` clamping in your move logic | Log `rowIndex`/`columnIndex` while pressing arrows repeatedly at an edge | This project's `moveColumn`/`moveRow` in `HomeViewModel.kt` already clamp correctly (Section 7) — copy that pattern. |
| **Wrong Tizen version targeted** | `config.xml`'s `required_version` doesn't match what your Compose Multiplatform version actually needs | Check the Compose Multiplatform docs' "Compatibility and versions" page for your exact version's browser/WasmGC requirement, and cross-reference Samsung's Web Engine Specifications page for Chromium-per-Tizen-version | Set `required_version` accordingly (this project uses `"9.0"` for Compose Multiplatform 1.12.0 — re-check for other versions) |
| **Certificate/signing problems** | No certificate profile configured, or `config.xml`'s package id doesn't match the certificate's author id | Tizen Studio will show a clear packaging error naming the mismatch | Create/select a certificate profile in Certificate Manager (Section 5, step 5) and let Tizen Studio rewrite `config.xml`'s id, or set it manually |
| **TV can't connect to Tizen Studio** | TV Developer Mode isn't on, wrong IP entered on the TV, or PC/TV aren't on the same network | `sdb devices` shows nothing, or a connection timeout | Re-check Developer Mode is on, re-enter the PC's IP on the TV, confirm both devices are on the same Wi-Fi/LAN |
| **`.wgt` installation failure** | Either a signing problem (above), or the TV's Tizen version is below `required_version` in `config.xml` | Tizen's install command/log names the specific rejection reason | If it's a version mismatch: this is Section 6's compatibility wall — no fix except newer hardware/emulator |
| **Missing Wasm/JS/assets in the Tizen folder** | The copy step (Section 4) was skipped or run against a stale build | `tizen-app/` is missing `shared.js` or the `.wasm` file(s), or they're older than your latest code change | Re-run `./scripts/copy-web-dist-to-tizen.sh` after every change to `shared` before repackaging for Tizen |
| **Browser compatibility issues (desktop)** | Same WasmGC requirement as Section 6, just for desktop browsers instead of TVs | Check your browser's version against Chrome/Edge 119+, Firefox 120+, Safari 18.2+ | Update the browser, or test in a current Chrome/Edge for development |

---

## 13. Important limitations

These are the limitations that are actually relevant to *this* project —
not a generic list.

- **Chrome compatibility does not guarantee Samsung TV compatibility.**
  This is the single most important limitation in the whole project. Every
  bit of runtime testing done so far — all of Section 7's keyboard-focus
  work included — was done in **desktop Chrome only**. Nothing has been
  confirmed on a real Tizen device or the Tizen emulator. Section 6 explains
  why even a 2025+ TV, which *should* work based on its published Chromium
  version, has not actually been tested.
- **2024-and-earlier Samsung TVs cannot run this app at all**, for the
  WasmGC/Chromium-version reason in Section 6. This is not something any
  amount of app-level Kotlin code can fix — it is a ceiling built into
  those TVs' firmware.
- **Android TV focus behavior is not automatically identical to Tizen's
  (or the browser's).** This is exactly why the project avoids Compose's
  built-in `Modifier.focusable()`/`FocusManager` focus traversal for the
  card grid (Section 8) — relying on each platform's default focus-search
  behavior to agree with the other two, in a nested-scrolling-list layout,
  was judged too risky without being able to test all three side by side.
- **Android APIs cannot simply be used on Web.** Not directly hit as a
  problem in this project (this app doesn't call any Android-only API from
  shared code), but it's the reason `PlatformBackHandler`/
  `PlatformInputBridge`/`PlatformFocusBridge` exist as `expect`/`actual`
  splits at all — anything that needs a real platform API must be written
  per-platform.
- **Android Media3/ExoPlayer cannot simply be used on Tizen Web.** **Not
  implemented in this project** — there is no video playback anywhere in
  this app (selecting a card just shows a text overlay, by design — see
  `README.md`'s "No backend" section). If you add real video playback on
  top of this project's structure, you will need a separate,
  Web/Tizen-specific video approach; Media3/ExoPlayer is Android-only.
- **DRM/video playback may require separate Tizen-specific work.** Same as
  above — **not implemented or investigated in this project** at all, since
  there is no video content here.
- **Older Samsung TVs may not support the required WebAssembly features.**
  This is the same finding as Section 6, restated as a general rule for any
  future Compose Multiplatform version: always re-check the current
  minimum browser/WasmGC requirement against Samsung's currently-published
  Chromium-per-Tizen-version table before assuming a given TV model year
  will work.

---

## 14. Complete flow diagram

```
Developer writes Kotlin
        ↓
Compose Multiplatform (App(), HomeScreen(), ContentRow(), ContentCard())
        ↓
commonMain
        ↓
Platform targets
   ┌────────┼─────────┐
   ↓        ↓         ↓
Android   iOS      wasmJs
                      ↓
                  Web/Wasm output
                  (index.html + shared.js + .wasm)
                      ↓
              Tizen Web App (tizen-app/ + config.xml)
                      ↓
                 .wgt package (signed)
                      ↓
               Samsung Smart TV
                      ↓
                  Tizen OS
                      ↓
              Web Engine (Chromium-based)
                      ↓
          Kotlin/Wasm application boots
                      ↓
             Compose UI renders
                      ↓
    TV remote navigation (D-pad + OK, via KeyboardEvents)
```

---

## Sources / further reading in this repo

- `README.md` — the original project brief write-up, including the full
  sourced research behind the WasmGC/Chromium/Tizen-version compatibility
  finding, and the reasoning against using `Modifier.focusable()`.
- `tizen-app/README-TIZEN.md` — the detailed Tizen packaging walkthrough
  this guide's Section 5 summarizes.
- `shared/src/commonMain/kotlin/com/example/dummytvapp/platform/PlatformFocusBridge.kt`
  and its `.wasmJs.kt` actual — the full story of the keyboard-focus fix in
  Section 7/12, including exactly what was tried and ruled out first.
