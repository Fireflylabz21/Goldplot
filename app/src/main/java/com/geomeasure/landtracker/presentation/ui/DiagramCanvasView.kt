package com.geomeasure.landtracker.presentation.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import com.geomeasure.landtracker.domain.model.LatLngNode

private val CanvasBackground = Color(0xFF1B2118)
private val GridLine = Color(0xFF2C3527)
private val BoundaryLine = Color(0xFFFFB85C)
private val BoundaryFill = Color(0x33A8B676)

@Composable
fun DiagramCanvasView(
    nodes: List<LatLngNode>,
    isClosed: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .background(CanvasBackground)
    ) {
        val width = size.width
        val height = size.height
        val padding = 70f

        // Draw Engineering Grid
        val gridSize = 50f
        var x = 0f
        while (x < width) {
            drawLine(GridLine, Offset(x, 0f), Offset(x, height), strokeWidth = 1f)
            x += gridSize
        }
        var y = 0f
        while (y < height) {
            drawLine(GridLine, Offset(0f, y), Offset(width, y), strokeWidth = 1f)
            y += gridSize
        }

        if (nodes.size < 2) return@Canvas

        val minLat = nodes.minOf { it.latitude }
        val maxLat = nodes.maxOf { it.latitude }
        val minLng = nodes.minOf { it.longitude }
        val maxLng = nodes.maxOf { it.longitude }

        val latSpan = (maxLat - minLat).takeIf { it > 0 } ?: 0.00001
        val lngSpan = (maxLng - minLng).takeIf { it > 0 } ?: 0.00001

        val drawWidth = width - (2 * padding)
        val drawHeight = height - (2 * padding)

        val scaledPoints = nodes.map { node ->
            val px = padding + ((node.longitude - minLng) / lngSpan * drawWidth).toFloat()
            val py = padding + ((maxLat - node.latitude) / latSpan * drawHeight).toFloat()
            Offset(px, py)
        }

        val polyPath = Path().apply {
            moveTo(scaledPoints.first().x, scaledPoints.first().y)
            for (i in 1 until scaledPoints.size) {
                lineTo(scaledPoints[i].x, scaledPoints[i].y)
            }
            if (isClosed) {
                close()
            }
        }

        // Draw boundary polygon stroke & fill
        if (isClosed) {
            drawPath(polyPath, BoundaryFill)
        }
        drawPath(polyPath, BoundaryLine, style = Stroke(width = 6f))

        // Draw Nodes
        scaledPoints.forEachIndexed { index, pt ->
            val nodeColor = if (index == 0) BoundaryLine else Color(0xFFB4BC83)
            drawCircle(nodeColor, radius = 9f, center = pt)
            drawCircle(Color.White, radius = 3f, center = pt)
        }
    }
}
