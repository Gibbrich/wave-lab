package com.gibbrich.wavelab.model

import org.junit.Assert.assertEquals
import org.junit.Test

class WaveGraphTest {

    @Test
    fun `getPointXCoordinateById with valid index`() {
        val waveGraph = WaveGraph(
            100,
            50,
            listOf(WavePoint.create(0.0F, 1.0F), WavePoint.create(0.5F, 0.5F), WavePoint.create(1.0F, 0.0F))
        )
        val xCoordinate = waveGraph.getPointXCoordinateById(1)
        assertEquals(50, xCoordinate)
    }

    @Test
    fun `getPointXCoordinateById with invalid index`() {
        val waveGraph = WaveGraph(
            100,
            50,
            listOf(WavePoint.create(0.0F, 1.0F), WavePoint.create(0.5F, 0.5F), WavePoint.create(1.0F, 0.0F))
        )
        val xCoordinate = waveGraph.getPointXCoordinateById(3)
        assertEquals(100, xCoordinate)
    }

    @Test
    fun `test getPointIdByXCoordinate`() {
        val waveGraph = WaveGraph(
            100,
            50,
            listOf(WavePoint.create(0.0F, 1.0F), WavePoint.create(0.5F, 0.5F), WavePoint.create(1.0F, 0.0F))
        )
        val pointId = waveGraph.getPointIdByXCoordinate(75)
        assertEquals(2, pointId)
    }

    @Test
    fun `test calculateGraphPoints`() {
        val waveGraph = WaveGraph(
            100,
            50,
            listOf(WavePoint.create(0.0F, 1.0F), WavePoint.create(0.5F, 0.5F), WavePoint.create(1.0F, 0.0F))
        )
        assertEquals(
            listOf(
                GraphPoint(0.0F, 0.0F),
                GraphPoint(50.0F, 12.5F),
                GraphPoint(100.0F, 25.0F),
                GraphPoint(100.0F, 0.0F),
                GraphPoint(50.0F, 12.5F),
                GraphPoint(0.0F, 25.0F),
            ), waveGraph.graphPoints
        )
    }
}
