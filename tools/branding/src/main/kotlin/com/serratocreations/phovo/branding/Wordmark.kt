package com.serratocreations.phovo.branding

import org.jetbrains.skia.Font
import org.jetbrains.skia.FontHinting
import org.jetbrains.skia.FontMgr
import org.jetbrains.skia.FontVariation
import org.jetbrains.skia.Path
import org.jetbrains.skia.PathBuilder
import org.jetbrains.skia.PathVerb
import org.jetbrains.skia.Point
import org.jetbrains.skia.Rect
import org.jetbrains.skia.TextLine
import java.io.File
import java.util.Locale

/**
 * The "Phovo" wordmark as glyph outlines, set in Fraunces at the axis values chosen for the brand
 * (see docs/branding/README.md). Outlines rather than text so every target can rasterise or embed
 * the mark without shipping the font.
 *
 * [path] is in Skia's Y-down space at [SIZE] px; [bounds] is its tight ink box.
 */
class Wordmark(fontFile: File) {
    val path: Path
    val bounds: Rect

    init {
        val base = FontMgr.default.makeFromFile(fontFile.absolutePath, 0)
            ?: error("cannot read font at $fontFile")
        val typeface = base.makeClone(
            arrayOf(
                FontVariation("opsz", 144f), // display optical size
                FontVariation("wght", 600f), // SemiBold
                FontVariation("SOFT", 100f), // fully soft terminals
                FontVariation("WONK", 1f),   // wonky alternates on
            ),
            0,
        )
        val font = Font(typeface, SIZE).apply {
            hinting = FontHinting.NONE
            isSubpixel = true
        }
        // TextLine shapes the run, so the font's own kerning applies before tracking is added.
        val line = TextLine.make(TEXT, font)
        val positions = line.positions
        val tracking = TRACKING_EM * SIZE
        val builder = PathBuilder()
        line.glyphs.forEachIndexed { i, glyph ->
            val outline = font.getPath(glyph) ?: return@forEachIndexed
            builder.addPath(outline, positions[2 * i] + i * tracking, positions[2 * i + 1])
        }
        path = builder.detach()
        bounds = path.bounds
    }

    val aspect: Float get() = bounds.width / bounds.height

    /**
     * Android VectorDrawable path data, origin at the ink box's top-left, scaled so the mark is
     * [width] units wide.
     */
    fun vectorPathData(width: Float): String {
        val scale = width / bounds.width
        fun pt(point: Point?): String {
            val p = requireNotNull(point) { "path segment is missing a point its verb needs" }
            return String.format(Locale.ROOT, "%.2f,%.2f", (p.x - bounds.left) * scale, (p.y - bounds.top) * scale)
        }
        return buildString {
            for (segment in path) {
                if (segment == null) continue
                when (segment.verb) {
                    PathVerb.MOVE -> append("M").append(pt(segment.p0))
                    PathVerb.LINE -> append("L").append(pt(segment.p1))
                    PathVerb.QUAD -> append("Q").append(pt(segment.p1)).append(" ").append(pt(segment.p2))
                    PathVerb.CUBIC -> append("C").append(pt(segment.p1)).append(" ").append(pt(segment.p2))
                        .append(" ").append(pt(segment.p3))
                    PathVerb.CLOSE -> append("Z")
                    PathVerb.CONIC -> error("conic segments have no VectorDrawable equivalent")
                    PathVerb.DONE -> Unit
                }
            }
        }
    }

    companion object {
        const val TEXT = "Phovo"
        const val SIZE = 512f
        const val TRACKING_EM = -0.015f
    }
}
