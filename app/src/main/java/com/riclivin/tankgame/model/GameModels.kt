package com.riclivin.tankgame.model

import androidx.compose.ui.geometry.Offset
import kotlin.math.cos
import kotlin.math.sin

enum class TankSide { LEFT, RIGHT }

data class TankState(
    val side: TankSide,
    val x: Float,
    val y: Float,
    val angleDeg: Float,
    val power: Float,
    val health: Float,
    val colorArgb: Int,
    val name: String,
)

data class ProjectileState(
    val position: Offset,
    val velocity: Offset,
    val traveled: List<Offset> = emptyList(),
)

data class ExplosionState(
    val center: Offset,
    val radius: Float,
    val age: Float = 0f,
)

data class WindState(
    val acceleration: Float,
) {
    val label: String
        get() = when {
            acceleration > 10f -> "Vendaval →"
            acceleration > 4f -> "Viento →"
            acceleration > 0.8f -> "Brisa →"
            acceleration < -10f -> "← Vendaval"
            acceleration < -4f -> "← Viento"
            acceleration < -0.8f -> "← Brisa"
            else -> "Calma"
        }
}

data class ShotPlan(
    val angleDeg: Float,
    val power: Float,
)

data class GameUiState(
    val terrain: List<Float>,
    val tanks: List<TankState>,
    val currentTurn: TankSide,
    val wind: WindState,
    val projectile: ProjectileState? = null,
    val explosion: ExplosionState? = null,
    val winner: TankSide? = null,
    val message: String = "",
    val aiEnabled: Boolean = true,
    val round: Int = 1,
)

fun launchVelocity(angleDeg: Float, power: Float, facingRight: Boolean): Offset {
    val radians = Math.toRadians(angleDeg.toDouble())
    val direction = if (facingRight) 1f else -1f
    val vx = (cos(radians) * power * direction).toFloat()
    val vy = (-sin(radians) * power).toFloat()
    return Offset(vx, vy)
}
