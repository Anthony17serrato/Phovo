# Phovo wordmark

The app name is set in **Fraunces**, at these axis values:

| Axis   | Value | Why |
| ------ | ----- | --- |
| `wght` | 600   | SemiBold — enough weight to hold next to the icon without going doughy. |
| `opsz` | 144   | Display optical size: high contrast, the thick-to-thin that makes the two `o`s read like glass. |
| `SOFT` | 100   | Fully soft terminals, so the serifs cushion rather than spike. |
| `WONK` | 1     | The alternate, slightly off-kilter letterforms. |

Tracking is −0.015em.

Fraunces is licensed under the SIL Open Font License 1.1 (see `OFL.txt`). The variable font here is
the Google Fonts release of https://github.com/undercasetype/Fraunces.

## Where it is used

In the app, only the splash screen, under the icon. Nothing else in the app uses this face — the UI
runs on the Material 3 type scale in `core/designsystem`.

Outside the app, the repo README banner: `docs/images/phovo_readme_banner_light.png` and `_dark.png`,
the icon beside the wordmark on a transparent ground, picked by GitHub's theme through `<picture>`.

The mark is **baked into art, not rendered at runtime**, because none of the three splash screens
can draw text:

| Platform | Splash mechanism | What ships |
| -------- | ---------------- | ---------- |
| Android  | `windowSplashScreenBrandingImage` | `phovo_wordmark.xml`, a VectorDrawable of the glyph outlines. The system masks the icon drawable to a circle (verified on a Pixel 9 emulator: a lockup composed into it loses its lower half), so the mark cannot sit directly under the icon — the branding slot puts it at the bottom of the screen. **That slot is a fixed 200x80dp and the platform stretches to fill it with FIT_XY**, so the drawable declares exactly 200x80dp and centres the glyphs in a viewport of the same aspect; any other aspect gets squashed. |
| iOS      | `LaunchScreen.storyboard` | `phovo_wordmark.imageset`, @1x/@2x/@3x with dark-appearance variants, 120×36pt below the icon. |
| Desktop  | JVM `-splash:` | `phovo_splash.png` / `@2x`, one flat image with the whole lockup composed in. |

No font file is bundled into any app binary; only outlines are.

## Regenerating

```
./gradlew :tools:branding:run
```

The renderer is a small Kotlin/JVM module in `tools/branding` that draws with Skia (Skiko, the same
library Compose Desktop renders through), so it runs on macOS, Linux or Windows. If the typography
changes, edit the axis values and tracking in `Wordmark.kt`; layouts and ink colors live in
`RenderBranding.kt`. Then rerun. Do not hand-edit the generated `pathData` or PNGs.
