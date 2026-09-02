# Packaging `tizen-app/` for a Samsung TV

This folder is a **separate project** from the Compose Multiplatform / Kotlin-Wasm
build in `../shared`. It only becomes a real app once you:

1. Build the CMP web distribution, and
2. Copy that distribution's files in here, replacing the placeholder `index.html`.

`../scripts/copy-web-dist-to-tizen.sh` does both steps for you. What follows
is the same thing done by hand, plus what comes after (signing/packaging/
installing), which requires Tizen Studio and cannot be scripted from this
repo alone.

## 1. Build the web distribution

```bash
./gradlew :shared:wasmJsBrowserDistribution
```

This produces `shared/build/dist/wasmJs/productionExecutable/`, containing
`index.html`, the compiled `shared.js` glue code, a `.wasm` binary, and any
static assets - exactly the "HTML / JavaScript / WebAssembly / assets"
bundle requirement 8 asks for. **This step has no Tizen-specific content at
all** - the same output would run on any desktop browser.

## 2. Copy it into `tizen-app/`

```bash
cp -r shared/build/dist/wasmJs/productionExecutable/* tizen-app/
```

(This overwrites the placeholder `index.html` but leaves `config.xml` and
`icon.png` - which belong to the Tizen wrapper, not the CMP build - alone.)

At this point `tizen-app/` contains a `config.xml` (Tizen's W3C-widget-format
manifest) sitting next to a completely ordinary Compose Multiplatform Web
build. That pairing **is** the Tizen Web Application.

## 3. Fix the placeholder signing identity

`config.xml`'s `<tizen:application id="XXXXXXXXXX.DummyTvApp" package="XXXXXXXXXX" .../>`
has an obviously-fake 10-character author prefix. Tizen requires this to
match a real certificate profile:

1. Install [Tizen Studio](https://developer.tizen.org/development/tizen-studio/download)
   plus its **TV Extension** (Tizen Studio > Package Manager > Extension SDK > TV).
2. Tools > **Certificate Manager** > create a new Tizen/Samsung certificate
   profile (a Samsung account is required for a device-deployable
   certificate; the default "Tizen" profile works for the emulator only).
3. Either let Tizen Studio rewrite `config.xml`'s package ID for you when you
   package through its UI, or open its config.xml editor and pick your
   profile from the "Package" tab.

## 4. Package, install, and run

The commands below are the standard, long-established Tizen CLI workflow
(`tizen build-web` -> `tizen package` -> `tizen install` -> `tizen run`).
**I was not able to re-confirm the exact current flags for these against a
live Samsung Developer doc page in this session** (my web searches for the
current CLI reference page did not resolve to a working page - see
README.md's "Verified toolchain versions" section for exactly what I could
and couldn't check). Treat these as a strong starting point, not gospel -
run `tizen --help` / `tizen package --help` / `tizen install --help` on your
own machine to confirm before relying on them:

```bash
# 1. Validate + stage the web app (produces tizen-app/.buildResult)
tizen build-web -- tizen-app

# 2. Package the staged output into a signed .wgt file
#    (run `tizen security-profiles list` to see your profile names)
tizen package -t wgt -s <your-certificate-profile> -- tizen-app/.buildResult

# 3. See connected/emulated TVs
sdb devices

# 4. Install onto a specific target
tizen install -n DummyTvApp.wgt -t <device-id-from-sdb-devices>

# 5. Launch it
tizen run -p XXXXXXXXXX.DummyTvApp -t <device-id-from-sdb-devices>
```

Tizen Studio's GUI (right-click the project > Run As > Tizen Web Application)
wraps all four of the package/install/run steps into one button and is the
easier path if you are doing this interactively rather than scripting a CI
pipeline.

## What I could not verify

- **I have not packaged, installed, or run this on the Tizen emulator or a
  real Samsung TV.** I have no macOS/Windows/Linux desktop with Tizen Studio
  in this environment, and no access to a Tizen emulator image or a physical
  TV. Everything above is the standard, documented Tizen Web App workflow,
  not something I've exercised end-to-end for this specific project.
- Per README.md's compatibility findings, **this app will refuse to install
  at all on 2024-or-earlier Samsung TVs** (config.xml's
  `required_version="9.0"` sees to that) because their bundled Chromium
  predates WebAssembly GC support. A 2025-or-newer TV (or the Tizen 9.0+
  emulator) is required to test any of this for real.
