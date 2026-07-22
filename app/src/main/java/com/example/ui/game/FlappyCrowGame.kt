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
            RetroAudioEngine.playFlap(isSoundEnabled)

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
                    shakeDuration = 15
                    triggerVibration(120)
                    RetroAudioEngine.playHit(isSoundEnabled)
                    delay(300)
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
                        RetroAudioEngine.playCoin(isSoundEnabled)
                        
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
                        RetroAudioEngine.playCoin(isSoundEnabled)

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
                    shakeDuration = 18
                    triggerVibration(200)
                    RetroAudioEngine.playHit(isSoundEnabled)
                    delay(350)
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
            val shakeX = if (shakeDuration > 0) Random.nextInt(-6, 7).toFloat() * scaleX else 0f
            val shakeY = if (shakeDuration > 0) Random.nextInt(-6, 7).toFloat() * scaleY else 0f

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
                if (collisionFlash && shakeDuration > 0) {
                    drawRect(
                        color = Color.Red.copy(alpha = 0.25f),
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
                    onClick = { isPaused = !isPaused },
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Navy2Color.copy(alpha = 0.8f))
                        .border(BorderStroke(1.dp, AmethystColor.copy(alpha = 0.4f)), CircleShape)
                        .size(42.dp)
                        .testTag("pause_button")
                ) {
                    Icon(
                        imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                        contentDescription = "Pause Game",
                        tint = CyanColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Pause Overlay
        if (isPaused) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.65f))
                    .clickable { /* swallow clicks */ },
                contentAlignment = Alignment.Center
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Navy2Color),
                    modifier = Modifier.width(280.dp),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.5.dp, PurpleColor.copy(alpha = 0.6f))
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Text(
                            text = "GAME PAUSED",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Black,
                                color = MoonColor,
                                letterSpacing = 1.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Score so far: $score",
                            style = MaterialTheme.typography.bodyMedium.copy(color = DimColor)
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = { restartGame() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Gray.copy(alpha = 0.3f)),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("RESTART", style = MaterialTheme.typography.bodyMedium.copy(color = MoonColor, fontWeight = FontWeight.Bold))
                            }
                            Button(
                                onClick = { isPaused = false },
                                colors = ButtonDefaults.buttonColors(containerColor = MagentaColor),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("RESUME", style = MaterialTheme.typography.bodyMedium.copy(color = MoonColor, fontWeight = FontWeight.Bold))
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
    // 1. Draw radial glowing gradient for deep cosmic atmosphere
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFF1B0B3C), Color(0xFF040209)),
            center = Offset(size.width * 0.5f, size.height * 0.3f),
            radius = size.height * 0.8f
        ),
        topLeft = Offset.Zero,
        size = size
    )

    // 2. Draw Twinkling Stars
    stars.toList().forEach { star ->
        drawCircle(
            color = SparkleWhite.copy(alpha = star.brightness),
            radius = star.size * scaleX,
            center = Offset(star.x * scaleX, star.y * scaleY)
        )
    }

    // 3. Draw Huge Glowing Moon
    val moonCenterX = 280f * scaleX
    val moonCenterY = 120f * scaleY
    val moonRadius = 45f * scaleX

    // Outer moon glow
    drawCircle(
        color = NightTertiary.copy(alpha = 0.15f),
        radius = moonRadius + 15f * scaleX,
        center = Offset(moonCenterX, moonCenterY)
    )
    // Core Moon
    drawCircle(
        color = Color(0xFFFFF9D8),
        radius = moonRadius,
        center = Offset(moonCenterX, moonCenterY)
    )
    // Subtle crater/shadow detail inside the moon
    drawCircle(
        color = Color(0xFFEADB9E).copy(alpha = 0.6f),
        radius = moonRadius * 0.25f,
        center = Offset(moonCenterX - moonRadius * 0.3f, moonCenterY - moonRadius * 0.2f)
    )
    drawCircle(
        color = Color(0xFFEADB9E).copy(alpha = 0.6f),
        radius = moonRadius * 0.15f,
        center = Offset(moonCenterX + moonRadius * 0.2f, moonCenterY + moonRadius * 0.3f)
    )

    // 4. Draw Parallax Background Silhouette of Magical City
    val buildingWidth = size.width / 10f
    for (i in 0..10) {
        val buildingHeight = (buildings.getOrNull(i) ?: 100f) * scaleY
        val left = i * buildingWidth
        val top = size.height - buildingHeight
        
        drawRect(
            color = Color(0xFF140D2C).copy(alpha = 0.65f),
            topLeft = Offset(left, top),
            size = Size(buildingWidth + 2f, buildingHeight)
        )

        // Draw small warm yellow windows on some background buildings
        if (i % 2 == 0) {
            val winSize = 4f * scaleX
            val winX = left + buildingWidth * 0.4f
            val winY = top + buildingHeight * 0.3f
            drawRect(
                color = NightAccentOrange.copy(alpha = 0.7f),
                topLeft = Offset(winX, winY),
                size = Size(winSize, winSize * 1.5f)
            )
            drawRect(
                color = NightAccentOrange.copy(alpha = 0.7f),
                topLeft = Offset(winX + 12f * scaleX, winY + 20f * scaleY),
                size = Size(winSize, winSize * 1.5f)
            )
        }
    }

    // 5. Draw drifting puffy clouds
    clouds.toList().forEach { cloud ->
        val cloudX = cloud.x * scaleX
        val cloudY = cloud.y * scaleY
        val cloudR = 25f * scaleX * cloud.scale

        drawCircle(
            color = Color(0xFF4C3B78).copy(alpha = 0.3f),
            radius = cloudR,
            center = Offset(cloudX, cloudY)
        )
        drawCircle(
            color = Color(0xFF4C3B78).copy(alpha = 0.3f),
            radius = cloudR * 0.8f,
            center = Offset(cloudX - cloudR * 0.6f, cloudY + cloudR * 0.1f)
        )
        drawCircle(
            color = Color(0xFF4C3B78).copy(alpha = 0.3f),
            radius = cloudR * 0.8f,
            center = Offset(cloudX + cloudR * 0.6f, cloudY + cloudR * 0.1f)
        )
    }
}

// Draw the customized obstacles (Chimney, PowerLine, CastleTower, TreeBranch, FlyingObstacle)
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
            // --- TOP CHIMNEY ---
            // Draw Main Brick Body
            drawRect(
                color = Color(0xFF8B2500), // Brick Red
                topLeft = Offset(left, 0f),
                size = Size(width, gapTop)
            )
            // Ledge collar at bottom
            val ledgeHeight = 16f * scaleY
            val ledgeWidthExtra = 6f * scaleX
            drawRoundRect(
                color = Color(0xFFA0360F),
                topLeft = Offset(left - ledgeWidthExtra, gapTop - ledgeHeight),
                size = Size(width + ledgeWidthExtra * 2f, ledgeHeight),
                cornerRadius = CornerRadius(4f * scaleX)
            )
            // Smoke soot border on bottom
            drawRect(
                color = Color(0xFF2B211E),
                topLeft = Offset(left, gapTop - 3f * scaleY),
                size = Size(width, 3f * scaleY)
            )

            // --- BOTTOM CHIMNEY ---
            // Draw Main Brick Body
            val bottomChimneyHeight = size.height - gapBottom
            drawRect(
                color = Color(0xFF8B2500),
                topLeft = Offset(left, gapBottom),
                size = Size(width, bottomChimneyHeight)
            )
            // Ledge collar at top
            drawRoundRect(
                color = Color(0xFFA0360F),
                topLeft = Offset(left - ledgeWidthExtra, gapBottom),
                size = Size(width + ledgeWidthExtra * 2f, ledgeHeight),
                cornerRadius = CornerRadius(4f * scaleX)
            )
            // Smoke soot border on top
            drawRect(
                color = Color(0xFF2B211E),
                topLeft = Offset(left, gapBottom),
                size = Size(width, 3f * scaleY)
            )
        }

        ObstacleType.PowerLine -> {
            // Draw wooden utility poles on upper/lower sides
            val poleWidth = 14f * scaleX
            drawRect(
                color = Color(0xFF3E2723), // Dark wood brown
                topLeft = Offset((obs.x - 7f) * scaleX, 0f),
                size = Size(poleWidth, gapTop)
            )
            drawRect(
                color = Color(0xFF3E2723),
                topLeft = Offset((obs.x - 7f) * scaleX, gapBottom),
                size = Size(poleWidth, size.height - gapBottom)
            )

            // Draw crossbeams
            val beamHeight = 10f * scaleY
            val beamWidth = 44f * scaleX
            drawRect(
                color = Color(0xFF5D4037),
                topLeft = Offset((obs.x - 22f) * scaleX, gapTop - beamHeight - 10f * scaleY),
                size = Size(beamWidth, beamHeight)
            )
            drawRect(
                color = Color(0xFF5D4037),
                topLeft = Offset((obs.x - 22f) * scaleX, gapBottom + 10f * scaleY),
                size = Size(beamWidth, beamHeight)
            )

            // Draw spark particles at the electrical wire endpoints
            val showSpark = (gameTicks % 15 < 6)
            if (showSpark) {
                drawCircle(
                    color = NightSecondary,
                    radius = 6f * scaleX,
                    center = Offset(obs.x * scaleX, gapTop - 5f * scaleY)
                )
                drawCircle(
                    color = NightSecondary,
                    radius = 6f * scaleX,
                    center = Offset(obs.x * scaleX, gapBottom + 5f * scaleY)
                )
            }
        }

        ObstacleType.CastleTower -> {
            // Stone towers
            // TOP TOWER
            drawRect(
                color = Color(0xFF5A5C66), // Medium grey stone
                topLeft = Offset(left, 0f),
                size = Size(width, gapTop)
            )
            // Battlement crown on bottom
            val battleHeight = 18f * scaleY
            val castleExtra = 4f * scaleX
            drawRect(
                color = Color(0xFF45464F), // Lighter stone accent
                topLeft = Offset(left - castleExtra, gapTop - battleHeight),
                size = Size(width + castleExtra * 2f, battleHeight)
            )
            // Cutouts for battlements
            val cutoutW = (width + castleExtra * 2f) / 3f
            drawRect(
                color = NightBackground,
                topLeft = Offset(left - castleExtra + cutoutW * 0.7f, gapTop - battleHeight * 0.4f),
                size = Size(cutoutW * 0.6f, battleHeight * 0.42f)
            )

            // BOTTOM TOWER
            drawRect(
                color = Color(0xFF5A5C66),
                topLeft = Offset(left, gapBottom),
                size = Size(width, size.height - gapBottom)
            )
            // Battlement crown on top
            drawRect(
                color = Color(0xFF45464F),
                topLeft = Offset(left - castleExtra, gapBottom),
                size = Size(width + castleExtra * 2f, battleHeight)
            )
            drawRect(
                color = NightBackground,
                topLeft = Offset(left - castleExtra + cutoutW * 0.7f, gapBottom),
                size = Size(cutoutW * 0.6f, battleHeight * 0.4f)
            )

            // Draw a tiny cute waving flag on top tower
            val flagPhase = sin(gameTicks * 0.2f) * 6f
            val poleX = obs.x * scaleX
            val poleY = gapBottom + 35f * scaleY
            
            // Flag pole
            drawLine(
                color = Color.LightGray,
                start = Offset(poleX, poleY),
                end = Offset(poleX + 12f * scaleX, poleY),
                strokeWidth = 2f * scaleX
            )
            // Flag banner
            val flagPath = Path().apply {
                moveTo(poleX + 12f * scaleX, poleY - 6f * scaleY)
                lineTo(poleX + 30f * scaleX + flagPhase * scaleX, poleY)
                lineTo(poleX + 12f * scaleX, poleY + 6f * scaleY)
                close()
            }
            drawPath(flagPath, color = Color.Red)
        }

        ObstacleType.TreeBranch -> {
            // Gnarled, crooked branches with bark texture
            // TOP BRANCH
            val topPath = Path().apply {
                moveTo(left + width * 0.4f, 0f)
                quadraticTo(
                    obs.x * scaleX, gapTop * 0.5f,
                    obs.x * scaleX, gapTop
                )
                lineTo((obs.x - 10f) * scaleX, gapTop)
                quadraticTo(
                    (obs.x - 20f) * scaleX, gapTop * 0.5f,
                    left, 0f
                )
                close()
            }
            drawPath(topPath, color = Color(0xFF381C00))

            // BOTTOM BRANCH
            val bottomPath = Path().apply {
                moveTo(obs.x * scaleX, gapBottom)
                quadraticTo(
                    (obs.x + 10f) * scaleX, gapBottom + (size.height - gapBottom) * 0.5f,
                    right, size.height
                )
                lineTo(left, size.height)
                quadraticTo(
                    (obs.x - 10f) * scaleX, gapBottom + (size.height - gapBottom) * 0.5f,
                    (obs.x - 15f) * scaleX, gapBottom
                )
                close()
            }
            drawPath(bottomPath, color = Color(0xFF381C00))

            // Add cute tiny glowing eyes inside a tree notch!
            val eyesFlash = (gameTicks % 40 < 32)
            if (eyesFlash) {
                val eyeX = (obs.x - 5f) * scaleX
                val eyeY = gapBottom + 45f * scaleY
                if (eyeY < size.height - 20f) {
                    drawCircle(color = NightSecondary, radius = 2.5f * scaleX, center = Offset(eyeX, eyeY))
                    drawCircle(color = NightSecondary, radius = 2.5f * scaleX, center = Offset(eyeX + 6f * scaleX, eyeY))
                }
            }
        }

        ObstacleType.FlyingObstacle -> {
            // A hybrid brick wall obstacle carrying animated floating items
            drawRect(
                color = Color(0xFF1B0024),
                topLeft = Offset(left, 0f),
                size = Size(width, gapTop)
            )
            drawRect(
                color = Color(0xFF1B0024),
                topLeft = Offset(left, gapBottom),
                size = Size(width, size.height - gapBottom)
            )

            // Animated bat/drone warning light
            val lightColor = if (gameTicks % 20 < 10) Color.Red else Color.Transparent
            drawCircle(
                color = lightColor,
                radius = 5f * scaleX,
                center = Offset(obs.x * scaleX, gapTop - 8f * scaleY)
            )
            drawCircle(
                color = lightColor,
                radius = 5f * scaleX,
                center = Offset(obs.x * scaleX, gapBottom + 8f * scaleY)
            )
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

        // 0. Outer Magical Aura Halo
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    CyanColor.copy(alpha = 0.25f),
                    AmethystColor.copy(alpha = 0.15f),
                    Color.Transparent
                ),
                center = Offset(px, py),
                radius = r * 1.8f
            ),
            radius = r * 1.8f,
            center = Offset(px, py)
        )

        // 1. Multi-Layered Tail Feathers Fan (Spread raven tail feathers with gradient & highlights)
        val tailSway = sin(gameTicks * 0.12f) * 4f
        for (i in -1..1) {
            val spreadAngle = i * 16f + tailSway
            rotate(degrees = spreadAngle, pivot = Offset(px - r * 0.7f, py)) {
                val tailPath = Path().apply {
                    moveTo(px - r * 0.7f, py)
                    lineTo(px - r * 1.65f, py - r * 0.22f)
                    quadraticTo(
                        px - r * 1.85f, py,
                        px - r * 1.65f, py + r * 0.22f
                    )
                    close()
                }
                drawPath(
                    tailPath,
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFF141121), Color(0xFF261D42), Color(0xFF382963)),
                        start = Offset(px - r * 0.7f, py),
                        end = Offset(px - r * 1.8f, py)
                    )
                )
                drawPath(
                    tailPath,
                    color = CyanColor.copy(alpha = 0.4f),
                    style = Stroke(width = 1.5f * scaleX)
                )
            }
        }

        // 2. Cute Avian Feet Claws (Tucked under body)
        val feetColor = Color(0xFFFF9800)
        val leftFootPath = Path().apply {
            moveTo(px - r * 0.2f, py + r * 0.85f)
            lineTo(px - r * 0.35f, py + r * 1.15f)
            lineTo(px - r * 0.15f, py + r * 1.12f)
            lineTo(px - r * 0.05f, py + r * 1.15f)
            close()
        }
        val rightFootPath = Path().apply {
            moveTo(px + r * 0.15f, py + r * 0.85f)
            lineTo(px + r * 0.05f, py + r * 1.15f)
            lineTo(px + r * 0.2f, py + r * 1.12f)
            lineTo(px + r * 0.3f, py + r * 1.15f)
            close()
        }
        drawPath(leftFootPath, color = feetColor)
        drawPath(rightFootPath, color = feetColor)

        // 3. Crow Main Body (Organic teardrop shape + Radial Obsidian/Amethyst Gradient)
        val bodyPath = Path().apply {
            moveTo(px + r * 0.85f, py - r * 0.1f) // Head top
            quadraticTo(
                px + r * 1.05f, py + r * 0.4f,
                px + r * 0.4f, py + r * 0.95f // Chest/belly curve
            )
            quadraticTo(
                px - r * 0.6f, py + r * 1.05f,
                px - r * 0.95f, py + r * 0.2f // Back lower curve
            )
            quadraticTo(
                px - r * 0.85f, py - r * 0.85f,
                px + r * 0.1f, py - r * 0.95f // Back upper curve
            )
            close()
        }

        // Draw deep body gradient fill
        drawPath(
            bodyPath,
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF3B2A63),
                    Color(0xFF1F1836),
                    Color(0xFF0F0B1A)
                ),
                center = Offset(px + r * 0.2f, py - r * 0.2f),
                radius = r * 1.35f
            )
        )

        // Scruffy Feather Crest Tufts on top/back of head (Signifying her playful, chaotic character)
        val headTufts = Path().apply {
            // Top crest tuft 1
            moveTo(px - r * 0.1f, py - r * 0.92f)
            quadraticTo(px - r * 0.25f, py - r * 1.35f, px - r * 0.02f, py - r * 1.05f)
            // Top crest tuft 2
            quadraticTo(px - r * 0.45f, py - r * 1.25f, px - r * 0.25f, py - r * 0.85f)
            // Back scruffy tuft 3
            quadraticTo(px - r * 0.75f, py - r * 1.05f, px - r * 0.55f, py - r * 0.65f)
            close()
        }
        drawPath(
            headTufts,
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF4A3875), Color(0xFF1A132C)),
                startY = py - r * 1.35f,
                endY = py - r * 0.65f
            )
        )
        drawPath(headTufts, color = CyanColor.copy(alpha = 0.6f), style = Stroke(width = 1.6f * scaleX))

        // Cute Scruffy Cheek Feather Fluff
        val cheekFluff = Path().apply {
            moveTo(px - r * 0.15f, py + r * 0.15f)
            quadraticTo(px - r * 0.55f, py + r * 0.25f, px - r * 0.25f, py + r * 0.45f)
            quadraticTo(px - r * 0.6f, py + r * 0.55f, px - r * 0.15f, py + r * 0.65f)
            close()
        }
        drawPath(cheekFluff, color = Color(0xFF231A3D))
        drawPath(cheekFluff, color = AmethystColor.copy(alpha = 0.5f), style = Stroke(width = 1.4f * scaleX))

        // Glowing outer body stroke rim (Moonlit Silver/Cyan sheen)
        drawPath(
            bodyPath,
            brush = Brush.sweepGradient(
                colors = listOf(
                    CyanColor.copy(alpha = 0.85f),
                    Color(0xFFE2D6FF).copy(alpha = 0.7f), // Moonlit silver
                    AmethystColor.copy(alpha = 0.65f),
                    CyanColor.copy(alpha = 0.85f)
                ),
                center = Offset(px, py)
            ),
            style = Stroke(width = 2.5f * scaleX)
        )

        // Cute chest feather fluff ripples
        val chestFluff = Path().apply {
            moveTo(px + r * 0.45f, py + r * 0.25f)
            quadraticTo(px + r * 0.25f, py + r * 0.45f, px + r * 0.5f, py + r * 0.6f)
            quadraticTo(px + r * 0.25f, py + r * 0.75f, px + r * 0.35f, py + r * 0.85f)
        }
        drawPath(chestFluff, color = AmethystColor.copy(alpha = 0.5f), style = Stroke(width = 2f * scaleX))

        // 4. 3D Sculpted Golden Beak with Dynamic Flap/Upward Open Reaction
        val isBeakOpen = (playerAngle < -5f) || (gameTicks % 30 < 6)
        val beakOpenGap = if (isBeakOpen) r * 0.12f else 0f

        val upperBeak = Path().apply {
            moveTo(px + r * 0.75f, py - r * 0.25f)
            quadraticTo(
                px + r * 1.35f, py - r * 0.28f,
                px + r * 1.85f, py - beakOpenGap * 0.5f // Curved upper beak tip
            )
            lineTo(px + r * 0.78f, py - beakOpenGap * 0.2f)
            close()
        }
        val lowerBeak = Path().apply {
            moveTo(px + r * 0.78f, py + r * 0.05f + beakOpenGap * 0.3f)
            lineTo(px + r * 1.85f, py + beakOpenGap * 0.5f)
            quadraticTo(
                px + r * 1.25f, py + r * 0.38f + beakOpenGap,
                px + r * 0.75f, py + r * 0.28f + beakOpenGap
            )
            close()
        }

        // Fill beak with 3D gold gradient
        val beakGradient = Brush.verticalGradient(
            colors = listOf(Color(0xFFFFF59D), Color(0xFFFFB300), Color(0xFFE65100)),
            startY = py - r * 0.3f,
            endY = py + r * 0.3f
        )
        drawPath(upperBeak, brush = beakGradient)
        drawPath(lowerBeak, brush = beakGradient)

        // Dark interior mouth gap when beak is open
        if (isBeakOpen) {
            val mouthGap = Path().apply {
                moveTo(px + r * 0.8f, py - beakOpenGap * 0.1f)
                lineTo(px + r * 1.7f, py)
                lineTo(px + r * 0.8f, py + beakOpenGap * 0.4f)
                close()
            }
            drawPath(mouthGap, color = Color(0xFF3E1F00))
        }

        // Beak ridge white highlight
        drawLine(
            color = Color.White.copy(alpha = 0.9f),
            start = Offset(px + r * 0.85f, py - r * 0.22f),
            end = Offset(px + r * 1.6f, py - beakOpenGap * 0.4f),
            strokeWidth = 1.8f * scaleX,
            cap = StrokeCap.Round
        )

        // Tiny cute nostril slot
        drawCircle(
            color = Color(0xFF422100),
            radius = r * 0.05f,
            center = Offset(px + r * 0.95f, py - r * 0.16f)
        )

        // 5. Expressive High-Quality Anime Eyes (Glowing Amber Gold Iris)
        val eyeX = px + r * 0.42f
        val eyeY = py - r * 0.32f
        val eyeRadius = r * 0.38f

        val isBlinking = (gameTicks % 120 > 112)
        if (isBlinking) {
            // Cute fluttering closed eye arch
            val blinkPath = Path().apply {
                moveTo(eyeX - eyeRadius * 0.8f, eyeY)
                quadraticTo(eyeX, eyeY + eyeRadius * 0.4f, eyeX + eyeRadius * 0.8f, eyeY)
            }
            drawPath(blinkPath, color = NightOnBackground, style = Stroke(width = 3f * scaleX, cap = StrokeCap.Round))
        } else {
            // White eyeball background
            drawCircle(
                color = Color.White,
                radius = eyeRadius,
                center = Offset(eyeX, eyeY)
            )

            // Luminous Golden-Amber Iris (Warmth, high contrast & expressive charm)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFFFF176), Color(0xFFFFB300), Color(0xFFE65100), Color(0xFF4E1D00)),
                    center = Offset(eyeX + eyeRadius * 0.1f, eyeY - eyeRadius * 0.05f),
                    radius = eyeRadius * 0.72f
                ),
                radius = eyeRadius * 0.72f,
                center = Offset(eyeX + eyeRadius * 0.15f, eyeY)
            )

            // Black pupil
            drawCircle(
                color = Color(0xFF100A03),
                radius = eyeRadius * 0.42f,
                center = Offset(eyeX + eyeRadius * 0.18f, eyeY)
            )

            // Primary Sparkle Catchlight (Large top-right reflection)
            drawCircle(
                color = Color.White,
                radius = eyeRadius * 0.26f,
                center = Offset(eyeX + eyeRadius * 0.32f, eyeY - eyeRadius * 0.25f)
            )

            // Secondary Catchlight (Small bottom-left sparkle reflection)
            drawCircle(
                color = Color.White.copy(alpha = 0.85f),
                radius = eyeRadius * 0.13f,
                center = Offset(eyeX - eyeRadius * 0.08f, eyeY + eyeRadius * 0.25f)
            )
        }

        // Cheeky Eyebrow / Brow Feather Ridge (Gives Coco her iconic mischievous, clever personality)
        val browRidge = Path().apply {
            moveTo(eyeX - eyeRadius * 0.9f, eyeY - eyeRadius * 0.85f)
            quadraticTo(
                eyeX, eyeY - eyeRadius * 1.25f,
                eyeX + eyeRadius * 1.05f, eyeY - eyeRadius * 0.6f
            )
        }
        drawPath(
            browRidge,
            color = Color(0xFF160F2B),
            style = Stroke(width = 3.5f * scaleX, cap = StrokeCap.Round)
        )
        drawPath(
            browRidge,
            color = CyanColor.copy(alpha = 0.75f),
            style = Stroke(width = 1.5f * scaleX, cap = StrokeCap.Round)
        )

        // Cute Rosy Pink Blush Cheek Mark
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFFF5252).copy(alpha = 0.55f), Color.Transparent),
                center = Offset(eyeX + eyeRadius * 0.1f, eyeY + eyeRadius * 1.15f),
                radius = r * 0.28f
            ),
            radius = r * 0.28f,
            center = Offset(eyeX + eyeRadius * 0.1f, eyeY + eyeRadius * 1.15f)
        )

        // 6. Multi-Layered Feather Wings with Gradient Fills
        val wingWidth = r * 1.35f
        val wingHeight = r * 0.8f
        val wingPivotX = px - r * 0.1f
        val wingPivotY = py + r * 0.12f

        rotate(degrees = wingAngle, pivot = Offset(wingPivotX, wingPivotY)) {
            val spread = ((wingAngle + 45f) / 85f).coerceIn(0f, 1f)

            // Primary Flight Feather 1
            val feather1 = Path().apply {
                moveTo(wingPivotX, wingPivotY)
                lineTo(wingPivotX - wingWidth * 1.15f, wingPivotY - wingHeight * (0.88f + 0.25f * spread))
                quadraticTo(
                    wingPivotX - wingWidth * 0.55f, wingPivotY + wingHeight * 0.45f,
                    wingPivotX, wingPivotY
                )
                close()
            }
            drawPath(
                feather1,
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFF120E21), Color(0xFF2E2154), Color(0xFF4A3482)),
                    start = Offset(wingPivotX, wingPivotY),
                    end = Offset(wingPivotX - wingWidth, wingPivotY - wingHeight)
                )
            )
            drawPath(feather1, color = CyanColor.copy(alpha = 0.85f), style = Stroke(width = 2f * scaleX))

            // Secondary Covert Feather 2
            val feather2 = Path().apply {
                moveTo(wingPivotX, wingPivotY)
                lineTo(wingPivotX - wingWidth * 0.92f, wingPivotY - wingHeight * (0.58f - 0.2f * spread))
                quadraticTo(
                    wingPivotX - wingWidth * 0.45f, wingPivotY + wingHeight * 0.48f,
                    wingPivotX, wingPivotY
                )
                close()
            }
            drawPath(
                feather2,
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFF1C1733), Color(0xFF382766)),
                    start = Offset(wingPivotX, wingPivotY),
                    end = Offset(wingPivotX - wingWidth * 0.8f, wingPivotY - wingHeight * 0.5f)
                )
            )
            drawPath(feather2, color = AmethystColor.copy(alpha = 0.8f), style = Stroke(width = 1.6f * scaleX))

            // Inner Feather 3
            val feather3 = Path().apply {
                moveTo(wingPivotX, wingPivotY)
                lineTo(wingPivotX - wingWidth * 0.68f, wingPivotY - wingHeight * 0.28f)
                quadraticTo(
                    wingPivotX - wingWidth * 0.32f, wingPivotY + wingHeight * 0.38f,
                    wingPivotX, wingPivotY
                )
                close()
            }
            drawPath(feather3, color = Color(0xFF151026))
            drawPath(feather3, color = CyanColor.copy(alpha = 0.6f), style = Stroke(width = 1.3f * scaleX))
        }

        // 7. Draw SELECTED ACCESSORY (Detective Hat, Red Scarf, Wizard Hat, Golden Crown, Pilot Goggles)
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
