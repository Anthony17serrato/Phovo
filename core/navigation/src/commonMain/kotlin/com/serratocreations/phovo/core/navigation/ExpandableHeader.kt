package com.serratocreations.phovo.core.navigation

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Content the top app bar shows below its own row, at full width, while expanded. The bar's
 * actions stay in their row however tall this gets.
 */
@Stable
class ExpandableHeader(
    val state: ExpandableHeaderState,
    val content: @Composable () -> Unit
)

/**
 * Open or closed state of an [ExpandableHeader], following how Google Photos treats its backup
 * header: the header pushes the content down rather than scrolling it, a forward scroll drags the
 * header shut before the content moves, and pulling down past the top of the content drags it
 * open. On release it snaps to whichever end the drag got past [settleThresholdPx] towards.
 * Opening or closing from a tap, and a pull down passing the threshold, give haptic feedback.
 *
 * Attach [nestedScrollConnection] outside any top app bar scroll behavior, so the header gets
 * scrolls first and the bar cannot hide while the header is open.
 */
@Stable
class ExpandableHeaderState internal constructor(
    private val scope: CoroutineScope,
    private val settleThresholdPx: Float,
    private val hapticFeedback: HapticFeedback
) {
    /** Where the header is settled, or settling towards once a drag or animation ends. */
    var isExpanded by mutableStateOf(false)
        private set

    /** Visible height while a drag or animation is in progress, null once settled. */
    private var transientHeight by mutableStateOf<Float?>(null)

    /** Full height of the header content as last measured. Only read outside composition. */
    internal var contentHeight = 0f

    /** Whether the header was expanded when the current drag started, null outside a drag. */
    private var dragStartedExpanded: Boolean? = null

    /** Whether the current pull down has already passed the threshold and given its tick. */
    private var hasPulledPastThreshold = false

    private var settleJob: Job? = null

    internal fun visibleHeight(contentHeight: Float): Float =
        transientHeight ?: if (isExpanded) contentHeight else 0f

    /** How much of the header is showing, in pixels. */
    val visibleHeight: Float
        get() = visibleHeight(contentHeight)

    /** How much of the header is showing, from 0 when closed to 1 when fully open. */
    val expandedFraction: Float
        get() = when {
            transientHeight == null -> if (isExpanded) 1f else 0f
            contentHeight <= 0f -> 0f
            else -> (visibleHeight / contentHeight).coerceIn(0f, 1f)
        }

    fun expand() {
        hapticFeedback.performHapticFeedback(HapticFeedbackType.ToggleOn)
        animateTo(expanded = true)
    }

    fun collapse() {
        hapticFeedback.performHapticFeedback(HapticFeedbackType.ToggleOff)
        animateTo(expanded = false)
    }

    val nestedScrollConnection: NestedScrollConnection = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            // A forward scroll shrinks an open header before the content moves.
            if (available.y >= 0f || visibleHeight(contentHeight) <= 0f) return Offset.Zero
            return Offset(0f, dragBy(available.y))
        }

        override fun onPostScroll(
            consumed: Offset,
            available: Offset,
            source: NestedScrollSource
        ): Offset {
            // Whatever is left of a pull down once the content is at its top opens the header.
            // Flings are left out, so reaching the top with momentum does not throw it open.
            if (available.y <= 0f || source != NestedScrollSource.UserInput) return Offset.Zero
            return Offset(0f, dragBy(available.y))
        }

        override suspend fun onPreFling(available: Velocity): Velocity {
            val startedExpanded = dragStartedExpanded ?: return Velocity.Zero
            val height = visibleHeight(contentHeight)
            val isPartlyOpen = height > 0f && height < contentHeight
            settle(startedExpanded)
            // Swallow the fling while the header is moving, so the content does not scroll out
            // from under it, and when flinging forward from fully open so it closes in place.
            return if (isPartlyOpen || (height > 0f && available.y < 0f)) available else Velocity.Zero
        }
    }

    /** Moves the header by [delta] within its range, returning how much of it was used. */
    private fun dragBy(delta: Float): Float {
        val current = visibleHeight(contentHeight)
        val next = (current + delta).coerceIn(0f, contentHeight)
        if (next == current) return 0f
        settleJob?.cancel()
        if (dragStartedExpanded == null) dragStartedExpanded = isExpanded
        transientHeight = next
        // Ticks once per pull down, when letting go would now leave the header open.
        if (dragStartedExpanded == false && !hasPulledPastThreshold && next >= settleThresholdPx) {
            hasPulledPastThreshold = true
            hapticFeedback.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
        }
        return next - current
    }

    private fun settle(startedExpanded: Boolean) {
        dragStartedExpanded = null
        hasPulledPastThreshold = false
        val startHeight = if (startedExpanded) contentHeight else 0f
        val draggedFar = abs(visibleHeight(contentHeight) - startHeight) >= settleThresholdPx
        animateTo(expanded = if (draggedFar) !startedExpanded else startedExpanded)
    }

    private fun animateTo(expanded: Boolean) {
        settleJob?.cancel()
        val from = visibleHeight(contentHeight)
        isExpanded = expanded
        settleJob = scope.launch {
            animate(
                initialValue = from,
                targetValue = if (expanded) contentHeight else 0f,
                animationSpec = SettleAnimationSpec
            ) { value, _ -> transientHeight = value }
            transientHeight = null
        }
    }
}

@Composable
fun rememberExpandableHeaderState(settleThreshold: Dp = DefaultSettleThreshold): ExpandableHeaderState {
    val scope = rememberCoroutineScope()
    val settleThresholdPx = with(LocalDensity.current) { settleThreshold.toPx() }
    val hapticFeedback = LocalHapticFeedback.current
    return remember(scope, settleThresholdPx, hapticFeedback) {
        ExpandableHeaderState(scope, settleThresholdPx, hapticFeedback)
    }
}

/**
 * Lays out [header]'s content at its full height and shows as much of it as the state allows,
 * from the top down. Nothing is placed while the header is closed, so its controls stay out of
 * focus and accessibility until it opens.
 */
@Composable
fun ExpandableHeaderLayout(header: ExpandableHeader, modifier: Modifier = Modifier) {
    val state = header.state
    Layout(content = header.content, modifier = modifier.clipToBounds()) { measurables, constraints ->
        val placeables = measurables.map {
            it.measure(constraints.copy(minHeight = 0, maxHeight = Constraints.Infinity))
        }
        val contentHeight = placeables.maxOfOrNull { it.height } ?: 0
        state.contentHeight = contentHeight.toFloat()
        val height = state.visibleHeight(contentHeight.toFloat()).roundToInt()
        layout(constraints.maxWidth, height) {
            if (height > 0) placeables.forEach { it.place(0, 0) }
        }
    }
}

object ExpandableHeaderDefaults {
    /**
     * Fills the bar and the header while the header is open, fading in as it opens, so the open
     * header reads as one surface above the content.
     */
    val containerColor: Color
        @Composable get() = MaterialTheme.colorScheme.surfaceContainer
}

/** Paints [color] behind the top app bar and its header in step with how open [state] is. */
fun Modifier.expandableHeaderBackground(state: ExpandableHeaderState, color: Color): Modifier =
    drawBehind {
        val fraction = state.expandedFraction
        if (fraction > 0f) drawRect(color.copy(alpha = color.alpha * fraction))
    }

/**
 * Rounds the top corners of the content below [state]'s header while it is open, growing the
 * radius with the first [radius] of opening so the corners do not pop in. The cut-outs are filled
 * with [color], so the header's background appears to wrap around the content's corners. Google
 * Photos does the same to its feed.
 */
fun Modifier.expandableHeaderTopCorners(
    state: ExpandableHeaderState,
    color: Color,
    radius: Dp = DefaultTopCornerRadius
): Modifier = drawWithCache {
    val cornerRadius = min(radius.toPx(), state.visibleHeight)
    val fillColor = color.copy(alpha = color.alpha * state.expandedFraction)
    val cutOuts = Path().apply {
        if (cornerRadius > 0f) {
            val corners = Path().apply {
                addRect(Rect(0f, 0f, size.width, cornerRadius))
            }
            val rounded = Path().apply {
                addRoundRect(
                    RoundRect(
                        rect = Rect(0f, 0f, size.width, cornerRadius * 2),
                        topLeft = CornerRadius(cornerRadius),
                        topRight = CornerRadius(cornerRadius)
                    )
                )
            }
            op(corners, rounded, PathOperation.Difference)
        }
    }
    onDrawBehind { drawPath(cutOuts, fillColor) }
}.graphicsLayer {
    val cornerRadius = min(radius.toPx(), state.visibleHeight)
    if (cornerRadius > 0f) {
        shape = RoundedCornerShape(topStart = cornerRadius, topEnd = cornerRadius)
        clip = true
    }
}

private val DefaultSettleThreshold = 48.dp

private val DefaultTopCornerRadius = 28.dp

private val SettleAnimationSpec: AnimationSpec<Float> = spring()
