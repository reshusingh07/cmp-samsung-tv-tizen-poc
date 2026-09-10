# Compose Multiplatform on Apple TV (tvOS) — developer guide

This guide explains how the shared Compose Multiplatform (CMP) UI in this
repository runs as a native **Apple TV app**, driven by the Siri Remote. It is
the tvOS counterpart of [`CMP_SAMSUNG_TV_GUIDE.md`](CMP_SAMSUNG_TV_GUIDE.md)
and assumes you have read the [README](../README.md) first.

The one thing to understand up front: **JetBrains does not publish tvOS
builds of Compose Multiplatform.** Kotlin/Native has supported `tvosArm64`
for years, and Compose's UIKit layer (`ComposeUIViewController`, Skiko
rendering, key events) is largely platform-neutral between iOS and tvOS, but
the official `org.jetbrains.compose.*` artifacts on Maven Central simply do
not contain `tvos_arm64` / `tvos_simulator_arm64` klibs. Adding
`tvosArm64()` to a stock CMP project therefore fails at dependency
resolution.

This project closes that gap with a **community fork** maintained by Sajid Ali
(`sajidalidev` on GitHub), consumed exactly the way its author intends: through
a single Gradle settings plugin, with no direct dependency on the fork
repositories and no change to the Compose coordinates in `build.gradle.kts`.

---

## Table of contents

1. [How tvOS support actually works](#1-how-tvos-support-actually-works)
2. [What changed in this project](#2-what-changed-in-this-project)
3. [Building and running](#3-building-and-running)
4. [Siri Remote input](#4-siri-remote-input)
5. [Screen density: the 10-foot canvas](#5-screen-density-the-10-foot-canvas)
6. [Versions and constraints](#6-versions-and-constraints)
7. [Common problems](#7-common-problems)
8. [Verified vs. not verified](#8-verified-vs-not-verified)
9. [Sources](#9-sources)

---

## 1. How tvOS support actually works

Three repositories are involved, all by the same maintainer:

| Repository | Role | What this project touches |
|---|---|---|
| [`sajidalidev/compose-tvos`](https://github.com/sajidalidev/compose-tvos) | The **Gradle settings plugin** `dev.sajidali.compose-tvos`, its version manifest, and the canonical docs at <https://sajidalidev.github.io/compose-tvos/> | Applied in `settings.gradle.kts`. This is the only coordinate we reference directly. |
| [`sajidalidev/compose-multiplatform-core`](https://github.com/sajidalidev/compose-multiplatform-core) (branch `tvos-main`) | Fork of JetBrains' Compose sources with real `tvosArm64`/`tvosSimulatorArm64` targets for runtime, ui, foundation, material3, navigation, lifecycle, etc. — plus the Siri Remote, focus and density work. Published to Maven Central as `dev.sajidali.compose.*` | Never referenced directly; resolved by the plugin for tvOS targets only. |
| [`sajidalidev/compose-multiplatform`](https://github.com/sajidalidev/compose-multiplatform) (branch `tvos-main`) | Fork of the Compose Gradle plugin and `components-resources` with tvOS resource packaging. Published as `dev.sajidali.compose:compose-gradle-plugin` and `dev.sajidali.compose.components:components-resources` | Never referenced directly; the settings plugin swaps it in for `org.jetbrains.compose`. |

### 1.1 What the settings plugin does at resolution time

Applying `id("dev.sajidali.compose-tvos")` in `settings.gradle.kts` does
four things, all inside Gradle's dependency-resolution machinery:

1. **tvOS variant injection.** A Gradle `ComponentMetadataRule` attaches a
   tvOS `available-at` variant, pointing at the `dev.sajidali.*` twin, onto
   the official `org.jetbrains.compose.*` / `org.jetbrains.androidx.*`
   "umbrella" module. Gradle's normal variant matching then picks that variant
   for `tvosArm64` / `tvosSimulatorArm64` configurations — and nothing else.
   Android, iOS and wasmJs configurations never see a `dev.sajidali`
   coordinate. You can confirm this yourself:

   ```bash
   ./gradlew :shared:dependencies --configuration tvosSimulatorArm64CompileKlibraries | grep sajidali
   ./gradlew :shared:dependencies --configuration iosSimulatorArm64CompileKlibraries | grep sajidali   # prints nothing
   ```

2. **Official-first.** Before injecting anything, the plugin checks whether the
   official artifact already ships a genuine tvOS klib at that exact version
   (some do — `org.jetbrains.compose.runtime`, the JetBrains `lifecycle` and
   `savedstate` libraries, Skiko). If so, it is left alone. This is what lets
   the same setup keep working as JetBrains gradually adds tvOS upstream.

3. **Plugin-marker interception.** `id("org.jetbrains.compose")` in
   `build.gradle.kts` is transparently resolved to
   `dev.sajidali.compose:compose-gradle-plugin` at the same version, so
   Compose Resources can be packaged into a tvOS app bundle. (This project has
   no `composeResources/` yet, so the swap is currently inert, but it is what
   makes `compose.components.resources` resolve for tvOS.)

4. **Same-version convention.** For every `org.jetbrains.X:Y:V` the fork
   publishes `dev.sajidali.X:Y:V` — identical version, differing only by the
   added tvOS klibs. There are no version mappings; a version the fork has not
   published fails loudly naming the coordinate. Compose **1.12.0 is the
   floor**; older lines are not back-published.

At the end of every build that touched a tvOS configuration the plugin prints
a summary line (`Injected tvOS variants into N module(s)`) and, if it probed
any coordinate and found no tvOS klib, a `WARNING` block listing them. The
plugin also offers `composeTvos { strictMode.set(true) }` to turn that block
into a build failure. This project ran with it on while bringing tvOS up —
which is how the material3 problem in [Section 7](#7-common-problems) was
caught — but ships with it **off**, because the same block also lists old
transitive candidate versions that Gradle probed and then discarded during
conflict resolution (see Section 7). Read the block; do not let it fail the
build.

### 1.2 What the fork adds beyond "it compiles"

From the fork's `TVOS.md` and its `[tvOS]`-prefixed commits, the parts that
matter for a TV app:

- Siri Remote presses are delivered as ordinary Compose `KeyEvent`s (see
  [Section 4](#4-siri-remote-input)), including key-repeat, and swipes on the
  clickpad are turned into direction key events.
- The Compose scene starts in `InputMode.Keyboard`, so `Modifier.focusable()`
  targets can take focus on a cold launch (there is no touch surface to flip
  the mode).
- The scene lays out at a "10-foot" density, giving the same 960×540 dp canvas
  Android TV uses for 1080p ([Section 5](#5-screen-density-the-10-foot-canvas)).
- An unconsumed Menu press is forwarded up the UIKit responder chain so tvOS
  can suspend the app, as Apple's Human Interface Guidelines require.
- `androidx.tv:tv-material` (Google's Android TV Material components) is
  ported to CMP as `dev.sajidali.androidx.tv:tv-material`. This project does
  not use it — its focus model is deliberately hand-rolled, see the README —
  but it is one line away if a future screen wants `Carousel`/`ImmersiveList`.

---

## 2. What changed in this project

| Where | Change | Why |
|---|---|---|
| `settings.gradle.kts` | `plugins { id("dev.sajidali.compose-tvos") version "1.4.2" }` | Everything in Section 1. `plugins {}` was also moved directly after `pluginManagement {}`, where Gradle requires it. `strictMode` is left at its default (off) — see Section 7. |
| `shared/build.gradle.kts` | `tvosArm64()` and `tvosSimulatorArm64()` added next to the two iOS targets, each producing the same static `shared` framework | Kotlin/Native targets for Apple TV hardware and the Apple Silicon simulator. No `tvosX64`: the fork does not build it (CMP 1.11+ dropped Apple x86_64 entirely). |
| `shared/build.gradle.kts`, `gradle/libs.versions.toml` | Compose dependencies declared as explicit coordinates (`libs.compose.*`) instead of the plugin's `compose.runtime` / `compose.material3` accessors; material3 pinned to `1.12.0-alpha03` | The accessors are deprecated in 1.12.0 ("Specify dependency directly"), and the material3 one resolves a version with no tvOS klib — see Section 7. |
| `shared/src/iosMain` → `shared/src/appleMain` | `main.ios.kt` renamed to `MainViewController.kt`; the three `Platform*.ios.kt` actuals renamed to `*.apple.kt` | They are byte-for-byte identical for iOS and tvOS. Kotlin's default source-set hierarchy makes `appleMain` the parent of both `iosMain` and `tvosMain`, so one copy serves both. The Swift-visible entry point is now `MainViewControllerKt.MainViewController()` (updated in `iosApp/iosApp/ContentView.swift`). |
| `tvosApp/` (new) | A plain Xcode project mirroring `iosApp/`: SwiftUI `App` → `UIViewControllerRepresentable` → `MainViewController()`, a "Compile Kotlin" run-script phase, `Config.xcconfig`, tvOS `Info.plist`, an asset catalog with App Icon / Top Shelf brand assets, and a shared `tvosApp` scheme | The tvOS app shell. Not a Gradle module, exactly like `iosApp`. |
| `scripts/run-tvos-simulator.sh` (new) | Boots an Apple TV simulator, runs `xcodebuild` (which runs Gradle), installs and launches the app | One-command build-and-run without opening Xcode; also what the verification in Section 8 used. |
| `gradlew` | Executable bit set | It was checked in without one, so every `./gradlew` — including the one Xcode runs — failed with "permission denied" on a fresh clone. |
| `shared/src/commonMain` | Doc comments only | No shared UI or input code changed for tvOS. |

The last row is the headline: **no `commonMain` Kotlin changed.** The whole
remote-navigation design (`TvFocusState`, `HomeViewModel.move()`,
`TvKeyHandling.kt`) was written against Compose's common `Key`/`KeyEvent`
API, and the fork delivers Siri Remote presses through exactly that API.

---

## 3. Building and running

### 3.1 From the command line (recommended first run)

```bash
./scripts/run-tvos-simulator.sh
```

The script picks the newest installed `Apple TV 4K (3rd generation)` simulator
(override with `TVOS_SIMULATOR_NAME`), boots it, runs `xcodebuild` for the
`tvosApp` scheme, installs the resulting `Dummy TV App.app` and launches it.
The first run is slow: Gradle downloads Kotlin/Native for the tvOS targets and
the fork klibs, then compiles and links the framework.

### 3.2 From Xcode

```bash
open tvosApp/tvosApp.xcodeproj
```

Pick an Apple TV simulator (or a real Apple TV, after filling in `TEAM_ID` in
`tvosApp/Configuration/Config.xcconfig`) and press Run. The "Compile Kotlin"
build phase runs

```bash
cd "$SRCROOT/.."
./gradlew :shared:embedAndSignAppleFrameworkForXcode
```

Kotlin's Gradle plugin reads Xcode's `SDK_NAME` / `PLATFORM_NAME`
(`appletvsimulator` or `appletvos`) and `ARCHS`, builds the matching
`tvosSimulatorArm64` / `tvosArm64` framework, and places it at
`shared/build/xcode-frameworks/<Configuration>/<SDK>/shared.framework`, which
is on the target's `FRAMEWORK_SEARCH_PATHS`. Because the framework is static
(`isStatic = true`), it is linked straight into the app executable and there is
nothing to embed or code-sign separately.

Two Xcode-side settings are worth knowing about because both are silent
build-breakers when missing:

- `ENABLE_USER_SCRIPT_SANDBOXING = NO` — Xcode 15+ defaults this to `YES` for
  new projects, which forbids the Gradle build phase from writing outside its
  declared outputs.
- `UILaunchScreen` (an empty dictionary) in `Info.plist` — modern tvOS crashes
  at launch for a storyboard-less bundle without it.

### 3.3 Gradle only

```bash
./gradlew :shared:linkDebugFrameworkTvosSimulatorArm64   # framework for the simulator
./gradlew :shared:linkDebugFrameworkTvosArm64            # framework for a real Apple TV
```

Output: `shared/build/bin/tvosSimulatorArm64/debugFramework/shared.framework`
(and the `tvosArm64` equivalent).

---

## 4. Siri Remote input

The fork translates `UIPress` events (in `KeyEvent.ios.kt`, shared with iOS)
and post-processes them for tvOS (in `ComposeSceneMediator.tvos.kt`). The
resulting Compose keys, and what `TvKeyHandling.kt` does with them:

| Siri Remote | `UIPressType` | Compose `Key` delivered | This app |
|---|---|---|---|
| Clickpad edge / D-pad up, down, left, right | `UpArrow`, `DownArrow`, `LeftArrow`, `RightArrow` | `Key.DirectionUp` / `Down` / `Left` / `Right` | `HomeViewModel.move(...)` |
| Clickpad swipe | (touch, not a press) | Same four direction keys, synthesised once per swipe | `HomeViewModel.move(...)` |
| Click the clickpad (Select) | `Select` | `Key.DirectionCenter` | `HomeViewModel.activate()` |
| Menu / Back (`<`) | `Menu` | `Key.Menu`, **rewritten to `Key.Back`** before dispatch | `HomeViewModel.back()` |
| Play/Pause | `PlayPause` | `Key.MediaPlayPause` | ignored |
| Hardware keyboard (simulator, Bluetooth) | regular `UIKey` | The usual `Key.*` codes, e.g. `Key.Enter`, `Key.Escape` | Enter → `activate()`, Escape → `back()` |

Two consequences for `TvKeyHandling.kt`, neither of which needed a code
change:

- The existing `Key.Enter, Key.NumPadEnter, Key.DirectionCenter -> activate()`
  line already covered Select, because it was written for Android TV's D-pad
  center, and the fork reuses that key.
- `Key.Back, Key.Escape -> back()` returns `back()`'s result, which is `false`
  when no card is selected. The fork treats a `false` from `onKeyEvent` on a
  Menu press as **unconsumed**, calls `super.pressesBegan` up the responder
  chain, and tvOS suspends the app — i.e. Menu on the root screen goes to the
  Apple TV Home screen, the behaviour Apple requires. When the selection
  overlay *is* showing, `back()` returns `true`, the press is consumed, and the
  overlay closes instead. This is the same "consume only if you handled it"
  contract the Android `BackHandler` follows, which is why the shared code
  needed no tvOS branch.

The root `Modifier.focusable()` + `Modifier.onKeyEvent` in `App.kt` is the
receiver for all of this, exactly as on the other platforms. It gets focus on
launch because the fork starts the scene in `InputMode.Keyboard`
(`PlatformFocusBridge.apple.kt` therefore reports "has input focus" as
always-true, same as iOS).

**In the tvOS Simulator**, with the Simulator window focused, the keyboard
arrow keys drive the D-pad, Return is Select and Escape is Menu. There is also
a virtual remote under Window ▸ Show Apple TV Remote.

---

## 5. Screen density: the 10-foot canvas

Apple TV always renders a 1920×1080 point canvas, at UIKit scale 1× (Apple TV
HD, or any Apple TV driving a 1080p display) or 2× (Apple TV 4K on a 4K
display). Left at UIKit's own density that would be a 1920×1080 **dp** canvas
— every card in this app would be half the size it is on Android TV, and
readable from the couch only with binoculars.

The fork instead sets the Compose scene density to `2 × UIKit scale`
(`TvSceneDensity.tvos.kt`), which is precisely Android TV's rule (1080p →
density 2.0). Both platforms therefore lay this app out on a **960×540 dp**
canvas, and the `160.dp × 210.dp` cards, `24.dp` gutters and typography in
`ContentCard.kt` / `HomeScreen.kt` look the same on an Apple TV as on an
Android TV, with about five cards and two rows visible at once. Touch
positions, safe-area insets and accessibility frames keep UIKit's own
point-to-pixel density, so nothing else is affected.

`tvosApp/tvosApp/ContentView.swift` applies `.ignoresSafeArea()` so Compose
owns the full canvas and receives tvOS's overscan margins as `WindowInsets`
rather than being shrunk by SwiftUI.

---

## 6. Versions and constraints

| Concern | Value in this project | Notes |
|---|---|---|
| `dev.sajidali.compose-tvos` | `1.4.2` | From the Gradle Plugin Portal. |
| Compose Multiplatform | `1.12.0` | The fork's supported floor and current line. Fork klibs mirror this version exactly. |
| Compose Material 3 | `1.12.0-alpha03` | The only material3 version with fork tvOS klibs. Applies to **all** targets — see Section 7. |
| Kotlin | `2.4.10` | The fork's klibs (and JetBrains' own 1.12.0 iOS klibs) are compiled with Kotlin/Native 2.3.20, `abi_version=2.3.0`. A newer compiler consumes older klibs fine; the fork's stated minimum is Kotlin 2.3.20. |
| Kotlin/Native targets | `tvosArm64`, `tvosSimulatorArm64` | `tvosX64` (Intel simulator) is not published by the fork. |
| tvOS deployment target | 17.0 (`tvosApp`) | A conservative POC choice; the Kotlin framework itself is linked for Kotlin/Native's default minimum. |
| Compose Resources | Dependency present, none used | The fork's Gradle plugin syncs `composeResources/` into the tvOS bundle the way it does for iOS. Not exercised here. |
| Plugin caches | `~/.gradle/compose-tvos-redirect-cache-v4/` | Variant-discovery and manifest caches. Delete if a fork publish changes shape and results look stale. |
| Network | Required on first build | The plugin fetches its version manifest from GitHub and probes Maven Central for tvOS variants; `--offline` uses the caches. |

Things this setup does **not** give you:

- Anything JetBrains has not built and the fork has not ported. The fork
  covers Compose core, material3 (+ adaptive), navigation, lifecycle,
  savedstate, navigation-event, `window-core`, `tv-material`, and tvOS builds
  of Koin and Coil 3. Any other KMP library must publish its own tvOS klibs.
- A guarantee of upstream parity. The fork is one maintainer's work, rebased
  onto JetBrains' `jb-main` periodically and republished roughly once per
  stable Compose line. Treat it as you would any pre-1.0 dependency.

---

## 7. Common problems

**`Could not resolve org.jetbrains.compose.material3:material3:1.11.0-alpha07`
for a tvOS configuration** (the plugin's WARN block names the same
coordinate). Compose Material 3 is versioned separately from Compose
itself, and the Gradle plugin's `compose.material3` accessor hard-codes a
pairing: the official 1.12.0 plugin maps it to `1.9.0`, the fork's 1.12.0
plugin to `1.11.0-alpha07` — and the fork publishes tvOS material3 klibs for
neither, only for `1.12.0-alpha03`. This project therefore declares
`org.jetbrains.compose.material3:material3:1.12.0-alpha03` explicitly (via
`libs.compose.material3`), which is also the version the compose-tvos docs
list for the 1.12 line and is published officially for Android, iOS, desktop
and wasmJs. Consequence: every platform moved from material3 `1.9.0` to
`1.12.0-alpha03`. The API this app uses (`MaterialTheme`, `Surface`, `Text`,
`darkColorScheme`) is unchanged between them.

**`[ComposeTvos] WARNING: tvOS variant discovery found nothing for 5
redirect-eligible module(s)`** listing things like
`org.jetbrains.androidx.savedstate:savedstate-compose-uikitarm64:1.3.6`,
`lifecycle-runtime-compose-uikitsimarm64:2.9.6` or
`navigationevent-compose:1.1.0` — after a build that otherwise **succeeded**.
These are not gaps. The plugin's Gradle `ComponentMetadataRule` runs on every
coordinate the resolver visits, and two kinds of harmless visitor end up in
the list:

- **iOS-only platform leaves.** `savedstate-compose-uikitarm64` and
  `lifecycle-runtime-compose-uikitsimarm64` are the *iOS* platform modules of
  the `savedstate-compose:1.3.6` / `lifecycle-runtime-compose:2.9.6` umbrellas.
  Those umbrellas resolve a genuine tvOS klib — from JetBrains' own artifacts,
  which already ship tvOS for lifecycle/savedstate (the fork's `TVOS.md` says
  as much), so the plugin leaves them alone ("official-first"). Their iOS
  leaves naturally have no tvOS variant, but nothing on tvOS asks for them.
- **Conflict-resolution losers.** `navigationevent-compose:1.1.0` is requested
  transitively and loses to the fork's `1.1.1`, which is what the graph
  actually contains.

`./gradlew :shared:dependencies --configuration tvosSimulatorArm64CompileKlibraries`
shows all of this (every umbrella has a `-tvossimulatorarm64` leaf under it),
and the framework links. The plugin has no hook to filter these out after
conflict resolution, which is why `strictMode` is off in this project: with it
on, the build fails *after* a successful link, when the plugin's diagnostics
service shuts down. A real gap looks different — the coordinate also appears in
a `Could not resolve ...` error, as material3 did above.

**`./gradlew: permission denied`** (including from Xcode's build phase). The
wrapper script was committed without its executable bit; fixed on this branch
with `git update-index --chmod=+x gradlew`. If you hit it on another checkout,
`chmod +x gradlew`.

**`Downloading https://services.gradle.org/distributions/gradle-9.4.1-bin.zip
failed: timeout (10000ms)`**. The wrapper's `networkTimeout` is 10 s and the
distribution is 137 MB behind a redirect. Re-run, or download the zip with
`curl -L` into the `~/.gradle/wrapper/dists/gradle-9.4.1-bin/<hash>/` directory
the failed attempt created — the wrapper skips the download when the zip is
already there and verifies its SHA-256 from `gradle-wrapper.properties`.

**Xcode: `Unable to locate a Java Runtime`** in the Compile Kotlin phase. Xcode
runs scripts with a minimal `PATH`; `/usr/bin/java` resolves through
`/usr/libexec/java_home`, so a JDK must be installed system-wide (or export
`JAVA_HOME` at the top of the build phase). A JetBrains Runtime bundled inside
Android Studio/IntelliJ is *not* visible that way unless you link it.

**Black screen, or `Could not find ... -tvosarm64` at link time, after a fork
update.** Clear `~/.gradle/compose-tvos-redirect-cache-v4/` and rebuild.
Also check `~/.m2` for stale `org/jetbrains/compose` "shadow" publishes if you
ever add `mavenLocal()`: the plugin prefers an official artifact that already
looks tvOS-capable, and a stale local one will win and mix two Compose
lineages (symptom: `IrPropertySymbolImpl is already bound`). This project's
repositories do not include `mavenLocal()`, so it is not exposed to this.

**Menu does nothing / app exits unexpectedly.** Check what `back()` returns.
`true` = "I handled it, stay in the app"; `false` = "nothing to go back to",
which on tvOS means the system suspends the app. That is correct at the root
screen and wrong anywhere else.

---

## 8. Verified vs. not verified

Everything below was done on this branch on a Mac with Xcode 26.5, tvOS 26.5
SDK and the "Apple TV 4K (3rd generation)" simulator; nothing has been run on
Apple TV hardware.

**Verified**

- `./gradlew :shared:linkDebugFrameworkTvosSimulatorArm64` compiles
  `commonMain` + `appleMain` for `tvos_simulator_arm64` and links a static
  `shared.framework`, with the tvOS graph resolving `dev.sajidali.*` klibs for
  exactly the Compose modules (ui, foundation, animation, material3
  `1.12.0-alpha03`, material-ripple, components-resources, navigationevent)
  and the iOS graph resolving zero `dev.sajidali` coordinates.
- `./scripts/run-tvos-simulator.sh`: `xcodebuild` runs the Compile Kotlin
  phase (Kotlin's `assembleDebugAppleFrameworkForXcodeTvosSimulatorArm64`
  copies the framework into `shared/build/xcode-frameworks/Debug/appletvsimulator26.5/`;
  `embedAndSignAppleFrameworkForXcode` is skipped because the framework is
  static), links `Dummy TV App.app`, installs it and launches it.
- Rendering: the home screen shows the title, row headers and cards at the
  expected 10-foot size (about 5.5 cards and two rows visible on the 960×540 dp
  canvas), the first card focused, dark Material 3 theme.
- No phantom input: two screenshots 4 s apart on a fresh launch are
  byte-identical.
- Scripted keyboard input to the Simulator (AppleScript `key code` for → → ↓
  and Return): focus moved to row 2 / column 3 ("Movie 015") with the rows
  auto-scrolling to keep it in view, and Select opened the selection overlay
  with the ✓ badge on the card. Interactive D-pad navigation was also
  confirmed by hand.

**Not verified**

- Menu / Back. Scripted Escape keystrokes (`key code 53` and ASCII 27 via
  System Events) produced no visible reaction and no app suspension, so they
  most likely never reached the simulated device as a Menu press; this says
  nothing yet about the `Key.Back -> back()` path itself, which is the same
  code that is verified on Web/Android. Test it by hand with the on-screen
  Apple TV Remote (Simulator ▸ Window ▸ Show Apple TV Remote) or a real
  remote: with the overlay open, Menu should close it; on the root screen,
  Menu should go to the tvOS Home screen.
- Clickpad swipes, key repeat, Play/Pause: not exercised.
- Release configuration, a real Apple TV, code signing with a team ID.
- Compose Resources on tvOS: the project has none to package.

## 9. Sources

- compose-tvos plugin docs: <https://sajidalidev.github.io/compose-tvos/>
  (how it works, supported versions, app embedding, troubleshooting)
- Fork of Compose core: <https://github.com/sajidalidev/compose-multiplatform-core>
  — `TVOS.md`, and the `compose/ui/ui/src/tvosMain/` sources read for
  Section 4 and 5 (`KeyEvent.ios.kt`, `ComposeSceneMediator.tvos.kt`,
  `TvBackNavigationEventInput.tvos.kt`, `TvSceneDensity.tvos.kt`,
  `ComposeHostingViewController.tvos.kt`)
- Fork of the Compose Gradle plugin: <https://github.com/sajidalidev/compose-multiplatform>
- Published artifacts: <https://repo1.maven.org/maven2/dev/sajidali/> and
  <https://plugins.gradle.org/plugin/dev.sajidali.compose-tvos>
- Compose Multiplatform 1.12.0 changelog (deprecation of the `compose.*`
  dependency accessors; upstream Siri Remote press support in 1.11):
  <https://github.com/JetBrains/compose-multiplatform/blob/master/CHANGELOG.md>
- Apple, tvOS Human Interface Guidelines — Remotes and focus:
  <https://developer.apple.com/design/human-interface-guidelines/remotes>
