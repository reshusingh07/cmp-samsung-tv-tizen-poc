#!/usr/bin/env bash
# Builds the Compose Multiplatform Web (Kotlin/Wasm) production distribution
# and copies it into tizen-app/, overwriting the placeholder index.html.
#
# This script draws the exact boundary the brief asks for: everything before
# the "copy" step is the ordinary CMP Web/Wasm build (could just as well be
# hosted on a normal website); everything after is Tizen-specific packaging.
set -euo pipefail
cd "$(dirname "$0")/.."

echo "==> Building the wasmJs production distribution (./gradlew :shared:wasmJsBrowserDistribution)"
./gradlew :shared:wasmJsBrowserDistribution

DIST_DIR="shared/build/dist/wasmJs/productionExecutable"
TIZEN_DIR="tizen-app"

if [ ! -d "$DIST_DIR" ]; then
    echo "ERROR: $DIST_DIR does not exist. Did the Gradle build above succeed?" >&2
    exit 1
fi

echo "==> Copying $DIST_DIR/* into $TIZEN_DIR/ (config.xml and icon.png are left alone)"
find "$TIZEN_DIR" -maxdepth 1 -type f ! -name 'config.xml' ! -name 'icon.png' -delete
cp -r "$DIST_DIR"/* "$TIZEN_DIR"/

echo "==> Done. $TIZEN_DIR/ now contains:"
ls -la "$TIZEN_DIR"

cat <<'EOF'

Next steps (see README-TIZEN.md for the full walkthrough):
  1. Open Tizen Studio, File > Import > Tizen > "Tizen Project" and point it
     at the tizen-app/ folder (or use the tizen CLI directly - see below).
  2. Fix the placeholder author certificate in config.xml via
     Tools > Certificate Manager, then package + install:
       tizen package -t wgt -s <your-profile-name> -- tizen-app
       tizen install -n DummyTvApp.wgt -t <your-target-device-id>
EOF
