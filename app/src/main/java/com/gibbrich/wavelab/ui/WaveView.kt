package com.gibbrich.wavelab.ui

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.core.graphics.withSave
import com.gibbrich.wavelab.R
import com.gibbrich.wavelab.model.WaveGraph
import com.gibbrich.wavelab.model.WavePoint
import com.google.android.material.math.MathUtils
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

class WaveView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(context, attrs, defStyle) {
    /**
     * Can be called often, beware of creating objects in handler
     */
    var onSelectedPointsChanged: ((Int, Int) -> Unit)? = null
    private var startPointId = 0
    private var endPointId = 0

    private var waveGraph: WaveGraph? = null
    private var points: List<WavePoint> = emptyList()
    private var cachedWidth = 0
    private var cachedHeight = 0

    private val startHandle = Handle(0, 0)
    private val endHandle = Handle(Int.MAX_VALUE, Int.MAX_VALUE)
    private var activeHandle: Handle? = null

    private val handleTouchMaxOffset = context.resources.getDimensionPixelSize(R.dimen.wave_handle_touch_distance)
    private var touchDelta = 0

    private val handleSnapAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 100
        interpolator = LinearInterpolator()
        addUpdateListener {
            val factor = it.animatedValue as Float
            startHandle.snapPosition(factor)
            endHandle.snapPosition(factor)
            invalidate()
        }
    }

    private val shadowRect = Rect()

    /**
     * NOTE - using Path with big amounts of data can affect performance. In this case
     * there can be 3 alternatives:
     * 1. Using bitmap to draw graph on it
     * 2. Using drawLines
     * 3. Extract graph to a separate view, which can reduce number of onDraw calls
     */
    private val graphPath = Path()
    private val graphPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.wave_graph_color)
    }
    private val shadowPaint = Paint().apply {
        color = context.getColor(R.color.wave_shadow)
    }
    private val inactiveHandlePaint = Paint().apply {
        color = context.getColor(R.color.wave_handle_inactive)
    }
    private val activeHandlePaint = Paint().apply {
        color = context.getColor(R.color.wave_handle_active)
        strokeWidth = 3.0F
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        cachedWidth = w - paddingStart - paddingEnd
        cachedHeight = h - paddingTop - paddingBottom
        if (tryUpdateWaveGraph()) {
            setSelection(startPointId, endPointId)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!hasData()) {
            return false
        }

        return when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> handleDownEvent(event)

            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> handleUpEvent()

            MotionEvent.ACTION_MOVE -> handleMoveEvent(event)

            else -> false
        }
    }

    override fun onDraw(canvas: Canvas) = canvas.withSave {
        translate(paddingStart.toFloat(), paddingTop.toFloat())

        if (hasData()) {
            // draw graph
            drawPath(graphPath, graphPaint)

            // draw shadows before and after selection handles
            drawShadow(canvas, 0, startHandle.visualPosition)
            drawShadow(canvas, endHandle.visualPosition, cachedWidth)

            // draw handles
            drawHandle(canvas, startHandle)
            drawHandle(canvas, endHandle)
        }
    }

    fun setData(newPoints: List<WavePoint>) {
        points = newPoints
        if (tryUpdateWaveGraph()) {
            setSelection(0, newPoints.lastIndex)
        }
    }

    fun setSelection(startPointId: Int, endPointId: Int) {
        this.startPointId = startPointId
        this.endPointId = endPointId
        if (!isMeasured() || !hasData()) {
            return
        }
        setHandlesPosition(startPointId, endPointId)
        invalidate()
    }

    private fun setHandlesPosition(startPointId: Int, endPointId: Int) {
        val startPosition = waveGraph?.getPointXCoordinateById(startPointId) ?: 0
        val endPosition = waveGraph?.getPointXCoordinateById(endPointId)?.minus(1) ?: Int.MAX_VALUE

        startHandle.logicPosition = startPosition
        startHandle.visualPosition = startPosition

        endHandle.logicPosition = endPosition
        endHandle.visualPosition = endPosition
    }

    private fun drawShadow(canvas: Canvas, start: Int, end: Int) {
        if (start < end) {
            shadowRect.apply {
                left = start
                top = 0
                right = end
                bottom = cachedHeight
            }
            canvas.drawRect(shadowRect, shadowPaint)
        }
    }

    private fun drawHandle(canvas: Canvas, handle: Handle) {
        val paint = if (activeHandle == handle) activeHandlePaint else inactiveHandlePaint
        val x = handle.visualPosition.toFloat()
        canvas.drawLine(x, 0.0F, x, cachedHeight.toFloat(), paint)
        drawHandleEar(canvas, handle == startHandle, x, paint)
    }

    private fun drawHandleEar(canvas: Canvas, isStartHandle: Boolean, x: Float, paint: Paint) {
        val left = x - 20
        val top = if (isStartHandle) 0f else (cachedHeight - 30).toFloat()
        val right = x + 20
        val bottom = if (isStartHandle) 30f else cachedHeight.toFloat()
        val startAngle = if (isStartHandle) 270f else 90f
        canvas.drawArc(left, top, right, bottom, startAngle, 180f, true, paint)
    }

    private fun handleUpEvent(): Boolean {
        if (activeHandle == null) {
            return false
        }

        activeHandle = null
        touchDelta = 0
        handleSnapAnimator.start()
        invalidate()
        return true
    }

    private fun handleDownEvent(event: MotionEvent): Boolean {
        if (activeHandle != null) {
            return true
        }

        val touchPoint = event.x
        val handle = when {
            ((touchPoint - startHandle.logicPosition).absoluteValue < handleTouchMaxOffset) -> startHandle
            ((touchPoint - endHandle.logicPosition).absoluteValue < handleTouchMaxOffset) -> endHandle
            else -> null
        }

        return if (handle != null) {
            activeHandle = handle
            touchDelta = event.x.toInt() - handle.logicPosition
            invalidate()
            true
        } else {
            false
        }
    }

    private fun handleMoveEvent(event: MotionEvent): Boolean {
        val activeHandle = activeHandle ?: return false
        val waveGraph = waveGraph ?: return false

        val startLogicPosition = startHandle.logicPosition
        val endLogicPosition = endHandle.logicPosition

        val visualPosition =
            (event.x.toInt() - touchDelta).coerceIn(0, cachedWidth - 1)
        val logicPosition =
            ((visualPosition / waveGraph.pointStep).roundToInt() * waveGraph.pointStep).toInt().coerceIn(0, cachedWidth - 1)

        activeHandle.visualPosition = visualPosition
        activeHandle.logicPosition = logicPosition

        // move inactive handle in case of collision
        if (activeHandle == startHandle) {
            if (startHandle.visualPosition > endHandle.visualPosition) {
                endHandle.visualPosition = startHandle.visualPosition
                endHandle.logicPosition = startHandle.logicPosition
            }
        } else if (activeHandle == endHandle) {
            if (endHandle.visualPosition < startHandle.visualPosition) {
                startHandle.visualPosition = endHandle.visualPosition
                startHandle.logicPosition = endHandle.logicPosition
            }
        }

        if (startHandle.logicPosition != startLogicPosition || endHandle.logicPosition != endLogicPosition) {
            notifyHandleMoved()
        }

        invalidate()
        return true
    }

    private fun tryUpdateWaveGraph(): Boolean {
        if (!isMeasured() || !hasData()) {
            return false
        }

        val graph = WaveGraph(cachedWidth, cachedHeight, points)
        waveGraph = graph

        graphPath.reset()
        graph.graphPoints.forEachIndexed { index, point ->
            if (index == 0) {
                graphPath.moveTo(point.x, point.y)
            } else {
                graphPath.lineTo(point.x, point.y)
            }
        }
        graphPath.close()

        return true
    }

    private fun notifyHandleMoved() {
        val waveGraph = waveGraph ?: return
        if (!hasData() || !isMeasured()) {
            return
        }

        startPointId = waveGraph.getPointIdByXCoordinate(startHandle.logicPosition)
        endPointId = waveGraph.getPointIdByXCoordinate(endHandle.logicPosition)
        onSelectedPointsChanged?.invoke(startPointId, endPointId)
    }

    private fun hasData() = points.isNotEmpty()
    private fun isMeasured() = cachedWidth != 0 && cachedHeight != 0
}

private class Handle(
    var visualPosition: Int,
    var logicPosition: Int
) {
    fun snapPosition(factor: Float) {
        if (visualPosition != logicPosition) {
            visualPosition =
                MathUtils.lerp(visualPosition.toFloat(), logicPosition.toFloat(), factor).toInt()
        }
    }
}