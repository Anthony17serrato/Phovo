package com.serratocreations.phovo.branding

import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import org.jetbrains.skia.Paint
import org.jetbrains.skia.RRect
import org.jetbrains.skia.Rect
import org.jetbrains.skia.SamplingMode
import org.jetbrains.skia.Surface
import java.io.File
import kotlin.math.roundToInt

// core/designsystem/theme/Color.kt: primaryLight and primaryDark. The splash grounds follow the
// system light/dark setting, so the mark needs both.
private const val INK = 0xFF006B5F.toInt()
private const val INK_DARK = 0xFF83D5C6.toInt()

/** Run via `./gradlew :tools:branding:run`; the only argument is the repo root. */
fun main(args: Array<String>) {
    val root = File(args.firstOrNull() ?: error("pass the repo root as the first argument"))
    val wordmark = Wordmark(root.resolve("docs/branding/Fraunces[SOFT,WONK,opsz,wght].ttf"))
    val icon = SplashIcon(
        root.resolve("applicationContainers/iosApp/iosApp/Assets.xcassets/phovo_splash_icon.imageset/phovo_splash@3x.png"),
    )

    println("iOS imageset")
    val iosSet = root.resolve("applicationContainers/iosApp/iosApp/Assets.xcassets/phovo_wordmark.imageset")
    for ((width, suffix) in listOf(120 to "", 240 to "@2x", 360 to "@3x")) {
        writeWordmarkPng(wordmark, iosSet.resolve("phovo_wordmark$suffix.png"), width, INK)
        writeWordmarkPng(wordmark, iosSet.resolve("phovo_wordmark_dark$suffix.png"), width, INK_DARK)
    }

    println("Android vector")
    writeAndroidVector(wordmark, root.resolve("sharedumbrella/src/androidMain/res/drawable/phovo_wordmark.xml"))

    println("Desktop splash")
    val desktop = root.resolve("applicationContainers/desktopApp/src/jvmMain/assets/common")
    writeDesktopSplash(wordmark, icon, desktop.resolve("phovo_splash.png"), 1f)
    writeDesktopSplash(wordmark, icon, desktop.resolve("phovo_splash@2x.png"), 2f)

    println("README banner")
    val images = root.resolve("docs/images")
    writeReadmeBanner(wordmark, icon, images.resolve("phovo_readme_banner_light.png"), INK)
    writeReadmeBanner(wordmark, icon, images.resolve("phovo_readme_banner_dark.png"), INK_DARK)

    println("done")
}

/** The splash icon plus its ink box, so padding in the source never shifts a layout. */
private class SplashIcon(file: File) {
    val image: Image = Image.makeFromEncoded(file.readBytes())
    val ink: Rect

    init {
        val bitmap = Bitmap().apply { allocN32Pixels(image.width, image.height, false) }
        check(image.readPixels(bitmap)) { "cannot read pixels of $file" }
        var minX = image.width
        var maxX = -1
        var minY = image.height
        var maxY = -1
        for (y in 0 until image.height) {
            for (x in 0 until image.width) {
                if ((bitmap.getColor(x, y) ushr 24) > 8) {
                    if (x < minX) minX = x
                    if (x > maxX) maxX = x
                    if (y < minY) minY = y
                    if (y > maxY) maxY = y
                }
            }
        }
        check(maxX >= 0) { "$file is fully transparent" }
        ink = Rect.makeLTRB(minX.toFloat(), minY.toFloat(), (maxX + 1).toFloat(), (maxY + 1).toFloat())
    }

    /** Draws the icon's ink box at [height], top-left at ([x], [y]). Returns the drawn width. */
    fun draw(canvas: Canvas, x: Float, y: Float, height: Float): Float {
        val width = ink.width * height / ink.height
        canvas.drawImageRect(image, ink, Rect.makeXYWH(x, y, width, height), SamplingMode.MITCHELL, null, true)
        return width
    }

    fun widthAt(height: Float): Float = ink.width * height / ink.height
}

private fun Canvas.drawWordmark(wordmark: Wordmark, x: Float, y: Float, width: Float, color: Int) {
    val scale = width / wordmark.bounds.width
    save()
    translate(x, y)
    scale(scale, scale)
    translate(-wordmark.bounds.left, -wordmark.bounds.top)
    drawPath(wordmark.path, Paint().apply { this.color = color; isAntiAlias = true })
    restore()
}

private fun render(file: File, pxWidth: Int, pxHeight: Int, scale: Float, draw: Canvas.() -> Unit) {
    val surface = Surface.makeRasterN32Premul(pxWidth, pxHeight)
    surface.canvas.apply {
        scale(scale, scale)
        draw()
    }
    val png = surface.makeImageSnapshot().encodeToData(EncodedImageFormat.PNG, 100)
        ?: error("cannot encode $file")
    file.writeBytes(png.bytes)
    println("  ${file.name} ${pxWidth}x$pxHeight")
}

/** A bare wordmark on transparent, [width] px wide: the iOS launch screen imageset. */
private fun writeWordmarkPng(wordmark: Wordmark, file: File, width: Int, color: Int) {
    val height = (width / wordmark.aspect).roundToInt()
    render(file, width, height, 1f) { drawWordmark(wordmark, 0f, 0f, width.toFloat(), color) }
}

/**
 * The platform stretches the branding image to fill its slot (200x80dp on AOSP) with FIT_XY, so a
 * drawable of any other aspect gets squashed - measured at 2.48 against the mark's true 3.35 before
 * this padding existed. Declaring the slot's exact size and centring the glyphs inside a viewport of
 * the same aspect makes that stretch a no-op.
 */
private fun writeAndroidVector(wordmark: Wordmark, file: File) {
    val width = 1200
    val height = (width / wordmark.aspect).roundToInt()
    val dpWidth = 200
    val dpHeight = 80
    val viewportHeight = (width.toFloat() * dpHeight / dpWidth).roundToInt()
    val padY = ((viewportHeight - height) / 2f).roundToInt()
    file.writeText(
        """
        |<?xml version="1.0" encoding="utf-8"?>
        |<!--
        |    The Phovo wordmark, set in Fraunces (SemiBold, 144pt optical size, SOFT 100, WONK 1) and
        |    converted to outlines so no font has to ship in the app. Regenerate with
        |    ./gradlew :tools:branding:run rather than editing the path by hand.
        |
        |    Vector rather than a raster so it stays sharp at every density, matching how the splash icon
        |    is sized.
        |-->
        |<vector xmlns:android="http://schemas.android.com/apk/res/android"
        |    android:width="${dpWidth}dp"
        |    android:height="${dpHeight}dp"
        |    android:viewportWidth="$width"
        |    android:viewportHeight="$viewportHeight">
        |    <group android:translateY="$padY">
        |        <path
        |            android:fillColor="@color/phovo_wordmark_tint"
        |            android:pathData="${wordmark.vectorPathData(width.toFloat())}" />
        |    </group>
        |</vector>
        |
        """.trimMargin(),
    )
    println("  ${file.name} ${dpWidth}dp x ${dpHeight}dp, viewport ${width}x$viewportHeight, glyphs offset $padY")
}

/**
 * The desktop AWT splash: rounded card, icon, wordmark beneath it. The JVM -splash: flag takes one
 * flat PNG, so the lockup has to be baked in. Layout is in @1x pixels.
 */
private fun writeDesktopSplash(wordmark: Wordmark, icon: SplashIcon, file: File, scale: Float) {
    val cardWidth = 864f
    val cardHeight = 486f
    val radius = 20f
    val iconHeight = 240f
    val gap = 28f
    val markWidth = 200f
    val markHeight = markWidth / wordmark.aspect
    val top = (cardHeight - (iconHeight + gap + markHeight)) / 2

    render(file, (cardWidth * scale).toInt(), (cardHeight * scale).toInt(), scale) {
        drawRRect(
            RRect.makeXYWH(0f, 0f, cardWidth, cardHeight, radius),
            Paint().apply { color = 0xFFF5F5F3.toInt(); isAntiAlias = true },
        )
        icon.draw(this, (cardWidth - icon.widthAt(iconHeight)) / 2, top, iconHeight)
        drawWordmark(wordmark, (cardWidth - markWidth) / 2, top + iconHeight + gap, markWidth, INK)
    }
}

/**
 * The README banner: icon on the left, wordmark to its right, on a transparent ground so it sits on
 * GitHub's light and dark themes alike. Rendered at @2x of a layout in points.
 */
private fun writeReadmeBanner(wordmark: Wordmark, icon: SplashIcon, file: File, color: Int) {
    val scale = 2f
    val iconHeight = 240f
    val gap = 44f
    val markHeight = 118f
    val padX = 24f
    val padY = 24f
    val iconWidth = icon.widthAt(iconHeight)
    val markWidth = markHeight * wordmark.aspect
    val width = padX + iconWidth + gap + markWidth + padX
    val height = padY + iconHeight + padY

    render(file, (width * scale).roundToInt(), (height * scale).roundToInt(), scale) {
        icon.draw(this, padX, padY, iconHeight)
        drawWordmark(wordmark, padX + iconWidth + gap, (height - markHeight) / 2, markWidth, color)
    }
}
