package com.upsiway.voidfill

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class TileCanvasView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    // Paint objects
    private val blackPaint = Paint().apply {
        color = Color.BLACK
        style = Paint.Style.FILL
    }

    private val backgroundPaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }

    private val bitmapPaint = Paint().apply {
        isAntiAlias = false
        isFilterBitmap = false
    }

    // Tile management
    private val tileManager = TileManager(context)

    // Drawing state
    private var isDrawing = false
    private var lastX = 0f
    private var lastY = 0f

    // Zoom and pan variables
    // NOTE: replacing the existing scale logic
    private val zoomLevels = listOf(0.125f, 0.25f, 0.5f, 1.0f, 2.0f, 4.0f, 8.0f)
    private var currentZoomIndex = zoomLevels.indexOf(1.0f)

    val scale: Float
        get() = zoomLevels[currentZoomIndex]
    /*
    private var scale = 1.0f
    private val minScale = 1.0f
    private val maxScale = 10.0f
     */

    private var translateX = 0f
    private var translateY = 0f
    private var isPanning = false
    private var previousTouchX = 0f
    private var previousTouchY = 0f
    private var previousTouchCount = 0

    // Brush size (2x2 pixels fixed)
    private val brushSize = 2

    // Scale gesture detector for pinch-to-zoom
    //private val scaleDetector = ScaleGestureDetector(context, ScaleListener())

    init {
        // Set view properties
        isClickable = true
        isFocusable = true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Clear background
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), backgroundPaint)

        // Apply zoom and pan transformations
        canvas.save()
        canvas.translate(translateX, translateY)
        canvas.scale(scale, scale)

        // Draw all visible tiles
        val visibleTiles = getVisibleTileCoordinates()
        for ((tileX, tileY) in visibleTiles) {
            val tile = tileManager.getTile(tileX, tileY) ?: continue

            // Get the cached bitmap for this tile (will build it if dirty)
            val bitmap = tile.getBitmap()

            // Calculate where to draw this tile on the canvas
            val startX = tileX * TileManager.TILE_SIZE.toFloat()
            val startY = tileY * TileManager.TILE_SIZE.toFloat()

            // Draw the bitmap for this tile in a single call
            canvas.drawBitmap(bitmap, startX, startY, bitmapPaint)
        }

        canvas.restore()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        // Process zoom gestures first
        //scaleDetector.onTouchEvent(event)

        val pointerCount = event.pointerCount
        val x = event.x
        val y = event.y

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                previousTouchX = x
                previousTouchY = y
                previousTouchCount = pointerCount

                if (pointerCount == 1) {
                    // Start drawing with one finger
                    // But don't draw at the initial point
                    isDrawing = true
                    lastX = x
                    lastY = y
                    // No drawing on initial touch
                    //drawAtPoint(screenToCanvasX(x).toInt(), screenToCanvasY(y).toInt())
                }
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                if (pointerCount == 2) {
                    // Two fingers for panning
                    handlePanning(event)
                } else if (pointerCount == 1 && isDrawing) {
                    // Draw with one finger
                    val canvasX = screenToCanvasX(x)
                    val canvasY = screenToCanvasY(y)

                    // Use Bresenham's algorithm to ensure smooth line drawing
                    val lastCanvasX = screenToCanvasX(lastX)
                    val lastCanvasY = screenToCanvasY(lastY)

                    if ((canvasX != lastCanvasX || canvasY != lastCanvasY)) {
                        // Draw a line from last point to current point
                        drawBresenhamLine(lastCanvasX.toInt(), lastCanvasY.toInt(), canvasX.toInt(), canvasY.toInt())
                        lastX = x
                        lastY = y
                    }
                }

                previousTouchCount = pointerCount
                return true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (isDrawing) {
                    isDrawing = false
                    // Save all modified tiles when the gesture ends
                    tileManager.saveModifiedTiles()
                }
                isPanning = false
                previousTouchCount = 0
                return true
            }

            MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_POINTER_DOWN -> {
                // When a second finger is placed, stop drawing
                if (event.actionMasked == MotionEvent.ACTION_POINTER_DOWN && isDrawing) {
                    isDrawing = false
                    // Save any modified tiles
                    tileManager.saveModifiedTiles()
                }
                return true
            }
        }

        return super.onTouchEvent(event)
    }

    private fun handlePanning(event: MotionEvent) {
        if (!isPanning) {
            isPanning = true
            return
        }

        val dx = event.x - previousTouchX
        val dy = event.y - previousTouchY

        translateX += dx
        translateY += dy

        previousTouchX = event.x
        previousTouchY = event.y

        invalidate()
    }

    private fun drawAtPoint(pixelX: Int, pixelY: Int) {
        // Draw with 2x2 brush size
        for (y in 0 until brushSize) {
            for (x in 0 until brushSize) {
                setPixel(pixelX + x, pixelY + y, true)
            }
        }
        //invalidate()
    }

    // Implement Bresenham's line algorithm for smooth line drawing
    private fun drawBresenhamLine(x0: Int, y0: Int, x1: Int, y1: Int) {
        val dx = abs(x1 - x0)
        val dy = abs(y1 - y0)
        val sx = if (x0 < x1) 1 else -1
        val sy = if (y0 < y1) 1 else -1
        var err = dx - dy

        var x = x0
        var y = y0

        while (true) {
            // Draw with 2x2 brush at this point
            drawAtPoint(x, y)

            // Exit condition
            if (x == x1 && y == y1) break

            // Calculate next point
            val e2 = 2 * err
            if (e2 > -dy) {
                err -= dy
                x += sx
            }
            if (e2 < dx) {
                err += dx
                y += sy
            }
        }
        invalidate()
    }

    private fun setPixel(x: Int, y: Int, filled: Boolean) {
        // Calculate which tile this belongs to
        val tileX = Math.floorDiv(x, TileManager.TILE_SIZE)
        val tileY = Math.floorDiv(y, TileManager.TILE_SIZE)

        // Calculate the position within the tile
        val tilePixelX = Math.floorMod(x, TileManager.TILE_SIZE)
        val tilePixelY = Math.floorMod(y, TileManager.TILE_SIZE)

        // Set the pixel in the tile
        tileManager.setPixel(tileX, tileY, tilePixelX, tilePixelY, filled)
    }

    // Convert screen coordinates to canvas coordinates
    private fun screenToCanvasX(screenX: Float): Float {
        return (screenX - translateX) / scale
    }

    private fun screenToCanvasY(screenY: Float): Float {
        return (screenY - translateY) / scale
    }

    // Determine which tiles are currently visible based on view dimensions and transform
    private fun getVisibleTileCoordinates(): List<Pair<Int, Int>> {
        val result = mutableListOf<Pair<Int, Int>>()

        // Calculate visible area in canvas coordinates
        val left = screenToCanvasX(0f)
        val top = screenToCanvasY(0f)
        val right = screenToCanvasX(width.toFloat())
        val bottom = screenToCanvasY(height.toFloat())

        // Calculate tile coordinates
        val startTileX = Math.floorDiv(left.toInt(), TileManager.TILE_SIZE)
        val startTileY = Math.floorDiv(top.toInt(), TileManager.TILE_SIZE)
        val endTileX = Math.floorDiv(right.toInt(), TileManager.TILE_SIZE) + 1
        val endTileY = Math.floorDiv(bottom.toInt(), TileManager.TILE_SIZE) + 1

        // Add all tiles in the visible range
        for (y in startTileY..endTileY) {
            for (x in startTileX..endTileX) {
                result.add(Pair(x, y))
            }
        }

        return result
    }

    // Save any pending tiles (called from activity onPause)
    fun savePendingTiles() {
        tileManager.saveModifiedTiles()
    }

    // zoom stuff?
    fun zoomIn() {
        if (currentZoomIndex < zoomLevels.size - 1) {
            val focusX = width / 2f
            val focusY = height / 2f
            applyZoomChange(currentZoomIndex + 1, focusX, focusY)
        }
    }

    fun zoomOut() {
        if (currentZoomIndex > 0) {
            val focusX = width / 2f
            val focusY = height / 2f
            applyZoomChange(currentZoomIndex - 1, focusX, focusY)
        }
    }

    private fun applyZoomChange(newIndex: Int, focusX: Float, focusY: Float) {
        val oldScale = scale
        currentZoomIndex = newIndex
        val newScale = scale
        val scaleFactor = newScale / oldScale

        // Adjust pan so that focus point stays fixed
        translateX = focusX - (focusX - translateX) * scaleFactor
        translateY = focusY - (focusY - translateY) * scaleFactor

        invalidate()
    }

    fun getZoomPercent(): Int {
        return (scale * 100).toInt()
    }

    fun panBy(dx: Float, dy: Float) {
        translateX += dx
        translateY += dy
        invalidate()
    }


}