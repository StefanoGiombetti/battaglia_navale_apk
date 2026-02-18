package com.battaglianavale.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.*
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.battaglianavale.models.EnemyCellState
import com.battaglianavale.models.MyCellState
import com.battaglianavale.models.Position
import com.battaglianavale.ui.theme.*
import kotlin.math.min

private fun colLabel(col: Int) = ('A' + col).toString()

// ─── Full grid component ──────────────────────────────────────────────────────
@Composable
fun HandDrawnGrid(
    gridSize: Int,
    cellSizeDp: Dp,
    modifier: Modifier = Modifier,
    shipCells: List<Position>       = emptyList(),
    hitCells: List<Position>        = emptyList(),
    missCells: List<Position>       = emptyList(),
    sunkCells: List<Position>       = emptyList(),
    hoverCell: Position?            = null,
    previewCells: List<Position>    = emptyList(),
    previewValid: Boolean           = true,
    onCellTap: ((Position) -> Unit)? = null,
    onCellHover: ((Position?) -> Unit)? = null,
) {
    val density     = LocalDensity.current
    val cellPx      = with(density) { cellSizeDp.toPx() }
    val labelOffset = with(density) { 16.dp.toPx() }
    val totalW      = cellPx * gridSize + labelOffset
    val totalH      = cellPx * gridSize + labelOffset

    val textMeasurer = rememberTextMeasurer()

    Canvas(
        modifier = modifier
            .size(width  = with(density) { totalW.toDp() },
                  height = with(density) { totalH.toDp() })
            .then(
                if (onCellTap != null) {
                    Modifier.pointerInput(gridSize, cellSizeDp) {
                        detectTapGestures(
                            onPress  = { off ->
                                val pos = offsetToPosition(off, labelOffset, cellPx, gridSize)
                                onCellHover?.invoke(pos)
                            },
                            onTap    = { off ->
                                val pos = offsetToPosition(off, labelOffset, cellPx, gridSize)
                                pos?.let { onCellTap(it) }
                                onCellHover?.invoke(null)
                            }
                        )
                    }
                } else Modifier
            )
    ) {
        drawGridBackground(labelOffset, cellPx, gridSize)
        // Cell fills
        shipCells.forEach  { fillCell(it, ShipFill, labelOffset, cellPx) }
        sunkCells.forEach  { fillCell(it, SunkFill, labelOffset, cellPx) }
        hitCells.filter    { it !in sunkCells }.forEach { fillCell(it, HitFill, labelOffset, cellPx) }
        hoverCell?.let     { fillCell(it, InkBlue.copy(alpha = 0.12f), labelOffset, cellPx) }
        previewCells.forEach {
            fillCell(it, if (previewValid) InkGreen.copy(alpha = 0.35f) else InkRed.copy(alpha = 0.3f), labelOffset, cellPx)
        }
        // Grid lines
        drawGridLines(labelOffset, cellPx, gridSize)
        // Markers
        missCells.forEach  { drawCircleMarker(it, labelOffset, cellPx) }
        hitCells.filter { it !in sunkCells }.forEach { drawXMarker(it, labelOffset, cellPx, thick = false) }
        sunkCells.forEach  { drawXMarker(it, labelOffset, cellPx, thick = true) }
        // Labels
        drawLabels(textMeasurer, labelOffset, cellPx, gridSize)
    }
}

// ─── Helper: offset → Position ────────────────────────────────────────────────
private fun offsetToPosition(offset: Offset, labelOffset: Float, cellPx: Float, gridSize: Int): Position? {
    val col = ((offset.x - labelOffset) / cellPx).toInt()
    val row = ((offset.y - labelOffset) / cellPx).toInt()
    if (col < 0 || col >= gridSize || row < 0 || row >= gridSize) return null
    return Position(row, col)
}

// ─── Drawing helpers ──────────────────────────────────────────────────────────
private fun DrawScope.cellRect(pos: Position, labelOffset: Float, cellPx: Float): Rect =
    Rect(
        left   = labelOffset + pos.col * cellPx + 1f,
        top    = labelOffset + pos.row * cellPx + 1f,
        right  = labelOffset + (pos.col + 1) * cellPx - 1f,
        bottom = labelOffset + (pos.row + 1) * cellPx - 1f
    )

private fun DrawScope.fillCell(pos: Position, color: Color, labelOffset: Float, cellPx: Float) {
    val r = cellRect(pos, labelOffset, cellPx)
    drawRect(color = color, topLeft = r.topLeft, size = r.size)
}

private fun DrawScope.drawCircleMarker(pos: Position, labelOffset: Float, cellPx: Float) {
    val r   = cellRect(pos, labelOffset, cellPx)
    val pad = r.width * 0.22f
    drawCircle(
        color  = InkBlue.copy(alpha = 0.7f),
        radius = (r.width / 2f) - pad,
        center = r.center,
        style  = Stroke(width = 1.5f)
    )
}

private fun DrawScope.drawXMarker(pos: Position, labelOffset: Float, cellPx: Float, thick: Boolean) {
    val r   = cellRect(pos, labelOffset, cellPx)
    val pad = r.width * 0.18f
    val lw  = if (thick) 2.5f else 2.0f
    val stroke = Stroke(width = lw, cap = StrokeCap.Round)
    drawLine(InkRed.copy(alpha = 0.9f), Offset(r.left + pad, r.top + pad),  Offset(r.right - pad, r.bottom - pad), strokeWidth = lw, cap = StrokeCap.Round)
    drawLine(InkRed.copy(alpha = 0.9f), Offset(r.right - pad, r.top + pad), Offset(r.left + pad, r.bottom - pad),  strokeWidth = lw, cap = StrokeCap.Round)
}

private fun DrawScope.drawGridBackground(labelOffset: Float, cellPx: Float, gridSize: Int) {
    drawRect(Paper, topLeft = Offset.Zero, size = size)
}

private fun DrawScope.drawGridLines(labelOffset: Float, cellPx: Float, gridSize: Int) {
    for (i in 0..gridSize) {
        val x     = labelOffset + i * cellPx
        val y     = labelOffset + i * cellPx
        val isBorder = i == 0 || i == gridSize
        val color = if (isBorder) InkBlue.copy(alpha = 0.6f) else GridLine.copy(alpha = 0.7f)
        val lw    = if (isBorder) 1.5f else 0.8f
        // vertical
        drawLine(color, Offset(x, labelOffset), Offset(x, labelOffset + gridSize * cellPx), strokeWidth = lw)
        // horizontal
        drawLine(color, Offset(labelOffset, y), Offset(labelOffset + gridSize * cellPx, y), strokeWidth = lw)
    }
}

private fun DrawScope.drawLabels(textMeasurer: TextMeasurer, labelOffset: Float, cellPx: Float, gridSize: Int) {
    val style = TextStyle(
        color    = InkBlue.copy(alpha = 0.65f),
        fontSize = min(cellPx * 0.45f, 12f).sp,
        fontFamily = FontFamily.Monospace
    )
    for (col in 0 until gridSize) {
        val lbl    = colLabel(col)
        val result = textMeasurer.measure(lbl, style)
        val x      = labelOffset + col * cellPx + cellPx / 2 - result.size.width / 2
        drawText(result, topLeft = Offset(x, labelOffset / 2 - result.size.height / 2))
    }
    for (row in 0 until gridSize) {
        val lbl    = "${row + 1}"
        val result = textMeasurer.measure(lbl, style)
        val y      = labelOffset + row * cellPx + cellPx / 2 - result.size.height / 2
        drawText(result, topLeft = Offset(labelOffset / 2 - result.size.width / 2, y))
    }
}

// ─── Utilities re-used across screens ─────────────────────────────────────────
fun shipPositions(origin: Position, size: Int, horizontal: Boolean): List<Position> =
    (0 until size).map { i ->
        if (horizontal) Position(origin.row, origin.col + i)
        else            Position(origin.row + i, origin.col)
    }

fun isValidPlacement(origin: Position, size: Int, horizontal: Boolean,
                     gridSize: Int, placed: List<Position>): Boolean {
    val positions = shipPositions(origin, size, horizontal)
    if (positions.any { it.row < 0 || it.row >= gridSize || it.col < 0 || it.col >= gridSize }) return false
    val occupiedSet = placed.toSet()
    for (pos in positions) {
        if (pos in occupiedSet) return false
        for (dr in -1..1) for (dc in -1..1) {
            val neighbor = Position(pos.row + dr, pos.col + dc)
            if (neighbor in occupiedSet && neighbor !in positions) return false
        }
    }
    return true
}
