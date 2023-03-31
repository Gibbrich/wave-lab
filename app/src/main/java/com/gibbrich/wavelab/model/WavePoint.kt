package com.gibbrich.wavelab.model

import java.io.BufferedReader
import java.io.InputStream
import java.io.OutputStream
import java.lang.Float.max
import java.lang.Float.min


class WavePoint private constructor(
    val min: Float,
    val max: Float
) {
    companion object {
        fun create(min: Float, max: Float): WavePoint {
            val clampedMin = min.coerceIn(-1.0F, 1.0F)
            val clampedMax = max.coerceIn(-1.0F, 1.0F)
            return WavePoint(clampedMin, clampedMax)
        }

        fun save(data: List<WavePoint>, outputStream: OutputStream) = outputStream
            .bufferedWriter()
            .use { writer ->
                data.forEach {
                    writer.write("${it.min} ${it.max}")
                    writer.newLine()
                }
            }

        fun load(inputStream: InputStream) = inputStream
            .bufferedReader()
            .use(BufferedReader::readLines)
            .map(String::trim)
            .filter(String::isNotEmpty)
            .map(Companion::parse)

        fun parse(line: String): WavePoint {
            val parts = line.split(' ')
            if (parts.size != 2) {
                throw IllegalArgumentException("Incorrect wave data: $line")
            }
            val low = parts.first().toFloat()
            val high = parts.last().toFloat()
            return create(low, high)
        }
    }
}
