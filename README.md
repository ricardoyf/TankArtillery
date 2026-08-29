# Tank Artillery

Juego nativo Android en **Kotlin + Jetpack Compose** tipo Artillery/Tanks.

## Qué incluye

- Terreno procedural distinto en cada partida
- Dos tanques por turnos
- Ajuste de ángulo y fuerza
- Viento variable en cada disparo
- Trayectoria visible del proyectil
- Explosión con daño por proximidad
- Barras de vida
- HUD claro
- Botón de nueva partida
- Modo **IA simple** opcional o **2 jugadores**
- Regeneración automática del escenario al ganar

## Abrir en Android Studio

Abre esta carpeta directamente:

`/home/n95/gDrive/TANK`

## Compilar

```bash
./gradlew assembleDebug
```

## APK debug generado

- `app/build/outputs/apk/debug/app-debug.apk`
- `TankArtillery-debug.apk`

## Estructura principal

- `app/src/main/java/com/riclivin/tankgame/MainActivity.kt` → UI Compose y render del campo de batalla
- `app/src/main/java/com/riclivin/tankgame/GameViewModel.kt` → estado, turnos, física, impacto, victoria
- `app/src/main/java/com/riclivin/tankgame/engine/TerrainGenerator.kt` → terreno procedural
- `app/src/main/java/com/riclivin/tankgame/engine/SimpleAi.kt` → IA básica
- `app/src/main/java/com/riclivin/tankgame/model/GameModels.kt` → modelos de dominio

## Nota

El proyecto usa el SDK local configurado en `local.properties`:

`/home/n95/Android/Sdk`
