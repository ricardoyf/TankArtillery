package com.riclivin.tankgame.engine

import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

object TerrainGenerator {
    fun create(width: Int, height: Int, random: Random): List<Float> {
        val base = height * 0.72f
        val amplitudes = listOf(
            height * 0.08f,
            height * 0.045f,
            height * 0.025f,
        )
        val frequencies = listOf(1.1f, 2.7f, 5.4f)
        val phases = List(3) { random.nextFloat() * (Math.PI * 2).toFloat() }

        val heights = MutableList(width) { x ->
            val nx = x / width.toFloat()
            var y = base
            amplitudes.indices.forEach { index ->
                y -= amplitudes[index] * cos(nx * frequencies[index] * Math.PI * 2 + phases[index]).toFloat()
            }
            y += random.nextFloat() * height * 0.008f
            y.coerceIn(height * 0.42f, height * 0.84f)
        }

        smooth(heights, passes = 10)
        flattenPlateau(heights, width / 6, width / 8)
        flattenPlateau(heights, width - width / 6, width / 8)
        smooth(heights, passes = 3)
        return heights
    }

    private fun smooth(values: MutableList<Float>, passes: Int) {
        repeat(passes) {
            val copy = values.toList()
            for (i in values.indices) {
                val from = max(0, i - 2)
                val to = min(values.lastIndex, i + 2)
                values[i] = (from..to).sumOf { copy[it].toDouble() }.toFloat() / (to - from + 1)
            }
        }
    }

    private fun flattenPlateau(values: MutableList<Float>, center: Int, halfWidth: Int) {
        val from = max(0, center - halfWidth)
        val to = min(values.lastIndex, center + halfWidth)
        val avg = (from..to).sumOf { values[it].toDouble() }.toFloat() / (to - from + 1)
        for (i in from..to) {
            values[i] = avg
        }
    }
}
