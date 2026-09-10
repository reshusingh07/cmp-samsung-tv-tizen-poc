#!/usr/bin/env bash
#
# Builds the tvOS app, installs it on an Apple TV simulator and launches it.
#
# The Xcode build runs `./gradlew :shared:embedAndSignAppleFrameworkForXcode`
# itself (the "Compile Kotlin" build phase in tvosApp.xcodeproj), so this is
# the complete Kotlin -> framework -> .app -> simulator pipeline in one step.
#
# Usage:
#   ./scripts/run-tvos-simulator.sh
#
# Optional environment variables:
#   TVOS_SIMULATOR_NAME   simulator device name (default: "Apple TV 4K (3rd generation)")
#   TVOS_CONFIGURATION    Debug (default) or Release
#   TVOS_DERIVED_DATA     Xcode DerivedData location (default: tvosApp/build/DerivedData)
#
# Requires Xcode with the tvOS platform installed (Xcode > Settings > Components).

set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SIM_NAME="${TVOS_SIMULATOR_NAME:-Apple TV 4K (3rd generation)}"
CONFIGURATION="${TVOS_CONFIGURATION:-Debug}"
DERIVED_DATA="${TVOS_DERIVED_DATA:-$ROOT/tvosApp/build/DerivedData}"
BUNDLE_ID="$(sed -n 's/^BUNDLE_ID=//p' "$ROOT/tvosApp/Configuration/Config.xcconfig")"

# Pick the requested Apple TV simulator on the newest installed tvOS runtime.
UDID="$(xcrun simctl list devices available -j | python3 -c '
import json, sys
name = sys.argv[1]
data = json.load(sys.stdin)["devices"]
matches = [
    (runtime, d["udid"])
    for runtime, devices in data.items() if "tvOS" in runtime
    for d in devices if d["name"] == name and d.get("isAvailable", True)
]
if not matches:
    sys.exit("No available tvOS simulator named %r. Installed: run `xcrun simctl list devices available | grep -A5 tvOS`" % name)
print(sorted(matches)[-1][1])
' "$SIM_NAME")"

echo "==> Simulator: $SIM_NAME ($UDID)"
xcrun simctl boot "$UDID" 2>/dev/null || true
open -a Simulator --args -CurrentDeviceUDID "$UDID"

echo "==> Building tvosApp ($CONFIGURATION) -- this also runs Gradle for the Kotlin framework"
xcodebuild \
  -project "$ROOT/tvosApp/tvosApp.xcodeproj" \
  -scheme tvosApp \
  -configuration "$CONFIGURATION" \
  -destination "id=$UDID" \
  -derivedDataPath "$DERIVED_DATA" \
  CODE_SIGNING_ALLOWED=NO \
  build

APP="$(find "$DERIVED_DATA/Build/Products/$CONFIGURATION-appletvsimulator" -maxdepth 1 -name '*.app' | head -1)"
if [[ -z "$APP" ]]; then
  echo "Built .app not found under $DERIVED_DATA/Build/Products" >&2
  exit 1
fi

echo "==> Installing $APP"
xcrun simctl install "$UDID" "$APP"

echo "==> Launching $BUNDLE_ID"
xcrun simctl launch "$UDID" "$BUNDLE_ID"

cat <<MSG

Running on the Apple TV simulator. With the Simulator window focused, the
keyboard drives the Siri Remote: arrow keys = D-pad, Return = Select,
Escape = Menu (Back). Screenshot with:
  xcrun simctl io "$UDID" screenshot tvos.png
MSG
