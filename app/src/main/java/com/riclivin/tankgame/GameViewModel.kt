package com.riclivin.tankgame

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.riclivin.tankgame.engine.SimpleAi
import com.riclivin.tankgame.engine.TerrainGenerator
import com.riclivin.tankgame.model.ExplosionState
import com.riclivin.tankgame.model.GameUiState
import com.riclivin.tankgame.model.ProjectileState
import com.riclivin.tankgame.model.TankSide
import com.riclivin.tankgame.model.TankState
import com.riclivin.tankgame.model.WindState
import com.riclivin.tankgame.model.launchVelocity
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.random.Random

class GameViewModel : ViewModel() {
    private val random = Random(System.currentTimeMillis())
    private val worldWidth = 1200
    private val worldHeight = 700
    private val gravity = 16f
    private val projectileStep = 0.12f
    private val blastRadius = 72f
    private val maxHealth = 100f

    private val _uiState = MutableStateFlow(createNewGame(round = 1, aiEnabled = true))
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    private var simulationJob: Job? = null

    fun newMatch() {
        simulationJob?.cancel()
        _uiState.value = createNewGame(round = _uiState.value.round + 1, aiEnabled = _uiState.value.aiEnabled)
        maybeTriggerAi()
    }

    fun setAiEnabled(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(aiEnabled = enabled)
        maybeTriggerAi()
    }

    fun adjustAngle(delta: Float) {
        updateActiveTank { copy(angleDeg = (angleDeg + delta).coerceIn(5f, 85f)) }
    }

    fun adjustPower(delta: Float) {
        updateActiveTank { copy(power = (power + delta).coerceIn(20f, 140f)) }
    }

    fun fire() {
        val state = _uiState.value
        if (state.projectile != null || state.winner != null) return

        val shooter = state.tanks.first { it.side == state.currentTurn }
        val angleRad = Math.toRadians(shooter.angleDeg.toDouble())
        val direction = if (shooter.side == TankSide.LEFT) 1f else -1f
        val launchPosition = Offset(
            shooter.x + (kotlin.math.cos(angleRad) * 34.0 * direction).toFloat(),
            shooter.y - 22f - (kotlin.math.sin(angleRad) * 34.0).toFloat()
        )
        val projectile = ProjectileState(
            position = launchPosition,
            velocity = launchVelocity(
                angleDeg = shooter.angleDeg,
                power = shooter.power,
                facingRight = shooter.side == TankSide.LEFT,
            )
        )

        _uiState.value = state.copy(
            projectile = projectile,
            explosion = null,
            message = "${shooter.name} dispara"
        )
        simulateProjectile()
    }

    private fun maybeTriggerAi() {
        val state = _uiState.value
        if (!state.aiEnabled || state.currentTurn != TankSide.RIGHT || state.winner != null || state.projectile != null) return
        simulationJob?.cancel()
        simulationJob = viewModelScope.launch {
            delay(900)
            val aiTank = state.tanks.first { it.side == TankSide.RIGHT }
            val target = state.tanks.first { it.side == TankSide.LEFT }
            val plan = SimpleAi.chooseShot(
                terrain = state.terrain,
                selfX = aiTank.x,
                selfY = aiTank.y,
                targetX = target.x,
                targetY = target.y,
                wind = state.wind.acceleration,
                side = TankSide.RIGHT,
                random = random,
            )
            _uiState.value = _uiState.value.copy(
                tanks = _uiState.value.tanks.map {
                    if (it.side == TankSide.RIGHT) it.copy(angleDeg = plan.angleDeg, power = plan.power) else it
                },
                message = "IA calcula disparo…"
            )
            delay(700)
            fire()
        }
    }

    private fun simulateProjectile() {
        simulationJob?.cancel()
        simulationJob = viewModelScope.launch {
            while (true) {
                val state = _uiState.value
                val projectile = state.projectile ?: break
                var nextVelocity = projectile.velocity
                val nextPosition = projectile.position + projectile.velocity * projectileStep
                nextVelocity = Offset(
                    nextVelocity.x + state.wind.acceleration * projectileStep,
                    nextVelocity.y + gravity * projectileStep,
                )
                val trail = (projectile.traveled + nextPosition).takeLast(40)

                if (nextPosition.x !in 0f..worldWidth.toFloat() || nextPosition.y > worldHeight + 180f) {
                    onImpact(nextPosition.copy(y = worldHeight.toFloat()))
                    break
                }
                val terrainY = state.terrain[nextPosition.x.toInt().coerceIn(0, state.terrain.lastIndex)]
                if (nextPosition.y >= terrainY) {
                    onImpact(nextPosition.copy(y = terrainY))
                    break
                }
                val directHit = state.tanks.any { tank ->
                    val isOwnTankEarly = tank.side == state.currentTurn && projectile.traveled.size < 8
                    !isOwnTankEarly && abs(nextPosition.x - tank.x) < 22f && abs(nextPosition.y - (tank.y - 12f)) < 16f
                }
                if (directHit) {
                    onImpact(nextPosition)
                    break
                }

                _uiState.value = state.copy(projectile = ProjectileState(nextPosition, nextVelocity, trail))
                delay(28)
            }
        }
    }

    private suspend fun onImpact(position: Offset) {
        val state = _uiState.value
        val updatedTanks = state.tanks.map { tank ->
            val distance = hypot((position.x - tank.x).toDouble(), (position.y - (tank.y - 8f)).toDouble()).toFloat()
            val damage = when {
                distance < 18f -> 55f
                distance < blastRadius -> ((blastRadius - distance) / blastRadius) * 48f
                else -> 0f
            }
            tank.copy(health = (tank.health - damage).coerceAtLeast(0f))
        }
        val winner = updatedTanks.firstOrNull { it.health <= 0f }?.let { dead ->
            updatedTanks.first { it.side != dead.side }.side
        }
        _uiState.value = state.copy(
            projectile = null,
            explosion = ExplosionState(position, blastRadius),
            tanks = updatedTanks,
            winner = winner,
            message = winner?.let { if (it == TankSide.LEFT) "Gana Jugador 1" else if (state.aiEnabled) "Gana la IA" else "Gana Jugador 2" }
                ?: "Impacto"
        )
        repeat(18) {
            delay(24)
            _uiState.value = _uiState.value.copy(
                explosion = _uiState.value.explosion?.copy(age = _uiState.value.explosion!!.age + 0.06f)
            )
        }
        if (winner != null) {
            delay(1600)
            _uiState.value = createNewGame(round = state.round + 1, aiEnabled = _uiState.value.aiEnabled)
            maybeTriggerAi()
        } else {
            val nextTurn = if (state.currentTurn == TankSide.LEFT) TankSide.RIGHT else TankSide.LEFT
            _uiState.value = _uiState.value.copy(
                explosion = null,
                currentTurn = nextTurn,
                wind = WindState(randomWind()),
                message = if (nextTurn == TankSide.LEFT) "Turno Jugador 1" else if (_uiState.value.aiEnabled) "Turno IA" else "Turno Jugador 2"
            )
            maybeTriggerAi()
        }
    }

    private fun updateActiveTank(block: TankState.() -> TankState) {
        val state = _uiState.value
        if (state.projectile != null || state.winner != null) return
        if (state.aiEnabled && state.currentTurn == TankSide.RIGHT) return
        _uiState.value = state.copy(
            tanks = state.tanks.map {
                if (it.side == state.currentTurn) it.block() else it
            }
        )
    }

    private fun createNewGame(round: Int, aiEnabled: Boolean): GameUiState {
        val terrain = TerrainGenerator.create(worldWidth, worldHeight, random)
        val leftX = worldWidth * 0.18f
        val rightX = worldWidth * 0.82f
        val leftTank = TankState(
            side = TankSide.LEFT,
            x = leftX,
            y = terrain[leftX.toInt()],
            angleDeg = 48f,
            power = 78f,
            health = maxHealth,
            colorArgb = Color(0xFF4FC3F7).value.toInt(),
            name = "Jugador 1",
        )
        val rightTank = TankState(
            side = TankSide.RIGHT,
            x = rightX,
            y = terrain[rightX.toInt()],
            angleDeg = 42f,
            power = 76f,
            health = maxHealth,
            colorArgb = Color(0xFFFF8A65).value.toInt(),
            name = "IA",
        )
        return GameUiState(
            terrain = terrain,
            tanks = listOf(leftTank, rightTank),
            currentTurn = TankSide.LEFT,
            wind = WindState(randomWind()),
            message = "Turno Jugador 1",
            aiEnabled = aiEnabled,
            round = round,
        )
    }

    private fun randomWind(): Float = random.nextFloat() * 10f - 5f
}

private operator fun Offset.plus(other: Offset): Offset = Offset(x + other.x, y + other.y)
private operator fun Offset.times(scale: Float): Offset = Offset(x * scale, y * scale)
