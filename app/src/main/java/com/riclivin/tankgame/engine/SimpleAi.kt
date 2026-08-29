package com.riclivin.tankgame.engine

import com.riclivin.tankgame.model.ShotPlan
import com.riclivin.tankgame.model.TankSide
import com.riclivin.tankgame.model.launchVelocity
import kotlin.math.abs
import kotlin.random.Random

object SimpleAi {
    fun chooseShot(
        terrain: List<Float>,
        selfX: Float,
        selfY: Float,
        targetX: Float,
        targetY: Float,
        wind: Float,
        side: TankSide,
        random: Random,
    ): ShotPlan {
        var best = ShotPlan(angleDeg = 45f, power = 65f)
        var bestScore = Float.MAX_VALUE

        for (angle in 20..80 step 2) {
            for (power in 36..96 step 2) {
                var x = selfX
                var y = selfY - 14f
                val velocity = launchVelocity(angle.toFloat(), power.toFloat(), side == TankSide.LEFT)
                var vx = velocity.x
                var vy = velocity.y
                repeat(240) {
                    x += vx * 0.22f
                    y += vy * 0.22f
                    vx += wind * 0.22f
                    vy += 24f * 0.22f
                    if (x < 0 || x >= terrain.size || y > terrain.size * 2f) return@repeat
                    if (x in 0f..(terrain.lastIndex.toFloat()) && y >= terrain[x.toInt()]) return@repeat
                    val score = abs(x - targetX) + abs(y - targetY) * 0.55f
                    if (score < bestScore) {
                        bestScore = score
                        best = ShotPlan(angle.toFloat(), power.toFloat())
                    }
                }
            }
        }

        val angleNudge = random.nextInt(-3, 4)
        val powerNudge = random.nextInt(-4, 5)
        return best.copy(
            angleDeg = (best.angleDeg + angleNudge).coerceIn(15f, 85f),
            power = (best.power + powerNudge).coerceIn(28f, 100f),
        )
    }
}
