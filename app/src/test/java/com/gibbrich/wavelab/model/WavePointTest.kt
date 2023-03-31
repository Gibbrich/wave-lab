package com.gibbrich.wavelab.model

import org.junit.Assert
import org.junit.Test


/**
 * Note - further improvement add more tests on loading/saving logic
 */
class WavePointTest {
    @Test
    fun `Parse single line to wave point`() {
        // when
        val line = "-0.1 0.3"

        // action
        val point = WavePoint.parse(line)

        // then
        Assert.assertEquals(-0.1f, point.min)
        Assert.assertEquals(0.3f, point.max)
    }

    @Test
    fun `Wave min and max values clamped`() {
        // when
        val line = "-5 10"

        // action
        val point = WavePoint.parse(line)

        // then
        Assert.assertEquals(-1.0f, point.min)
        Assert.assertEquals(1.0f, point.max)
    }

    @Test
    fun `Fail parse single number line`() {
        // when
        val line = "-1.0"

        try {
            // action
            val wavePoint = WavePoint.parse(line)
            Assert.fail()
        } catch (e: Exception) {
            // then
            return
        }
    }

    @Test
    fun `Fail parse more then 2 number line`() {
        // when
        val line = "-1.0 0.4 0.3"

        try {
            // action
            val wavePoint = WavePoint.parse(line)
            Assert.fail()
        } catch (e: Exception) {
            // then
            return
        }
    }
}
