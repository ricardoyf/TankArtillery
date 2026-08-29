package com.riclivin.tankgame

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.riclivin.tankgame.model.GameUiState
import com.riclivin.tankgame.model.TankSide
import com.riclivin.tankgame.theme.TankArtilleryTheme
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TankArtilleryTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val vm: GameViewModel = viewModel()
                    val state by vm.uiState.collectAsState()
                    GameScreen(
                        state = state,
                        onAngleMinus = { vm.adjustAngle(-2f) },
                        onAnglePlus = { vm.adjustAngle(2f) },
                        onPowerMinus = { vm.adjustPower(-3f) },
                        onPowerPlus = { vm.adjustPower(3f) },
                        onFire = vm::fire,
                        onNewMatch = vm::newMatch,
                        onToggleAi = vm::setAiEnabled,
                    )
                }
            }
        }
    }
}

@Composable
private fun GameScreen(
    state: GameUiState,
    onAngleMinus: () -> Unit,
    onAnglePlus: () -> Unit,
    onPowerMinus: () -> Unit,
    onPowerPlus: () -> Unit,
    onFire: () -> Unit,
    onNewMatch: () -> Unit,
    onToggleAi: (Boolean) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF08111F))
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        TopHud(state, onNewMatch, onToggleAi)
        Card(modifier = Modifier.weight(1f)) {
            Battlefield(state)
        }
        Controls(state, onAngleMinus, onAnglePlus, onPowerMinus, onPowerPlus, onFire)
        Spacer(modifier = Modifier.height(48.dp))
    }
}

@Composable
private fun TopHud(state: GameUiState, onNewMatch: () -> Unit, onToggleAi: (Boolean) -> Unit) {
    Card(shape = MaterialTheme.shapes.medium) {
        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("R${state.round}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    FilterChip(selected = state.aiEnabled, onClick = { onToggleAi(true) }, label = { Text("IA") })
                    FilterChip(selected = !state.aiEnabled, onClick = { onToggleAi(false) }, label = { Text("2P") })
                    Button(onClick = onNewMatch, modifier = Modifier.height(34.dp)) { Text("Nueva") }
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val left = state.tanks.first { it.side == TankSide.LEFT }
                val right = state.tanks.first { it.side == TankSide.RIGHT }
                PlayerPanel(left.name, left.health, Color(0xFF4FC3F7), state.currentTurn == TankSide.LEFT, Modifier.weight(1f))
                PlayerPanel(if (state.aiEnabled) "IA" else right.name.replace("IA", "Jugador 2"), right.health, Color(0xFFFF8A65), state.currentTurn == TankSide.RIGHT, Modifier.weight(1f))
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(state.message, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                WindBadge(state.wind.acceleration)
            }
        }
    }
}

@Composable
private fun PlayerPanel(name: String, health: Float, color: Color, active: Boolean, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(modifier = Modifier.size(10.dp).background(color, MaterialTheme.shapes.small))
                Text(name + if (active) " · turno" else "", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .background(Color(0x33222222), MaterialTheme.shapes.small)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth((health / 100f).coerceIn(0f, 1f))
                        .height(8.dp)
                        .background(color, MaterialTheme.shapes.small)
                )
            }
            Text("${health.toInt()} PV", style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun WindBadge(wind: Float) {
    val arrow = if (wind > 0.4f) "→" else if (wind < -0.4f) "←" else "•"
    Text("Viento $arrow ${"%.1f".format(abs(wind))}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
}

@Composable
private fun Battlefield(state: GameUiState) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val scaleX = size.width / 1200f
        val scaleY = size.height / 700f

        drawRect(
            brush = Brush.verticalGradient(listOf(Color(0xFF15305D), Color(0xFF6EB7FF), Color(0xFFE0F1FF))),
            size = size,
        )

        val terrainPath = Path().apply {
            moveTo(0f, size.height)
            state.terrain.forEachIndexed { index, y ->
                lineTo(index * scaleX, y * scaleY)
            }
            lineTo(size.width, size.height)
            close()
        }
        drawPath(terrainPath, color = Color(0xFF6F8F49))
        drawPath(terrainPath, brush = Brush.verticalGradient(listOf(Color(0xFF8CB75F), Color(0xFF536E32))))

        state.projectile?.traveled?.windowed(2)?.forEach { segment ->
            drawLine(Color.White.copy(alpha = 0.45f), segment[0].scaled(scaleX, scaleY), segment[1].scaled(scaleX, scaleY), strokeWidth = 4f, cap = StrokeCap.Round)
        }

        state.tanks.forEach { tank ->
            val x = tank.x * scaleX
            val y = tank.y * scaleY
            val bodyColor = if (tank.side == TankSide.LEFT) Color(0xFF4FC3F7) else Color(0xFFFF8A65)
            drawRoundRect(bodyColor, topLeft = Offset(x - 24f, y - 20f), size = Size(48f, 20f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f))
            repeat(2) { idx ->
                drawCircle(Color.DarkGray, radius = 7f, center = Offset(x - 12f + idx * 24f, y + 2f))
            }
            val angleRad = Math.toRadians(tank.angleDeg.toDouble())
            val direction = if (tank.side == TankSide.LEFT) 1f else -1f
            val turretEnd = Offset(
                x + (cos(angleRad) * 30.0 * direction).toFloat(),
                y - 16f - (sin(angleRad) * 30.0).toFloat(),
            )
            drawLine(Color(0xFF263238), Offset(x, y - 14f), turretEnd, strokeWidth = 8f, cap = StrokeCap.Round)
        }

        state.projectile?.let {
            drawCircle(Color(0xFFFFF59D), radius = 7f, center = it.position.scaled(scaleX, scaleY))
        }

        state.explosion?.let {
            val progress = it.age.coerceIn(0f, 1f)
            drawCircle(
                color = Color(0xFFFFB300).copy(alpha = 0.45f * (1f - progress)),
                radius = it.radius * scaleX * (0.5f + progress),
                center = it.center.scaled(scaleX, scaleY),
                style = Stroke(width = 16f)
            )
            drawCircle(
                color = Color(0xFFFF7043).copy(alpha = 0.6f * (1f - progress)),
                radius = it.radius * scaleX * 0.45f * (1f + progress),
                center = it.center.scaled(scaleX, scaleY),
            )
        }
    }
}

@Composable
private fun Controls(
    state: GameUiState,
    onAngleMinus: () -> Unit,
    onAnglePlus: () -> Unit,
    onPowerMinus: () -> Unit,
    onPowerPlus: () -> Unit,
    onFire: () -> Unit,
) {
    val activeTank = state.tanks.first { it.side == state.currentTurn }
    val disabled = state.projectile != null || (state.aiEnabled && state.currentTurn == TankSide.RIGHT)

    Card {
        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("A: ${activeTank.angleDeg.toInt()}°", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                Text("F: ${activeTank.power.toInt()}%", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SmallAction("- Ángulo", !disabled, onAngleMinus, Modifier.weight(1f))
                SmallAction("+ Ángulo", !disabled, onAnglePlus, Modifier.weight(1f))
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SmallAction("- Fuerza", !disabled, onPowerMinus, Modifier.weight(1f))
                SmallAction("+ Fuerza", !disabled, onPowerPlus, Modifier.weight(1f))
            }
            Button(onClick = onFire, enabled = !disabled && state.winner == null, modifier = Modifier.fillMaxWidth().height(40.dp)) {
                Text(if (disabled) "Esperando…" else "Disparar")
            }
        }
    }
}

@Composable
private fun SmallAction(text: String, enabled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(onClick = onClick, enabled = enabled, modifier = modifier.height(38.dp)) { Text(text, style = MaterialTheme.typography.labelLarge) }
}

private fun Offset.scaled(scaleX: Float, scaleY: Float): Offset = Offset(x * scaleX, y * scaleY)
