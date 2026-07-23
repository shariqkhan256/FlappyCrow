package com.example.ui.game

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import java.lang.Math.PI
import kotlin.math.*
import kotlin.random.Random

// Obstacle type definition
enum class ObstacleType {
    Chimney,
    PowerLine,
    CastleTower,
    TreeBranch,
    FlyingObstacle
}

// Collectible type definition
enum class CollectibleType {
    Coin,
    Key,
    Gem,
    Watch,
    Star
}

// Game Object Classes
data class Obstacle(
    val id: Int,
    var x: Float,
    val type: ObstacleType,
    val gapY: Float,
    val gapHeight: Float,
    val width: Float = 55f,
    var passed: Boolean = false,
    var phase: Float = Random.nextFloat() * 10f, // For swinging/moving items
    var directionY: Int = if (Random.nextBoolean()) 1 else -1
)

data class Collectible(
    val id: Int,
    var x: Float,
    var y: Float,
    val type: CollectibleType,
    val radius: Float = 12f,
    var collected: Boolean = false,
    var pulsePhase: Float = Random.nextFloat() * 10f
)

data class Particle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    val color: Color,
    var size: Float,
    var alpha: Float = 1f,
    var life: Float = 1f, // 1.0 down to 0.0
    val decay: Float = Random.nextFloat() * 0.04f + 0.02f
)

data class Cloud(
    var x: Float,
    val y: Float,
    val speed: Float,
    val scale: Float
)

data class Star(
    val x: Float,
    val y: Float,
    val size: Float,
    val speed: Float,
    var brightness: Float = Random.nextFloat()
)

data class BackgroundBat(
    var x: Float,
    var y: Float,
    val speed: Float,
    val scale: Float,
    var wingPhase: Float = Random.nextFloat() * 10f
)

@OptIn(ExperimentalTextApi::class)
@Composable
fun FlappyCrowGame(
    selectedAccessory: String,
    highScore: Int = 0,
    isSoundEnabled: Boolean,
    isVibrationEnabled: Boolean,
    onGameOver: (score: Int, coins: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val vibrator = remember { context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator }

    // Game states
    var score by remember { mutableStateOf(0) }
    var coinsCollected by remember { mutableStateOf(0) }
    var isPaused by remember { mutableStateOf(false) }
    var isStarted by remember { mutableStateOf(false) }
    var soundOn by remember(isSoundEnabled) { mutableStateOf(isSoundEnabled) }

    // Virtual Game Screen Dimensions (relative mapping)
    val virtualWidth = 360f
    val virtualHeight = 640f

    // Physics constants
    val gravity = 0.38f
    val flapStrength = -7.0f

    // Player (Coco) state
    var playerY by remember { mutableStateOf(virtualHeight / 2f) }
    var playerVelocity by remember { mutableStateOf(0f) }
    val playerRadius = 16f
    var playerAngle by remember { mutableStateOf(0f) }
    var wingAngle by remember { mutableStateOf(0f) }
    var wingDirection by remember { mutableStateOf(1) } // 1 down, -1 up
    var flapFrameTicks by remember { mutableStateOf(0) } // Sprite-swap / keyframe flap animation timer

    // Animation frame counters
    var gameTicks by remember { mutableStateOf(0L) }

    // Screen shake / Red flash feedback
    var shakeDuration by remember { mutableStateOf(0) }
    var shakeOffsetX by remember { mutableStateOf(0f) }
    var shakeOffsetY by remember { mutableStateOf(0f) }
    var collisionFlash by remember { mutableStateOf(false) }

    // Object lists
    val obstacles = remember { mutableStateListOf<Obstacle>() }
    val collectibles = remember { mutableStateListOf<Collectible>() }
    val particles = remember { mutableStateListOf<Particle>() }
    val clouds = remember { mutableStateListOf<Cloud>() }
    val stars = remember { mutableStateListOf<Star>() }
    val backgroundBats = remember { mutableStateListOf<BackgroundBat>() }
    val buildings = remember { mutableStateListOf<Float>() } // Silhouette heights

    // Initialize environment once
    LaunchedEffect(Unit) {
        // Generate stars
        for (i in 0..25) {
            stars.add(
                Star(
                    x = Random.nextFloat() * virtualWidth,
                    y = Random.nextFloat() * (virtualHeight * 0.6f),
                    size = Random.nextFloat() * 2f + 1f,
                    speed = Random.nextFloat() * 0.05f + 0.01f
                )
            )
        }

        // Generate clouds
        for (i in 0..3) {
            clouds.add(
                Cloud(
                    x = Random.nextFloat() * virtualWidth,
                    y = Random.nextFloat() * 120f + 20f,
                    speed = Random.nextFloat() * 0.15f + 0.05f,
                    scale = Random.nextFloat() * 0.4f + 0.6f
                )
            )
        }

        // Generate building heights for parallax layer
        for (i in 0..12) {
            buildings.add(Random.nextFloat() * 120f + 60f)
        }

        // Generate initial bats
        for (i in 0..2) {
            backgroundBats.add(
                BackgroundBat(
                    x = Random.nextFloat() * virtualWidth + virtualWidth,
                    y = Random.nextFloat() * 200f + 50f,
                    speed = Random.nextFloat() * 0.6f + 0.4f,
                    scale = Random.nextFloat() * 0.3f + 0.3f
                )
            )
        }
    }

    // Vibration trigger
    val triggerVibration: (Long) -> Unit = { duration ->
        if (isVibrationEnabled && vibrator != null) {
            try {
                if (vibrator.hasVibrator()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
                    } else {
                        @Suppress("DEPRECATION")
                        vibrator.vibrate(duration)
                    }
                }
            } catch (e: Exception) {
                // Safeguard against SecurityException or device quirks
                e.printStackTrace()
            }
        }
    }

    // Restart game function
    val restartGame = {
        playerY = virtualHeight / 2f
        playerVelocity = 0f
        score = 0
        coinsCollected = 0
        isPaused = false
        isStarted = false
        shakeDuration = 0
        shakeOffsetX = 0f
        shakeOffsetY = 0f
        collisionFlash = false
        obstacles.clear()
        collectibles.clear()
        particles.clear()
    }

    // Trigger Flap
    val performFlap = {
        if (!isPaused) {
            if (!isStarted) {
                isStarted = true
            }
            playerVelocity = flapStrength
            flapFrameTicks = 14 // Trigger rapid 4-stage keyframe wing flap sequence
            triggerVibration(15)
            RetroAudioEngine.playFlap(soundOn)

            // Spawn cute feather particles trailing behind
            for (i in 0..3) {
                particles.add(
                    Particle(
                        x = 70f, // Behind the body (player is at 80f)
                        y = playerY + Random.nextFloat() * 10f - 5f,
                        vx = -(Random.nextFloat() * 1.5f + 1.0f),
                        vy = Random.nextFloat() * 2.0f - 1.0f,
                        color = if (Random.nextBoolean()) NightPrimary else FeatherHighlight,
                        size = Random.nextFloat() * 4f + 2f
                    )
                )
            }
        }
    }

    // Primary Game Loop
    LaunchedEffect(isPaused, isStarted) {
        if (!isPaused && isStarted) {
            var obstacleIdCounter = 0
            var collectibleIdCounter = 0

            while (true) {
                gameTicks++

                // Update visual counters/phases
                if (shakeDuration > 0) shakeDuration--

                // 1. Update Player Physics
                playerVelocity += gravity
                playerY += playerVelocity
                
                // Keep player within upper/lower bounds
                if (playerY < 10f) {
                    playerY = 10f
                    playerVelocity = 0.2f
                }
                
                // Handle hitting the ground
                if (playerY > virtualHeight - 20f) {
                    collisionFlash = true
                    triggerVibration(150)
                    RetroAudioEngine.playHit(soundOn)

                    // Dynamic 320ms screen shake loop
                    val totalSteps = 20
                    for (step in 1..totalSteps) {
                        val intensity = (1f - (step.toFloat() / totalSteps)) * 14f
                        shakeOffsetX = (Random.nextFloat() * 2f - 1f) * intensity
                        shakeOffsetY = (Random.nextFloat() * 2f - 1f) * intensity
                        delay(16)
                    }
                    shakeOffsetX = 0f
                    shakeOffsetY = 0f

                    onGameOver(score, coinsCollected)
                    break
                }

                // Multi-stage keyframe wing flap animation (sprite-frame sequence)
                if (flapFrameTicks > 0) {
                    flapFrameTicks--
                    // 4-frame keyframe cycle: FRAME_UP (-42°), FRAME_MID_UP (-18°), FRAME_DOWN (38°), FRAME_MID_RECOVERY (8°)
                    val frameIndex = (14 - flapFrameTicks) % 4
                    wingAngle = when (frameIndex) {
                        0 -> -42f // Wings high up on upstroke
                        1 -> -18f // Wings transitioning down
                        2 -> 38f  // Wings down on power flap
                        else -> 8f // Wings recovering to gliding position
                    }
                } else if (playerVelocity < 0) {
                    // Flapping up rapidly
                    wingAngle += 10f * wingDirection
                    if (abs(wingAngle) > 30f) wingDirection *= -1
                } else {
                    // Gliding/falling flight oscillation
                    wingAngle = sin(gameTicks * 0.18f) * 14f
                }

                // Smooth rotation angle based on velocity
                playerAngle = (playerVelocity * 3.5f).coerceIn(-30f, 45f)

                // 2. Parallax elements updating
                // Stars twinkle and scroll
                stars.forEach { star ->
                    star.brightness = 0.3f + 0.7f * sin((gameTicks * star.speed) + star.x).absoluteValue
                }

                // Clouds slow drift
                clouds.forEach { cloud ->
                    cloud.x -= cloud.speed
                    if (cloud.x < -120f) {
                        cloud.x = virtualWidth + 40f
                    }
                }

                // Background bats update
                backgroundBats.forEach { bat ->
                    bat.x -= bat.speed
                    bat.y += sin(gameTicks * 0.05f + bat.wingPhase) * 0.5f
                    bat.wingPhase += 0.2f
                    if (bat.x < -50f) {
                        bat.x = virtualWidth + Random.nextFloat() * 150f + 50f
                        bat.y = Random.nextFloat() * 200f + 50f
                    }
                }

                // 3. Obstacle Generation and Movement
                val gameSpeed = 2.4f + (score * 0.06f).coerceAtMost(1.2f) // Slightly faster as score increases

                // Spawn Obstacle every 110 ticks (~2.5 seconds)
                if (gameTicks % 110L == 0L || obstacles.isEmpty()) {
                    val obsType = ObstacleType.entries[Random.nextInt(ObstacleType.entries.size)]
                    val gapHeight = (145f - (score * 1.5f)).coerceAtLeast(115f)
                    val gapY = Random.nextFloat() * (virtualHeight - gapHeight - 140f) + 70f + gapHeight / 2f
                    
                    obstacles.add(
                        Obstacle(
                            id = obstacleIdCounter++,
                            x = virtualWidth + 60f,
                            type = obsType,
                            gapY = gapY,
                            gapHeight = gapHeight
                        )
                    )

                    // Spawn a collectible with 85% chance in or near the obstacle gap
                    if (Random.nextFloat() < 0.85f) {
                        val colType = CollectibleType.entries[Random.nextInt(CollectibleType.entries.size)]
                        val colY = gapY + (Random.nextFloat() * 40f - 20f)
                        collectibles.add(
                            Collectible(
                                id = collectibleIdCounter++,
                                x = virtualWidth + 60f + Random.nextFloat() * 30f - 15f,
                                y = colY.coerceIn(50f, virtualHeight - 50f),
                                type = colType
                            )
                        )
                    }
                }

                // Update Obstacles
                val obstacleIterator = obstacles.iterator()
                while (obstacleIterator.hasNext()) {
                    val obs = obstacleIterator.next()
                    obs.x -= gameSpeed
                    obs.phase += 0.05f

                    // Score point on successfully passing obstacle
                    if (!obs.passed && obs.x + obs.width / 2f < 80f) {
                        obs.passed = true
                        score++
                        triggerVibration(25)
                        RetroAudioEngine.playPoint(soundOn)
                        
                        // Particle celebration burst when score increases
                        for (i in 1..6) {
                            particles.add(
                                Particle(
                                    x = 80f,
                                    y = playerY,
                                    vx = Random.nextFloat() * 3f - 1.5f,
                                    vy = Random.nextFloat() * 3f - 1.5f,
                                    color = NightSecondary,
                                    size = Random.nextFloat() * 3f + 1.5f
                                )
                            )
                        }
                    }

                    // Remove out of screen obstacles
                    if (obs.x < -80f) {
                        obstacleIterator.remove()
                    }
                }

                // Update Collectibles
                val collectibleIterator = collectibles.iterator()
                while (collectibleIterator.hasNext()) {
                    val col = collectibleIterator.next()
                    col.x -= gameSpeed
                    col.pulsePhase += 0.1f

                    // Collision detection with Coco (the player is at x=80f, y=playerY)
                    val dx = col.x - 80f
                    val dy = col.y - playerY
                    val distance = sqrt(dx * dx + dy * dy)

                    if (!col.collected && distance < (playerRadius + col.radius + 2f)) {
                        col.collected = true
                        
                        // Increment coins based on gemstone value
                        val value = when(col.type) {
                            CollectibleType.Gem -> 3
                            CollectibleType.Watch -> 2
                            else -> 1
                        }
                        coinsCollected += value
                        triggerVibration(30)
                        RetroAudioEngine.playCoin(soundOn)

                        // Gold/Orange sparkles on collect
                        val sparkColor = when(col.type) {
                            CollectibleType.Gem -> Color.Red
                            CollectibleType.Key -> Color.LightGray
                            CollectibleType.Coin -> NightTertiary
                            CollectibleType.Watch -> NightSecondary
                            CollectibleType.Star -> Color.White
                        }
                        for (i in 0..8) {
                            particles.add(
                                Particle(
                                    x = col.x,
                                    y = col.y,
                                    vx = Random.nextFloat() * 4f - 2f,
                                    vy = Random.nextFloat() * 4f - 2f,
                                    color = sparkColor,
                                    size = Random.nextFloat() * 5f + 3f
                                )
                            )
                        }
                    }

                    // Remove collected or off-screen collectibles
                    if (col.collected || col.x < -40f) {
                        collectibleIterator.remove()
                    }
                }

                // 4. Update Particles
                val particleIterator = particles.iterator()
                while (particleIterator.hasNext()) {
                    val p = particleIterator.next()
                    p.x += p.vx
                    p.y += p.vy
                    p.life -= p.decay
                    p.alpha = p.life.coerceIn(0f, 1f)
                    p.size = (p.size - 0.05f).coerceAtLeast(0.1f)
                    if (p.life <= 0f) {
                        particleIterator.remove()
                    }
                }

                // 5. Collision Checks
                // Let's check collisions with each active obstacle
                var hitObstacle = false
                for (obs in obstacles) {
                    val left = obs.x - obs.width / 2f
                    val right = obs.x + obs.width / 2f

                    // Does player overlap horizontally?
                    if (80f + playerRadius > left && 80f - playerRadius < right) {
                        val upperBoundary: Float
                        val lowerBoundary: Float

                        // Calculate gaps and custom shapes
                        when (obs.type) {
                            ObstacleType.PowerLine -> {
                                // Electric lines have wire running between poles
                                // Center is the gap
                                upperBoundary = obs.gapY - obs.gapHeight / 2f
                                lowerBoundary = obs.gapY + obs.gapHeight / 2f
                            }
                            ObstacleType.TreeBranch -> {
                                // Tree branches are twisted and slightly narrower
                                upperBoundary = obs.gapY - obs.gapHeight / 2f
                                lowerBoundary = obs.gapY + obs.gapHeight / 2f
                            }
                            else -> {
                                upperBoundary = obs.gapY - obs.gapHeight / 2f
                                lowerBoundary = obs.gapY + obs.gapHeight / 2f
                            }
                        }

                        // Check collision with upper or lower obstacle bounds
                        if (playerY - playerRadius < upperBoundary || playerY + playerRadius > lowerBoundary) {
                            hitObstacle = true
                            break
                        }
                    }
                }

                if (hitObstacle) {
                    collisionFlash = true
                    triggerVibration(200)
                    RetroAudioEngine.playHit(soundOn)

                    // Dynamic 360ms screen shake loop
                    val totalSteps = 22
                    for (step in 1..totalSteps) {
                        val intensity = (1f - (step.toFloat() / totalSteps)) * 18f
                        shakeOffsetX = (Random.nextFloat() * 2f - 1f) * intensity
                        shakeOffsetY = (Random.nextFloat() * 2f - 1f) * intensity
                        delay(16)
                    }
                    shakeOffsetX = 0f
                    shakeOffsetY = 0f

                    onGameOver(score, coinsCollected)
                    break
                }

                delay(16) // Target ~60 FPS
            }
        }
    }

    // Draw scale calculations
    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("game_canvas_container")
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {
                if (!isPaused) {
                    performFlap()
                }
            }
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .background(NightBackground)
        ) {
            val scaleX = size.width / virtualWidth
            val scaleY = size.height / virtualHeight

            // Apply Screenshake translation
            val shakeX = (shakeOffsetX + (if (shakeDuration > 0) Random.nextInt(-6, 7).toFloat() else 0f)) * scaleX
            val shakeY = (shakeOffsetY + (if (shakeDuration > 0) Random.nextInt(-6, 7).toFloat() else 0f)) * scaleY

            translate(left = shakeX, top = shakeY) {
                // 1. Draw Magical Moonlit Sky background
                drawNightSky(scaleX, scaleY, stars, clouds, buildings, size, gameTicks)

                // 1.5 Draw Background Bats (animated flight path)
                backgroundBats.toList().forEach { bat ->
                    drawBackgroundBat(bat, scaleX, scaleY, gameTicks)
                }

                // 2. Draw Obstacles
                obstacles.toList().forEach { obs ->
                    drawObstacle(obs, scaleX, scaleY, virtualHeight, gameTicks)
                }

                // 3. Draw Collectibles
                collectibles.toList().forEach { col ->
                    drawCollectible(col, scaleX, scaleY)
                }

                // 4. Draw Particles
                particles.toList().forEach { p ->
                    drawCircle(
                        color = p.color.copy(alpha = p.alpha),
                        radius = p.size * scaleX,
                        center = Offset(p.x * scaleX, p.y * scaleY)
                    )
                }

                // 5. Draw Coco the Crow (Player)
                drawPlayer(
                    playerY = playerY,
                    playerAngle = playerAngle,
                    wingAngle = wingAngle,
                    selectedAccessory = selectedAccessory,
                    playerRadius = playerRadius,
                    scaleX = scaleX,
                    scaleY = scaleY,
                    gameTicks = gameTicks
                )

                // 6. Draw Red Collision Flash Overlay
                if (collisionFlash && (shakeOffsetX != 0f || shakeOffsetY != 0f || shakeDuration > 0)) {
                    drawRect(
                        color = Color.Red.copy(alpha = 0.35f),
                        topLeft = Offset.Zero,
                        size = size
                    )
                }
            }
        }

        // --- Visual overlays: Game HUD ---
        if (!isStarted) {
            // Elegant glowing hint to tap
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        text = "FLAP COCO",
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = NightPrimary,
                            letterSpacing = 4.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "TAP SCREEN TO FLY",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = NightSecondary
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Avoid Chimneys, Lines, & Branches!",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = NightOnSurface.copy(alpha = 0.8f)
                        )
                    )
                }
            }
        }

        // HUD Dashboard
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Score & Best floating card with neon borders
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(Navy2Color.copy(alpha = 0.75f))
                    .border(BorderStroke(1.dp, AmethystColor.copy(alpha = 0.4f)), RoundedCornerShape(16.dp))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SCORE ",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = AmethystColor,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    )
                    Text(
                        text = "$score",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = CyanColor,
                            fontWeight = FontWeight.Black
                        )
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Text(
                        text = "BEST ",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = DimColor,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    )
                    Text(
                        text = "${max(score, highScore)}",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = GoldColor,
                            fontWeight = FontWeight.Black
                        )
                    )
                }
            }

            // Right: Coins floating pill + Pause Control
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Coins pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Navy2Color.copy(alpha = 0.75f))
                        .border(BorderStroke(1.dp, GoldColor.copy(alpha = 0.3f)), RoundedCornerShape(12.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(GoldColor)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "$coinsCollected",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = GoldColor,
                                fontWeight = FontWeight.Black
                            )
                        )
                    }
                }

                // Pause Button
                IconButton(
                    onClick = {
                        RetroAudioEngine.playButtonClick(soundOn)
                        isPaused = !isPaused
                    },
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Navy2Color.copy(alpha = 0.85f))
                        .border(BorderStroke(1.5.dp, AmethystColor.copy(alpha = 0.6f)), CircleShape)
                        .size(44.dp)
                        .testTag("pause_button")
                ) {
                    Icon(
                        imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                        contentDescription = "Pause Game",
                        tint = CyanColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        // Overlay Pause Menu
        if (isPaused) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.72f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { /* Swallow touch events */ },
                contentAlignment = Alignment.Center
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Navy2Color),
                    modifier = Modifier
                        .width(300.dp)
                        .testTag("pause_menu_card"),
                    shape = RoundedCornerShape(28.dp),
                    border = BorderStroke(2.dp, Brush.horizontalGradient(listOf(CyanColor, AmethystColor)))
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        // Pause Badge Icon
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(AmethystColor.copy(alpha = 0.25f))
                                .border(BorderStroke(1.5.dp, AmethystColor), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Pause,
                                contentDescription = null,
                                tint = CyanColor,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "GAME PAUSED",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Black,
                                color = MoonColor,
                                letterSpacing = 1.2.sp
                            )
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Stats Summary Row (Score & Coins)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(NavyColor.copy(alpha = 0.8f))
                                .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)), RoundedCornerShape(16.dp))
                                .padding(vertical = 12.dp, horizontal = 16.dp),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "SCORE",
                                    style = MaterialTheme.typography.labelSmall.copy(color = DimColor, fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "$score",
                                    style = MaterialTheme.typography.titleMedium.copy(color = MoonColor, fontWeight = FontWeight.Black)
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(24.dp)
                                    .background(Color.White.copy(alpha = 0.15f))
                            )

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "COINS",
                                    style = MaterialTheme.typography.labelSmall.copy(color = DimColor, fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "$coinsCollected",
                                    style = MaterialTheme.typography.titleMedium.copy(color = GoldColor, fontWeight = FontWeight.Black)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Sound Toggle Option
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable {
                                    soundOn = !soundOn
                                    RetroAudioEngine.playButtonClick(soundOn)
                                }
                                .background(Color.White.copy(alpha = 0.06f))
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (soundOn) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                                    contentDescription = "Sound Toggle",
                                    tint = if (soundOn) CyanColor else DimColor,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Sound Effects",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = MoonColor,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                            }
                            Switch(
                                checked = soundOn,
                                onCheckedChange = {
                                    soundOn = it
                                    RetroAudioEngine.playButtonClick(soundOn)
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = CyanColor,
                                    uncheckedThumbColor = DimColor,
                                    uncheckedTrackColor = Color.Gray.copy(alpha = 0.3f)
                                ),
                                modifier = Modifier.height(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Action Buttons: Resume & Restart
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = {
                                    RetroAudioEngine.playButtonClick(soundOn)
                                    isPaused = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MagentaColor),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("resume_button"),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = MoonColor,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "RESUME GAME",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = MoonColor,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp
                                    )
                                )
                            }

                            OutlinedButton(
                                onClick = {
                                    RetroAudioEngine.playButtonClick(soundOn)
                                    restartGame()
                                },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MoonColor),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("restart_button"),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    tint = DimColor,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "RESTART",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = MoonColor,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// Draw the magical midnight space with full depth
private fun DrawScope.drawNightSky(
    scaleX: Float,
    scaleY: Float,
    stars: List<Star>,
    clouds: List<Cloud>,
    buildings: List<Float>,
    size: Size,
    gameTicks: Long
) {
    // 1. Draw smooth vertical pastel night sky gradient matching the poster
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color(0xFF3D2766), // Soft lavender purple top sky
                Color(0xFF261947), // Deep indigo mid sky
                Color(0xFF140C2C)  // Midnight violet horizon
            )
        ),
        topLeft = Offset.Zero,
        size = size
    )

    // 2. Draw Twinkling Stars & Diamond Sparkle Stars
    stars.toList().forEachIndexed { index, star ->
        val starX = star.x * scaleX
        val starY = star.y * scaleY
        val starRadius = star.size * scaleX

        if (index % 4 == 0) {
            // Diamond 4-point sparkle star
            val sparkleSize = starRadius * 2.2f * star.brightness
            val starPath = Path().apply {
                moveTo(starX, starY - sparkleSize)
                quadraticTo(starX, starY, starX + sparkleSize, starY)
                quadraticTo(starX, starY, starX, starY + sparkleSize)
                quadraticTo(starX, starY, starX - sparkleSize, starY)
                quadraticTo(starX, starY, starX, starY - sparkleSize)
                close()
            }
            drawPath(starPath, color = Color(0xFFE1D5FF).copy(alpha = star.brightness.coerceAtMost(0.9f)))
        } else {
            // Soft round star
            drawCircle(
                color = Color.White.copy(alpha = star.brightness),
                radius = starRadius,
                center = Offset(starX, starY)
            )
        }
    }

    // 3. Draw Huge Friendly Moon with Soft Layered Glowing Halos
    val moonCenterX = size.width * 0.78f
    val moonCenterY = size.height * 0.16f
    val moonRadius = 48f * scaleX

    // Concentric soft golden glow rings
    drawCircle(
        color = Color(0xFFFFF59D).copy(alpha = 0.08f),
        radius = moonRadius * 2.1f,
        center = Offset(moonCenterX, moonCenterY)
    )
    drawCircle(
        color = Color(0xFFFFF59D).copy(alpha = 0.15f),
        radius = moonRadius * 1.5f,
        center = Offset(moonCenterX, moonCenterY)
    )
    drawCircle(
        color = Color(0xFFFFF59D).copy(alpha = 0.28f),
        radius = moonRadius * 1.18f,
        center = Offset(moonCenterX, moonCenterY)
    )

    // Main Moon Core Disc with soft radial gradient
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFFFFFDE7), // Luminous cream center
                Color(0xFFFFF59D), // Soft pastel yellow
                Color(0xFFFFD54F)  // Warm golden edge
            ),
            center = Offset(moonCenterX - moonRadius * 0.2f, moonCenterY - moonRadius * 0.2f),
            radius = moonRadius * 1.2f
        ),
        radius = moonRadius,
        center = Offset(moonCenterX, moonCenterY)
    )

    // Soft crater details on the moon
    drawCircle(
        color = Color(0xFFFBC02D).copy(alpha = 0.35f),
        radius = moonRadius * 0.22f,
        center = Offset(moonCenterX - moonRadius * 0.32f, moonCenterY - moonRadius * 0.18f)
    )
    drawCircle(
        color = Color(0xFFFBC02D).copy(alpha = 0.3f),
        radius = moonRadius * 0.16f,
        center = Offset(moonCenterX + moonRadius * 0.25f, moonCenterY + moonRadius * 0.32f)
    )

    // 4. Draw Drifting Puffy Clouds
    clouds.toList().forEach { cloud ->
        val cloudX = cloud.x * scaleX
        val cloudY = cloud.y * scaleY
        val cloudR = 28f * scaleX * cloud.scale

        // Cloud body gradient
        val cloudBrush = Brush.verticalGradient(
            colors = listOf(
                Color(0xFF7E57C2).copy(alpha = 0.35f),
                Color(0xFF38235C).copy(alpha = 0.45f)
            ),
            startY = cloudY - cloudR,
            endY = cloudY + cloudR
        )

        drawCircle(
            brush = cloudBrush,
            radius = cloudR,
            center = Offset(cloudX, cloudY)
        )
        drawCircle(
            brush = cloudBrush,
            radius = cloudR * 0.78f,
            center = Offset(cloudX - cloudR * 0.62f, cloudY + cloudR * 0.12f)
        )
        drawCircle(
            brush = cloudBrush,
            radius = cloudR * 0.78f,
            center = Offset(cloudX + cloudR * 0.62f, cloudY + cloudR * 0.12f)
        )
    }

    // 5. Parallax City Silhouette with Warm Glowing Windows
    val buildingWidth = size.width / 10f
    for (i in 0..10) {
        val buildingHeight = (buildings.getOrNull(i) ?: 100f) * scaleY
        val left = i * buildingWidth
        val top = size.height - buildingHeight

        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF281C4A).copy(alpha = 0.85f),
                    Color(0xFF160E2E).copy(alpha = 0.95f)
                ),
                startY = top,
                endY = size.height
            ),
            topLeft = Offset(left, top),
            size = Size(buildingWidth + 2f, buildingHeight)
        )

        // Building rooftop stroke accent
        drawLine(
            color = Color(0xFF80DEEA).copy(alpha = 0.35f),
            start = Offset(left, top),
            end = Offset(left + buildingWidth + 2f, top),
            strokeWidth = 2f * scaleX
        )

        // Cozy glowing warm orange/yellow windows
        if (i % 2 == 0) {
            val winSize = 5f * scaleX
            val winX = left + buildingWidth * 0.38f
            val winY = top + buildingHeight * 0.28f

            drawCircle(
                color = Color(0xFFFFB300).copy(alpha = 0.3f),
                radius = winSize * 2.2f,
                center = Offset(winX + winSize / 2f, winY + winSize)
            )
            drawRoundRect(
                color = Color(0xFFFFD54F),
                topLeft = Offset(winX, winY),
                size = Size(winSize, winSize * 1.6f),
                cornerRadius = CornerRadius(2f * scaleX)
            )

            drawRoundRect(
                color = Color(0xFFFFD54F),
                topLeft = Offset(winX + 13f * scaleX, winY + 18f * scaleY),
                size = Size(winSize, winSize * 1.6f),
                cornerRadius = CornerRadius(2f * scaleX)
            )
        }
    }
}

// Draw the customized obstacles (3D Vibrant Cartoon Pipes/Pillars matching poster aesthetic)
private fun DrawScope.drawObstacle(
    obs: Obstacle,
    scaleX: Float,
    scaleY: Float,
    virtualHeight: Float,
    gameTicks: Long
) {
    val left = (obs.x - obs.width / 2f) * scaleX
    val right = (obs.x + obs.width / 2f) * scaleX
    val width = obs.width * scaleX
    val gapTop = (obs.gapY - obs.gapHeight / 2f) * scaleY
    val gapBottom = (obs.gapY + obs.gapHeight / 2f) * scaleY

    when (obs.type) {
        ObstacleType.Chimney -> {
            // --- 3D VIBRANT CARTOON PILLARS (Pipes matching poster) ---
            val capExtra = 8f * scaleX
            val capHeight = 22f * scaleY

            // Pillar 3D Body Gradient (Dark shadow -> Rich purple/indigo -> Bright sheen -> Dark shadow)
            val pillarBrush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFF221542), // Dark left shadow
                    Color(0xFF5E35B1), // Vibrant indigo-purple mid
                    Color(0xFF8E62D0), // Bright highlight stripe
                    Color(0xFF4527A0), // Deep purple right
                    Color(0xFF190F30)  // Dark right shadow edge
                ),
                start = Offset(left, 0f),
                end = Offset(right, 0f)
            )

            val capBrush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFF2A1B50),
                    Color(0xFF7E57C2),
                    Color(0xFFB388FF),
                    Color(0xFF512DA8),
                    Color(0xFF1F133B)
                ),
                start = Offset(left - capExtra, 0f),
                end = Offset(right + capExtra, 0f)
            )

            // --- TOP PILLAR ---
            // Main body
            drawRect(
                brush = pillarBrush,
                topLeft = Offset(left, 0f),
                size = Size(width, gapTop)
            )
            // Left gloss sheen line
            drawLine(
                color = Color.White.copy(alpha = 0.35f),
                start = Offset(left + width * 0.28f, 0f),
                end = Offset(left + width * 0.28f, gapTop - capHeight),
                strokeWidth = 2.5f * scaleX
            )
            // Brick grid lines
            for (yStep in 30..gapTop.toInt() step 32) {
                drawLine(
                    color = Color(0xFFB388FF).copy(alpha = 0.25f),
                    start = Offset(left + 2f * scaleX, yStep * scaleY),
                    end = Offset(right - 2f * scaleX, yStep * scaleY),
                    strokeWidth = 1.2f * scaleX
                )
            }
            // 3D Cap Collar
            drawRoundRect(
                brush = capBrush,
                topLeft = Offset(left - capExtra, gapTop - capHeight),
                size = Size(width + capExtra * 2f, capHeight),
                cornerRadius = CornerRadius(6f * scaleX)
            )
            // Glowing cyan rim trim along gap edge
            drawRoundRect(
                color = Color(0xFF80DEEA),
                topLeft = Offset(left - capExtra, gapTop - 3.5f * scaleY),
                size = Size(width + capExtra * 2f, 3.5f * scaleY),
                cornerRadius = CornerRadius(2f * scaleX)
            )
            // White highlight stroke on top of cap
            drawLine(
                color = Color.White.copy(alpha = 0.6f),
                start = Offset(left - capExtra + 4f * scaleX, gapTop - capHeight + 2f * scaleY),
                end = Offset(right + capExtra - 4f * scaleX, gapTop - capHeight + 2f * scaleY),
                strokeWidth = 2f * scaleX,
                cap = StrokeCap.Round
            )

            // --- BOTTOM PILLAR ---
            val bottomHeight = size.height - gapBottom
            // Main body
            drawRect(
                brush = pillarBrush,
                topLeft = Offset(left, gapBottom),
                size = Size(width, bottomHeight)
            )
            // Left gloss sheen line
            drawLine(
                color = Color.White.copy(alpha = 0.35f),
                start = Offset(left + width * 0.28f, gapBottom + capHeight),
                end = Offset(left + width * 0.28f, size.height),
                strokeWidth = 2.5f * scaleX
            )
            // Brick grid lines
            for (yStep in gapBottom.toInt() + 32..size.height.toInt() step 32) {
                drawLine(
                    color = Color(0xFFB388FF).copy(alpha = 0.25f),
                    start = Offset(left + 2f * scaleX, yStep.toFloat()),
                    end = Offset(right - 2f * scaleX, yStep.toFloat()),
                    strokeWidth = 1.2f * scaleX
                )
            }
            // 3D Cap Collar
            drawRoundRect(
                brush = capBrush,
                topLeft = Offset(left - capExtra, gapBottom),
                size = Size(width + capExtra * 2f, capHeight),
                cornerRadius = CornerRadius(6f * scaleX)
            )
            // Glowing cyan rim trim along gap edge
            drawRoundRect(
                color = Color(0xFF80DEEA),
                topLeft = Offset(left - capExtra, gapBottom),
                size = Size(width + capExtra * 2f, 3.5f * scaleY),
                cornerRadius = CornerRadius(2f * scaleX)
            )
            // White highlight stroke on bottom edge of cap
            drawLine(
                color = Color.White.copy(alpha = 0.6f),
                start = Offset(left - capExtra + 4f * scaleX, gapBottom + capHeight - 2f * scaleY),
                end = Offset(right + capExtra - 4f * scaleX, gapBottom + capHeight - 2f * scaleY),
                strokeWidth = 2f * scaleX,
                cap = StrokeCap.Round
            )
        }

        ObstacleType.PowerLine -> {
            // Draw 3D wooden utility poles with metallic crossbeams
            val poleWidth = 16f * scaleX
            val poleBrush = Brush.linearGradient(
                colors = listOf(Color(0xFF2C1A11), Color(0xFF5D4037), Color(0xFF8D6E63), Color(0xFF3E2723)),
                start = Offset((obs.x - 8f) * scaleX, 0f),
                end = Offset((obs.x + 8f) * scaleX, 0f)
            )

            drawRect(
                brush = poleBrush,
                topLeft = Offset((obs.x - 8f) * scaleX, 0f),
                size = Size(poleWidth, gapTop)
            )
            drawRect(
                brush = poleBrush,
                topLeft = Offset((obs.x - 8f) * scaleX, gapBottom),
                size = Size(poleWidth, size.height - gapBottom)
            )

            // Metallic Crossbeams
            val beamHeight = 12f * scaleY
            val beamWidth = 48f * scaleX
            val beamBrush = Brush.verticalGradient(
                colors = listOf(Color(0xFF8D6E63), Color(0xFF4E342E))
            )

            drawRoundRect(
                brush = beamBrush,
                topLeft = Offset((obs.x - 24f) * scaleX, gapTop - beamHeight - 12f * scaleY),
                size = Size(beamWidth, beamHeight),
                cornerRadius = CornerRadius(3f * scaleX)
            )
            drawRoundRect(
                brush = beamBrush,
                topLeft = Offset((obs.x - 24f) * scaleX, gapBottom + 12f * scaleY),
                size = Size(beamWidth, beamHeight),
                cornerRadius = CornerRadius(3f * scaleX)
            )

            // Ceramic insulators with electric spark animations
            drawCircle(color = Color(0xFF80DEEA), radius = 5f * scaleX, center = Offset((obs.x - 18f) * scaleX, gapTop - 18f * scaleY))
            drawCircle(color = Color(0xFF80DEEA), radius = 5f * scaleX, center = Offset((obs.x + 18f) * scaleX, gapTop - 18f * scaleY))
            drawCircle(color = Color(0xFF80DEEA), radius = 5f * scaleX, center = Offset((obs.x - 18f) * scaleX, gapBottom + 18f * scaleY))
            drawCircle(color = Color(0xFF80DEEA), radius = 5f * scaleX, center = Offset((obs.x + 18f) * scaleX, gapBottom + 18f * scaleY))

            val showSpark = (gameTicks % 12 < 6)
            if (showSpark) {
                drawCircle(color = Color(0xFFFFE082), radius = 7f * scaleX, center = Offset(obs.x * scaleX, gapTop - 6f * scaleY))
                drawCircle(color = Color(0xFFFFE082), radius = 7f * scaleX, center = Offset(obs.x * scaleX, gapBottom + 6f * scaleY))
            }
        }

        ObstacleType.CastleTower -> {
            // Fantasy 3D Stone Towers
            val capExtra = 6f * scaleX
            val battleHeight = 22f * scaleY

            val stoneBrush = Brush.linearGradient(
                colors = listOf(Color(0xFF28183D), Color(0xFF4A2C6D), Color(0xFF6A4093), Color(0xFF311B4E)),
                start = Offset(left, 0f),
                end = Offset(right, 0f)
            )

            // TOP TOWER
            drawRect(brush = stoneBrush, topLeft = Offset(left, 0f), size = Size(width, gapTop))
            drawRoundRect(
                brush = Brush.verticalGradient(listOf(Color(0xFFFFD54F), Color(0xFFFF8F00))),
                topLeft = Offset(left - capExtra, gapTop - battleHeight),
                size = Size(width + capExtra * 2f, battleHeight),
                cornerRadius = CornerRadius(4f * scaleX)
            )

            // BOTTOM TOWER
            drawRect(brush = stoneBrush, topLeft = Offset(left, gapBottom), size = Size(width, size.height - gapBottom))
            drawRoundRect(
                brush = Brush.verticalGradient(listOf(Color(0xFFFFD54F), Color(0xFFFF8F00))),
                topLeft = Offset(left - capExtra, gapBottom),
                size = Size(width + capExtra * 2f, battleHeight),
                cornerRadius = CornerRadius(4f * scaleX)
            )

            // Waving Red/Gold Flag
            val flagPhase = sin(gameTicks * 0.22f) * 7f
            val poleX = obs.x * scaleX
            val poleY = gapBottom + 35f * scaleY

            drawLine(
                color = Color.White,
                start = Offset(poleX, poleY),
                end = Offset(poleX + 14f * scaleX, poleY),
                strokeWidth = 2.5f * scaleX
            )
            val flagPath = Path().apply {
                moveTo(poleX + 14f * scaleX, poleY - 7f * scaleY)
                lineTo(poleX + 34f * scaleX + flagPhase * scaleX, poleY)
                lineTo(poleX + 14f * scaleX, poleY + 7f * scaleY)
                close()
            }
            drawPath(flagPath, color = Color(0xFFFF1744))
        }

        ObstacleType.TreeBranch -> {
            // Gnarled cartoon tree trunks
            val trunkBrush = Brush.linearGradient(
                colors = listOf(Color(0xFF211003), Color(0xFF4E2A0C), Color(0xFF7A4519), Color(0xFF361C07)),
                start = Offset(left, 0f),
                end = Offset(right, 0f)
            )

            val topPath = Path().apply {
                moveTo(left + width * 0.4f, 0f)
                quadraticTo(obs.x * scaleX, gapTop * 0.5f, obs.x * scaleX, gapTop)
                lineTo((obs.x - 12f) * scaleX, gapTop)
                quadraticTo((obs.x - 22f) * scaleX, gapTop * 0.5f, left, 0f)
                close()
            }
            drawPath(topPath, brush = trunkBrush)

            val bottomPath = Path().apply {
                moveTo(obs.x * scaleX, gapBottom)
                quadraticTo((obs.x + 12f) * scaleX, gapBottom + (size.height - gapBottom) * 0.5f, right, size.height)
                lineTo(left, size.height)
                quadraticTo((obs.x - 12f) * scaleX, gapBottom + (size.height - gapBottom) * 0.5f, (obs.x - 16f) * scaleX, gapBottom)
                close()
            }
            drawPath(bottomPath, brush = trunkBrush)

            // Leaf bunches
            drawCircle(color = Color(0xFF66BB6A), radius = 12f * scaleX, center = Offset(obs.x * scaleX, gapTop - 10f * scaleY))
            drawCircle(color = Color(0xFF66BB6A), radius = 12f * scaleX, center = Offset(obs.x * scaleX, gapBottom + 10f * scaleY))
        }

        ObstacleType.FlyingObstacle -> {
            // High-tech Cyber Pillars
            val cyberBrush = Brush.linearGradient(
                colors = listOf(Color(0xFF10002B), Color(0xFF3A0CA3), Color(0xFF7209B7), Color(0xFF180138)),
                start = Offset(left, 0f),
                end = Offset(right, 0f)
            )

            drawRect(brush = cyberBrush, topLeft = Offset(left, 0f), size = Size(width, gapTop))
            drawRect(brush = cyberBrush, topLeft = Offset(left, gapBottom), size = Size(width, size.height - gapBottom))

            val lightColor = if (gameTicks % 16 < 8) Color(0xFFFF0055) else Color(0xFF00F5D4)
            drawCircle(color = lightColor, radius = 6f * scaleX, center = Offset(obs.x * scaleX, gapTop - 10f * scaleY))
            drawCircle(color = lightColor, radius = 6f * scaleX, center = Offset(obs.x * scaleX, gapBottom + 10f * scaleY))
        }
    }
}

// Draw collectibles (Coin, Key, Gem, Watch, Star) with beautiful sparkling accents
private fun DrawScope.drawCollectible(col: Collectible, scaleX: Float, scaleY: Float) {
    val colX = col.x * scaleX
    val colY = col.y * scaleY
    val radius = col.radius * scaleX

    // Floating offset
    val floatOffset = sin(col.pulsePhase) * 3f * scaleY
    val finalY = colY + floatOffset

    when (col.type) {
        CollectibleType.Coin -> {
            // Gold Coin
            // Golden halo glow
            drawCircle(
                color = NightTertiary.copy(alpha = 0.25f),
                radius = radius + 4f * scaleX,
                center = Offset(colX, finalY)
            )
            // Coin core
            drawCircle(
                color = NightBeakGold,
                radius = radius,
                center = Offset(colX, finalY)
            )
            // Inner rim detail
            drawCircle(
                color = Color(0xFFB8860B),
                radius = radius * 0.7f,
                center = Offset(colX, finalY),
                style = Stroke(width = 1.5f * scaleX)
            )
        }

        CollectibleType.Key -> {
            // Silver Key
            val keyColor = Color(0xFFCFD8DC)
            // Head ring
            drawCircle(
                color = keyColor,
                radius = radius * 0.5f,
                center = Offset(colX, finalY - radius * 0.3f),
                style = Stroke(width = 2.5f * scaleX)
            )
            // Shaft
            drawLine(
                color = keyColor,
                start = Offset(colX, finalY + radius * 0.1f),
                end = Offset(colX, finalY + radius * 1.1f),
                strokeWidth = 3f * scaleX
            )
            // Bit
            drawRect(
                color = keyColor,
                topLeft = Offset(colX, finalY + radius * 0.7f),
                size = Size(radius * 0.4f, radius * 0.3f)
            )
        }

        CollectibleType.Gem -> {
            // Ruby Red Gem
            val gemPath = Path().apply {
                moveTo(colX, finalY - radius)
                lineTo(colX + radius * 0.8f, finalY - radius * 0.3f)
                lineTo(colX + radius * 0.5f, finalY + radius * 0.8f)
                lineTo(colX, finalY + radius)
                lineTo(colX - radius * 0.5f, finalY + radius * 0.8f)
                lineTo(colX - radius * 0.8f, finalY - radius * 0.3f)
                close()
            }
            // Glow
            drawCircle(
                color = Color.Red.copy(alpha = 0.2f),
                radius = radius + 5f * scaleX,
                center = Offset(colX, finalY)
            )
            drawPath(gemPath, color = Color(0xFFFF2E63))
            // Gem highlight reflection
            drawCircle(
                color = SparkleWhite.copy(alpha = 0.7f),
                radius = radius * 0.2f,
                center = Offset(colX - radius * 0.2f, finalY - radius * 0.4f)
            )
        }

        CollectibleType.Watch -> {
            // Pocket Watch
            val bronze = Color(0xFFCD7F32)
            // Outer casing
            drawCircle(
                color = bronze,
                radius = radius,
                center = Offset(colX, finalY)
            )
            // White face
            drawCircle(
                color = Color.White,
                radius = radius * 0.7f,
                center = Offset(colX, finalY)
            )
            // Hands
            drawLine(
                color = Color.Black,
                start = Offset(colX, finalY),
                end = Offset(colX + radius * 0.4f, finalY),
                strokeWidth = 1.5f * scaleX
            )
            drawLine(
                color = Color.Black,
                start = Offset(colX, finalY),
                end = Offset(colX, finalY - radius * 0.5f),
                strokeWidth = 1.5f * scaleX
            )
        }

        CollectibleType.Star -> {
            // Sparkling Gold Star
            val starPath = Path().apply {
                for (i in 0..4) {
                    val angle1 = i * 2 * PI / 5 - PI / 2
                    val angle2 = angle1 + PI / 5
                    
                    lineTo(
                        (colX + cos(angle1) * radius).toFloat(),
                        (finalY + sin(angle1) * radius).toFloat()
                    )
                    lineTo(
                        (colX + cos(angle2) * (radius * 0.4f)).toFloat(),
                        (finalY + sin(angle2) * (radius * 0.4f)).toFloat()
                    )
                }
                close()
            }
            drawCircle(
                color = Color.Yellow.copy(alpha = 0.2f),
                radius = radius + 6f * scaleX,
                center = Offset(colX, finalY)
            )
            drawPath(starPath, color = Color(0xFFFFEB3B))
        }
    }
}

// Draw Coco the Crow beautifully with customized accessories layered on top
private fun DrawScope.drawPlayer(
    playerY: Float,
    playerAngle: Float,
    wingAngle: Float,
    selectedAccessory: String,
    playerRadius: Float,
    scaleX: Float,
    scaleY: Float,
    gameTicks: Long
) {
    val px = 80f * scaleX
    val py = playerY * scaleY
    val r = playerRadius * scaleX

    // Rotate player based on velocity angle
    rotate(degrees = playerAngle, pivot = Offset(px, py)) {

        // 0. Outer Soft Pastel Aura
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFB388FF).copy(alpha = 0.22f), // Soft lavender glow
                    Color(0xFF80DEEA).copy(alpha = 0.15f), // Soft cyan glow
                    Color.Transparent
                ),
                center = Offset(px, py),
                radius = r * 1.85f
            ),
            radius = r * 1.85f,
            center = Offset(px, py)
        )

        // 1. Chubby Rounded Tail Feathers
        val tailSway = sin(gameTicks * 0.12f) * 3f
        for (i in -1..1) {
            val spreadAngle = i * 14f + tailSway
            rotate(degrees = spreadAngle, pivot = Offset(px - r * 0.7f, py + r * 0.1f)) {
                val tailPath = Path().apply {
                    moveTo(px - r * 0.6f, py + r * 0.1f)
                    quadraticTo(
                        px - r * 1.4f, py - r * 0.2f,
                        px - r * 1.6f, py + r * 0.1f
                    )
                    quadraticTo(
                        px - r * 1.4f, py + r * 0.4f,
                        px - r * 0.6f, py + r * 0.1f
                    )
                    close()
                }
                drawPath(
                    tailPath,
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFF281E45), Color(0xFF3D2E60)),
                        start = Offset(px - r * 0.6f, py),
                        end = Offset(px - r * 1.6f, py)
                    )
                )
                drawPath(
                    tailPath,
                    color = Color(0xFFB388FF).copy(alpha = 0.5f),
                    style = Stroke(width = 1.6f * scaleX)
                )
            }
        }

        // 2. Cute Rounded Orange Feet
        val feetColor = Color(0xFFFF9800)
        drawCircle(color = feetColor, radius = r * 0.14f, center = Offset(px - r * 0.18f, py + r * 0.95f))
        drawCircle(color = feetColor, radius = r * 0.14f, center = Offset(px + r * 0.18f, py + r * 0.95f))

        // 3. Ultra-Cute Chubby Round Body (Soft midnight-indigo baby bird silhouette matching icon)
        val bodyPath = Path().apply {
            moveTo(px + r * 0.2f, py - r * 0.92f) // Head top
            quadraticTo(
                px + r * 0.85f, py - r * 0.4f, // Forehead / nose bridge
                px + r * 0.82f, py - r * 0.15f
            )
            quadraticTo(
                px + r * 1.05f, py + r * 0.45f, // Cute chubby tummy bump!
                px + r * 0.35f, py + r * 0.96f
            )
            quadraticTo(
                px - r * 0.7f, py + r * 1.02f, // Bottom rounded curve
                px - r * 0.92f, py + r * 0.35f
            )
            quadraticTo(
                px - r * 0.88f, py - r * 0.65f, // Round back
                px + r * 0.2f, py - r * 0.92f
            )
            close()
        }

        // Soft gradient fill (3D plush toy aesthetic)
        drawPath(
            bodyPath,
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF4C3973), // Soft glowing lavender-indigo center
                    Color(0xFF2D214F), // Mid indigo
                    Color(0xFF191133)  // Deep soft dark indigo
                ),
                center = Offset(px + r * 0.25f, py - r * 0.25f),
                radius = r * 1.4f
            )
        )

        // Soft pastel outline stroke
        drawPath(
            bodyPath,
            color = Color(0xFFB388FF).copy(alpha = 0.65f),
            style = Stroke(width = 2.2f * scaleX)
        )

        // 4. Soft Rounded Head Fluff / Feather Puffs
        val headPuffs = Path().apply {
            moveTo(px - r * 0.05f, py - r * 0.9f)
            quadraticTo(px - r * 0.2f, py - r * 1.22f, px, py - r * 0.98f)
            quadraticTo(px - r * 0.35f, py - r * 1.15f, px - r * 0.2f, py - r * 0.82f)
            close()
        }
        drawPath(
            headPuffs,
            color = Color(0xFF3C2A62)
        )
        drawPath(
            headPuffs,
            color = Color(0xFFB388FF).copy(alpha = 0.7f),
            style = Stroke(width = 1.5f * scaleX)
        )

        // 5. Short, Smiling, Cute Golden Beak
        val isBeakFlap = (playerAngle < -5f) || (gameTicks % 28 < 5)
        val smileCurve = if (isBeakFlap) r * 0.08f else 0f

        val beakPath = Path().apply {
            moveTo(px + r * 0.72f, py - r * 0.22f)
            quadraticTo(
                px + r * 1.15f, py - r * 0.28f,
                px + r * 1.42f, py - r * 0.02f // Rounded tip
            )
            quadraticTo(
                px + r * 1.1f, py + r * 0.18f + smileCurve, // Cute happy smile curve!
                px + r * 0.72f, py + r * 0.15f
            )
            close()
        }

        val beakGradient = Brush.verticalGradient(
            colors = listOf(Color(0xFFFFE082), Color(0xFFFFB300), Color(0xFFFF8F00)),
            startY = py - r * 0.28f,
            endY = py + r * 0.18f
        )
        drawPath(beakPath, brush = beakGradient)

        // Beak smile line accent
        val smileLine = Path().apply {
            moveTo(px + r * 0.72f, py - r * 0.02f)
            quadraticTo(
                px + r * 1.15f, py - r * 0.02f,
                px + r * 1.38f, py - r * 0.02f
            )
        }
        drawPath(smileLine, color = Color(0xFFE65100).copy(alpha = 0.8f), style = Stroke(width = 1.4f * scaleX, cap = StrokeCap.Round))

        // Beak top white sheen highlight
        drawLine(
            color = Color.White.copy(alpha = 0.85f),
            start = Offset(px + r * 0.8f, py - r * 0.2f),
            end = Offset(px + r * 1.25f, py - r * 0.08f),
            strokeWidth = 1.8f * scaleX,
            cap = StrokeCap.Round
        )

        // 6. Cute Rosy Pink Blush Cheek
        val blushX = px + r * 0.38f
        val blushY = py + r * 0.12f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFFF5277).copy(alpha = 0.65f), Color.Transparent),
                center = Offset(blushX, blushY),
                radius = r * 0.28f
            ),
            radius = r * 0.28f,
            center = Offset(blushX, blushY)
        )

        // 7. Giant, Expressive, Adorable Anime Eye
        val eyeX = px + r * 0.38f
        val eyeY = py - r * 0.30f
        val eyeRadius = r * 0.38f

        val isBlinking = (gameTicks % 110 > 103)
        if (isBlinking) {
            val blinkPath = Path().apply {
                moveTo(eyeX - eyeRadius * 0.75f, eyeY)
                quadraticTo(eyeX, eyeY + eyeRadius * 0.45f, eyeX + eyeRadius * 0.75f, eyeY)
            }
            drawPath(blinkPath, color = Color.White, style = Stroke(width = 3.2f * scaleX, cap = StrokeCap.Round))
        } else {
            // White eye background
            drawCircle(color = Color.White, radius = eyeRadius, center = Offset(eyeX, eyeY))

            // Deep sparkling dark purple / indigo iris
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF5E35B1), Color(0xFF311B92), Color(0xFF10002B)),
                    center = Offset(eyeX + eyeRadius * 0.1f, eyeY - eyeRadius * 0.05f),
                    radius = eyeRadius * 0.82f
                ),
                radius = eyeRadius * 0.82f,
                center = Offset(eyeX + eyeRadius * 0.08f, eyeY)
            )

            // Large glossy main sparkle catchlight (top right)
            drawCircle(
                color = Color.White,
                radius = eyeRadius * 0.34f,
                center = Offset(eyeX + eyeRadius * 0.28f, eyeY - eyeRadius * 0.26f)
            )

            // Secondary cute sparkle reflection (bottom left)
            drawCircle(
                color = Color.White.copy(alpha = 0.9f),
                radius = eyeRadius * 0.16f,
                center = Offset(eyeX - eyeRadius * 0.15f, eyeY + eyeRadius * 0.28f)
            )

            // Tiny extra star sparkle dot
            drawCircle(
                color = Color.White.copy(alpha = 0.8f),
                radius = eyeRadius * 0.08f,
                center = Offset(eyeX + eyeRadius * 0.45f, eyeY + eyeRadius * 0.12f)
            )

            // Sweet eyelash arch
            val lashPath = Path().apply {
                moveTo(eyeX - eyeRadius * 0.8f, eyeY - eyeRadius * 0.4f)
                quadraticTo(
                    eyeX, eyeY - eyeRadius * 1.1f,
                    eyeX + eyeRadius * 0.85f, eyeY - eyeRadius * 0.35f
                )
            }
            drawPath(lashPath, color = Color(0xFF1A1133), style = Stroke(width = 2.8f * scaleX, cap = StrokeCap.Round))
        }

        // 8. Chubby Soft Flapping Wing
        val wingPivotX = px - r * 0.08f
        val wingPivotY = py + r * 0.15f
        val wingWidth = r * 1.2f
        val wingHeight = r * 0.75f

        rotate(degrees = wingAngle, pivot = Offset(wingPivotX, wingPivotY)) {
            val wingPath = Path().apply {
                moveTo(wingPivotX, wingPivotY)
                quadraticTo(
                    wingPivotX - wingWidth * 0.8f, wingPivotY - wingHeight * 0.9f,
                    wingPivotX - wingWidth * 1.15f, wingPivotY - wingHeight * 0.3f
                )
                quadraticTo(
                    wingPivotX - wingWidth * 0.7f, wingPivotY + wingHeight * 0.5f,
                    wingPivotX, wingPivotY
                )
                close()
            }
            drawPath(
                wingPath,
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF533F7D), Color(0xFF2D204E)),
                    center = Offset(wingPivotX - wingWidth * 0.5f, wingPivotY),
                    radius = wingWidth
                )
            )
            drawPath(
                wingPath,
                color = Color(0xFFB388FF).copy(alpha = 0.85f),
                style = Stroke(width = 2f * scaleX)
            )

            // Inner wing feather accent
            val innerWing = Path().apply {
                moveTo(wingPivotX, wingPivotY)
                quadraticTo(
                    wingPivotX - wingWidth * 0.6f, wingPivotY - wingHeight * 0.5f,
                    wingPivotX - wingWidth * 0.82f, wingPivotY - wingHeight * 0.1f
                )
            }
            drawPath(
                innerWing,
                color = Color(0xFF80DEEA).copy(alpha = 0.7f),
                style = Stroke(width = 1.6f * scaleX, cap = StrokeCap.Round)
            )
        }

        // 9. Draw Accessories
        drawAccessory(selectedAccessory, px, py, r, scaleX, scaleY, gameTicks)
    }
}

// Draw custom accessory elements mathematically
private fun DrawScope.drawAccessory(
    id: String,
    px: Float,
    py: Float,
    r: Float,
    scaleX: Float,
    scaleY: Float,
    gameTicks: Long
) {
    when (id) {
        "detective_hat" -> {
            // Classic sherlock/detective brown hat on head
            val hatColor = Color(0xFF5D4037)
            val hatTop = py - r * 0.9f
            
            // Brim
            drawRoundRect(
                color = hatColor,
                topLeft = Offset(px - r * 0.7f, hatTop - 3f * scaleY),
                size = Size(r * 1.6f, 5f * scaleY),
                cornerRadius = CornerRadius(2f * scaleX)
            )
            // Hat crown
            val crownPath = Path().apply {
                moveTo(px - r * 0.4f, hatTop - 3f * scaleY)
                quadraticTo(
                    px, hatTop - r * 0.9f,
                    px + r * 0.4f, hatTop - 3f * scaleY
                )
                close()
            }
            drawPath(crownPath, color = hatColor)
            // Black hat ribbon
            drawRect(
                color = Color.Black,
                topLeft = Offset(px - r * 0.4f, hatTop - 4f * scaleY),
                size = Size(r * 0.8f, 2.5f * scaleY)
            )
        }

        "red_scarf" -> {
            // Red scarf around neck (bottom of head/body)
            val scarfColor = Color(0xFFD32F2F)
            val neckY = py + r * 0.7f
            val neckX = px - r * 0.2f

            // Main scarf knot
            drawRoundRect(
                color = scarfColor,
                topLeft = Offset(neckX - 4f * scaleX, neckY - 2f * scaleY),
                size = Size(r * 0.8f, 6f * scaleY),
                cornerRadius = CornerRadius(3f * scaleX)
            )

            // Tail waving in wind
            val wavePhase = sin(gameTicks * 0.25f) * 4f * scaleY
            val tailPath = Path().apply {
                moveTo(neckX - 2f * scaleX, neckY + 2f * scaleY)
                quadraticTo(
                    neckX - r * 0.7f, neckY + 4f * scaleY + wavePhase,
                    neckX - r * 1.3f, neckY + wavePhase
                )
                lineTo(neckX - r * 1.2f, neckY + wavePhase + 6f * scaleY)
                quadraticTo(
                    neckX - r * 0.6f, neckY + 8f * scaleY,
                    neckX + 4f * scaleX, neckY + 4f * scaleY
                )
                close()
            }
            drawPath(tailPath, color = scarfColor)
        }

        "wizard_hat" -> {
            // Blue pointed wizard hat with tiny gold stars
            val wizardBlue = Color(0xFF1E3A8A)
            val hatTop = py - r * 0.9f
            
            // Brim
            drawRoundRect(
                color = wizardBlue,
                topLeft = Offset(px - r * 0.8f, hatTop - 3f * scaleY),
                size = Size(r * 1.7f, 5f * scaleY),
                cornerRadius = CornerRadius(2f * scaleX)
            )

            // Tall cone pointing back
            val conePath = Path().apply {
                moveTo(px - r * 0.4f, hatTop - 3f * scaleY)
                lineTo(px + r * 0.4f, hatTop - 3f * scaleY)
                lineTo(px - r * 0.6f, hatTop - r * 1.5f) // Leaning back
                close()
            }
            drawPath(conePath, color = wizardBlue)

            // Sparkle on tip
            drawCircle(
                color = Color.Yellow,
                radius = 3f * scaleX,
                center = Offset(px - r * 0.6f, hatTop - r * 1.5f)
            )
        }

        "golden_crown" -> {
            // Shiny royal gold crown with rubies
            val crownGold = NightBeakGold
            val hatTop = py - r * 0.9f

            val crownPath = Path().apply {
                moveTo(px - r * 0.6f, hatTop)
                lineTo(px - r * 0.6f, hatTop - r * 0.6f)
                lineTo(px - r * 0.3f, hatTop - r * 0.3f)
                lineTo(px, hatTop - r * 0.8f) // Middle spike tall
                lineTo(px + r * 0.3f, hatTop - r * 0.3f)
                lineTo(px + r * 0.6f, hatTop - r * 0.6f)
                lineTo(px + r * 0.6f, hatTop)
                close()
            }
            drawPath(crownPath, color = crownGold)

            // Ruby red dots on spikes
            drawCircle(color = Color.Red, radius = 2f * scaleX, center = Offset(px - r * 0.6f, hatTop - r * 0.6f))
            drawCircle(color = Color.Red, radius = 2.5f * scaleX, center = Offset(px, hatTop - r * 0.8f))
            drawCircle(color = Color.Red, radius = 2f * scaleX, center = Offset(px + r * 0.6f, hatTop - r * 0.6f))
        }

        "pilot_goggles" -> {
            // Brown pilot goggles over the eyes
            val goggleBrown = Color(0xFF4E342E)
            val eyeX = px + r * 0.4f
            val eyeY = py - r * 0.3f
            val eyeRadius = r * 0.38f

            // Draw brown strap around head
            drawLine(
                color = goggleBrown,
                start = Offset(px - r * 0.9f, eyeY),
                end = Offset(eyeX, eyeY),
                strokeWidth = 4f * scaleY
            )

            // Rim frame
            drawCircle(
                color = goggleBrown,
                radius = eyeRadius,
                center = Offset(eyeX, eyeY),
                style = Stroke(width = 3f * scaleX)
            )
            // Lens glow reflection
            drawCircle(
                color = NightSecondary.copy(alpha = 0.5f),
                radius = eyeRadius * 0.7f,
                center = Offset(eyeX, eyeY)
            )
        }
    }
}

// Draw animated background bat silhouettes
private fun DrawScope.drawBackgroundBat(bat: BackgroundBat, scaleX: Float, scaleY: Float, gameTicks: Long) {
    val bx = bat.x * scaleX
    val by = bat.y * scaleY
    val bs = 16f * scaleX * bat.scale
    
    // Wings flapping phase
    val wingFlap = sin(gameTicks * 0.2f + bat.wingPhase) * bs * 0.7f
    
    val batPath = Path().apply {
        moveTo(bx, by)
        // Left Wing
        lineTo(bx - bs, by - wingFlap)
        lineTo(bx - bs * 0.4f, by + bs * 0.2f)
        // Right Wing
        moveTo(bx, by)
        lineTo(bx + bs, by - wingFlap)
        lineTo(bx + bs * 0.4f, by + bs * 0.2f)
        // Tail/body
        lineTo(bx, by + bs * 0.4f)
        close()
    }
    
    drawPath(
        path = batPath,
        color = Color(0xFF0D061F),
        style = Stroke(width = 1.5f * scaleX)
    )
    drawPath(
        path = batPath,
        color = Color(0xFF190F2C).copy(alpha = 0.7f)
    )
}
