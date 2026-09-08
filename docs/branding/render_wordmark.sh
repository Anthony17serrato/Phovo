#!/usr/bin/env bash
# Regenerates every baked "Phovo" wordmark asset from the Fraunces source in this directory.
#
# The wordmark is baked into art rather than drawn at runtime because none of the three splash
# screens can render text: Android's system splash masks its icon drawable to a circle and only
# offers an image branding slot, the JVM's -splash: flag takes a single flat PNG, and the iOS
# launch screen is a storyboard shown before any app code runs.
#
# Requires macOS (CoreText supplies the variable-font rendering). Run from the repo root:
#     docs/branding/render_wordmark.sh
set -euo pipefail

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "$HERE/../.." && pwd)"
FONT="$HERE/Fraunces[SOFT,WONK,opsz,wght].ttf"

ICON_SRC="$ROOT/applicationContainers/iosApp/iosApp/Assets.xcassets/phovo_splash_icon.imageset/phovo_splash@3x.png"
IOS_SET="$ROOT/applicationContainers/iosApp/iosApp/Assets.xcassets/phovo_wordmark.imageset"
ANDROID_VEC="$ROOT/sharedumbrella/src/androidMain/res/drawable/phovo_wordmark.xml"
DESKTOP_DIR="$ROOT/applicationContainers/desktopApp/src/jvmMain/assets/common"

# core/designsystem/theme/Color.kt: primaryLight and primaryDark. The splash grounds follow the
# system light/dark setting on both platforms, so the mark needs both.
INK="006B5F"
INK_DARK="83D5C6"

echo "iOS imageset"
for pair in "120:" "240:@2x" "360:@3x"; do
    w="${pair%%:*}"; s="${pair##*:}"
    swift "$HERE/wordmark.swift" --font "$FONT" --mode png \
        --out "$IOS_SET/phovo_wordmark${s}.png" --width "$w" --color "$INK"
    swift "$HERE/wordmark.swift" --font "$FONT" --mode png \
        --out "$IOS_SET/phovo_wordmark_dark${s}.png" --width "$w" --color "$INK_DARK"
done

echo "Android vector"
TMP_PATH="$(mktemp -t phovo_wordmark_path)"
swift "$HERE/wordmark.swift" --font "$FONT" --mode path --out "$TMP_PATH" --width 1200
python3 - "$TMP_PATH" "$ANDROID_VEC" <<'PY'
import sys
src, dest = sys.argv[1], sys.argv[2]
w, h, d = open(src).read().split('\n')[:3]
w, h = int(w), int(h)
# The platform stretches the branding image to fill its slot (200x80dp on AOSP) with FIT_XY, so a
# drawable of any other aspect gets squashed - measured at 2.48 against the mark's true 3.35 before
# this padding existed. Declaring the slot's exact size and centring the glyphs inside a viewport of
# the same aspect makes that stretch a no-op.
dp_w, dp_h = 200, 80
viewport_h = round(w * dp_h / dp_w)
pad_y = round((viewport_h - h) / 2)
open(dest, 'w').write(f'''<?xml version="1.0" encoding="utf-8"?>
<!--
    The Phovo wordmark, set in Fraunces (SemiBold, 144pt optical size, SOFT 100, WONK 1) and
    converted to outlines so no font has to ship in the app. Regenerate with
    docs/branding/render_wordmark.sh rather than editing the path by hand.

    Vector rather than a raster so it stays sharp at every density, matching how the splash icon
    is sized.
-->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="{dp_w}dp"
    android:height="{dp_h}dp"
    android:viewportWidth="{w}"
    android:viewportHeight="{viewport_h}">
    <group android:translateY="{pad_y}">
        <path
            android:fillColor="@color/phovo_wordmark_tint"
            android:pathData="{d}" />
    </group>
</vector>
''')
print(f"  {dp_w}dp x {dp_h}dp, viewport {w}x{viewport_h}, glyphs offset {pad_y}")
PY
rm -f "$TMP_PATH"

echo "Desktop splash"
swift "$HERE/compose_desktop.swift" "$FONT" "$ICON_SRC" "$DESKTOP_DIR/phovo_splash.png" 1 "$INK"
swift "$HERE/compose_desktop.swift" "$FONT" "$ICON_SRC" "$DESKTOP_DIR/phovo_splash@2x.png" 2 "$INK"

echo "done"
