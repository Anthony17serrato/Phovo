package com.serratocreations.phovo.feature.photos.util

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.ScaleFactor
import me.saket.telephoto.ExperimentalTelephotoApi
import me.saket.telephoto.zoomable.DoubleClickToZoomListener
import me.saket.telephoto.zoomable.Viewport
import me.saket.telephoto.zoomable.ZoomFocalPoint
import me.saket.telephoto.zoomable.ZoomableCoordinateSystem
import me.saket.telephoto.zoomable.ZoomableState
import me.saket.telephoto.zoomable.spatial.CoordinateSpace
import me.saket.telephoto.zoomable.spatial.SpatialOffset

@OptIn(ExperimentalTelephotoApi::class)
data class CycleZoomOnDoubleClick(
    private val maxZoomFactor: Float? = null,
    private val onDoubleClick: () -> Unit = {}
) : DoubleClickToZoomListener {

    override suspend fun ZoomableCoordinateSystem.onDoubleClick(state: ZoomableState, centroid: SpatialOffset) {
        onDoubleClick()
        val transformation = state.contentTransformation.takeIf { it.isSpecified }
        val zoomFraction = state.zoomFraction

        if (transformation == null || zoomFraction == null) {
            // Content isn't ready yet. Technically, this should never happen because Modifier.zoomable()
            // doesn't register a double click listener until after it has measured the content.
            return
        }

        val isAtMaxZoom = if (maxZoomFactor == null) {
            zoomFraction >= 0.95f
        } else {
            maxZoomFactor - transformation.scale.maxScale < 0.05f
        }

        if (isAtMaxZoom) {
            state.resetZoom()
        } else {
            state.zoomTo(
                zoomFactor = maxZoomFactor ?: state.zoomSpec.maximum.factor,
                focal = ZoomFocalPoint.zoomAround(centroid),
            )
        }
    }

    override suspend fun onDoubleClick(state: ZoomableState, centroid: Offset) {
        with(state.coordinateSystem) {
            onDoubleClick(state, SpatialOffset(centroid, CoordinateSpace.Viewport))
        }
    }
}

internal val ScaleFactor.maxScale: Float
    get() = maxOf(scaleX, scaleY)