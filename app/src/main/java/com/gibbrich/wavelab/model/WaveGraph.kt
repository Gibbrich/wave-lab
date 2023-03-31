package com.gibbrich.wavelab.model

import kotlin.math.roundToInt

class WaveGraph(
    val width: Int,
    val height: Int,
    val wavePoints: List<WavePoint>
) {
    val pointStep = width.toFloat() / (wavePoints.size - 1)
    val graphPoints: List<GraphPoint>

    init {
        graphPoints = calculateGraphPoints()
    }

    fun getPointXCoordinateById(pointId: Int): Int {
        val clampedId = pointId.coerceIn(0, wavePoints.lastIndex)
        return (clampedId * pointStep).roundToInt()
    }

    fun getPointIdByXCoordinate(xCoordinate: Int): Int = (xCoordinate / pointStep).roundToInt()

    private fun calculateGraphPoints(): List<GraphPoint> {
        val topPoints = wavePoints.withIndex().map {
            getGraphPoint(it.index, it.value.max)
        }

        val bottomPoints = wavePoints.withIndex().reversed().map {
            getGraphPoint(it.index, it.value.min)
        }

        return topPoints + bottomPoints
    }

    private fun getGraphPoint(index: Int, normalizedHeight: Float): GraphPoint {
        val x = getPointXCoordinateById(index).toFloat()
        val dy = height / 2.0F
        val y = (1.0F - normalizedHeight) * dy
        return GraphPoint(x, y)
    }
}

data class GraphPoint(val x: Float, val y: Float)