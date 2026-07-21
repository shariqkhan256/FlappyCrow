package com.example.ui.game

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.UnlockedAccessory
import com.example.data.database.UnlockedAchievement
import com.example.data.database.UserStats
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.sin

// --- SCREEN 1: MAIN MENU ---
@Composable
fun MainMenuScreen(
    stats: UserStats,
    onNavigate: (GameScreen) -> Unit,
    modifier: Modifier = Modifier
) {
    // Beautiful pulsing/bobbing animation for Coco on the menu
    val infiniteTransition = rememberInfiniteTransition(label = "menu_bobbing")
    val bobbingOffset by infiniteTransition.animateFloat(
        initialValue = -10f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bobbing"
    )

    val wingAngle by infiniteTransition.animateFloat(
        initialValue = -20f,
        targetValue = 20f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wing_flap"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(NightBackground, Color(0xFF160D34))
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        // Decorative background stars
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Draw a huge soft glowing yellow moon in top-left
            drawCircle(
                color = NightTertiary.copy(alpha = 0.08f),
                radius = 120f * density,
                center = Offset(80f * density, 100f * density)
            )
            drawCircle(
                color = Color(0xFFFFFAD6).copy(alpha = 0.4f),
                radius = 50f * density,
                center = Offset(80f * density, 100f * density)
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            // High score banner
            Surface(
                color = NightSurface.copy(alpha = 0.8f),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, NightSecondary.copy(alpha = 0.5f)),
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = "High Score",
                        tint = NightTertiary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "BEST SCORE: ",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = NightOnSurface.copy(alpha = 0.8f)
                        )
                    )
                    Text(
                        text = "${stats.highScore}",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            color = NightSecondary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Game Title with Neon text effect
            Text(
                text = "FLAPPYCROW",
                style = MaterialTheme.typography.displayMedium.copy(
                    fontWeight = FontWeight.Black,
                    color = NightPrimary,
                    letterSpacing = 6.sp,
                    textAlign = TextAlign.Center
                ),
                modifier = Modifier.shadow(8.dp, clip = false)
            )

            Text(
                text = "Coco's Magical Flight",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = NightSecondary,
                    letterSpacing = 2.sp
                ),
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.weight(0.4f))

            // Animated Bobbing Crow Avatar sitting on a Chimney
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .offset(y = bobbingOffset.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val cx = size.width / 2f
                    val cy = size.height * 0.45f
                    val r = 28f * density

                    // 1. Draw Chimney base
                    drawRect(
                        color = Color(0xFF8B2500),
                        topLeft = Offset(cx - 25f * density, cy + r * 0.8f),
                        size = Size(50f * density, 60f * density)
                    )
                    drawRect(
                        color = Color(0xFF2B211E),
                        topLeft = Offset(cx - 25f * density, cy + r * 0.8f),
                        size = Size(50f * density, 4f * density)
                    )

                    // 2. Draw Crow Body
                    drawCircle(color = FeatherBlack, radius = r, center = Offset(cx, cy))
                    drawCircle(color = FeatherHighlight, radius = r * 0.85f, center = Offset(cx, cy), style = Stroke(2f * density))

                    // Beak
                    val beak = Path().apply {
                        moveTo(cx + r * 0.8f, cy - r * 0.2f)
                        lineTo(cx + r * 1.5f, cy)
                        lineTo(cx + r * 0.8f, cy + r * 0.2f)
                        close()
                    }
                    drawPath(beak, color = NightBeakGold)

                    // Eye
                    drawCircle(color = Color.White, radius = r * 0.3f, center = Offset(cx + r * 0.3f, cy - r * 0.3f))
                    drawCircle(color = Color.Black, radius = r * 0.15f, center = Offset(cx + r * 0.35f, cy - r * 0.3f))

                    // Flapping Wing
                    val wingW = r * 1.1f
                    val wingH = r * 0.6f
                    val wPivotX = cx - r * 0.1f
                    val wPivotY = cy + r * 0.1f

                    drawCircle(color = FeatherBlack, radius = r * 0.6f, center = Offset(wPivotX, wPivotY))
                }
            }

            Spacer(modifier = Modifier.weight(0.6f))

            // Menu Buttons: Standard Rounded M3 styled Game Actions
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxWidth(0.85f)
            ) {
                Button(
                    onClick = { onNavigate(GameScreen.Game) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("play_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = NightPrimary),
                    shape = RoundedCornerShape(27.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = NightBackground)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "START FLAPPING",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = NightBackground
                            )
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Customize button
                    Button(
                        onClick = { onNavigate(GameScreen.Customize) },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("customize_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = NightSurface),
                        shape = RoundedCornerShape(25.dp),
                        border = BorderStroke(1.dp, NightSecondary.copy(alpha = 0.4f))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.Checkroom, contentDescription = null, tint = NightSecondary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "WARDROBE",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = NightSecondary
                                )
                            )
                        }
                    }

                    // Achievements button
                    Button(
                        onClick = { onNavigate(GameScreen.Achievements) },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("achievements_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = NightSurface),
                        shape = RoundedCornerShape(25.dp),
                        border = BorderStroke(1.dp, NightSecondary.copy(alpha = 0.4f))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.MilitaryTech, contentDescription = null, tint = NightTertiary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "TROPHIES",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = NightTertiary
                                )
                            )
                        }
                    }
                }

                // Settings button (large wide secondary)
                Button(
                    onClick = { onNavigate(GameScreen.Settings) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("settings_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = NightSurface.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = null, tint = NightOnSurface.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "SETTINGS",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = NightOnSurface.copy(alpha = 0.7f)
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}


// --- SCREEN 2: CUSTOMIZE COCO (WARDROBE SHOP) ---
@Composable
fun CustomizeScreen(
    stats: UserStats,
    accessories: List<UnlockedAccessory>,
    onSelect: (String) -> Unit,
    onBuy: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Current equipped accessory info
    val currentSelected = stats.selectedAccessory

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(NightBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(NightSurface)
                        .testTag("back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = NightSecondary
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = "COCO'S WARDROBE",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Black,
                        color = NightOnBackground
                    )
                )

                Spacer(modifier = Modifier.weight(1f))

                // Coin balance display
                Surface(
                    color = NightSurface,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, NightTertiary.copy(alpha = 0.6f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(NightTertiary)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${stats.totalCoins}",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = NightTertiary
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Premium Preview Panel: Draw Coco with selected accessory
            Card(
                colors = CardDefaults.cardColors(containerColor = NightSurface),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                shape = MaterialTheme.shapes.large,
                border = BorderStroke(1.dp, NightPrimary.copy(alpha = 0.3f))
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    // Soft glowing moon spotlight inside wardrobe preview
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(
                            color = NightPrimary.copy(alpha = 0.1f),
                            radius = 65f * density,
                            center = Offset(size.width / 2f, size.height / 2f)
                        )
                    }

                    // Large mathematically drawn custom preview of Coco with equipped Hat/Scarf
                    PreviewCrow(accessoryId = currentSelected, modifier = Modifier.size(110.dp))

                    Text(
                        text = "Current: " + (accessories.find { it.id == currentSelected }?.name ?: "None"),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = NightOnSurface.copy(alpha = 0.6f),
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "AVAILABLE ACCESSORIES",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = NightOnBackground.copy(alpha = 0.7f),
                    letterSpacing = 1.5.sp
                ),
                modifier = Modifier.padding(bottom = 10.dp)
            )

            // Grid of accessories shop items
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(accessories) { acc ->
                    val isEquipped = acc.id == currentSelected
                    val canAfford = stats.totalCoins >= acc.cost

                    Card(
                        colors = CardDefaults.cardColors(containerColor = NightSurface),
                        border = BorderStroke(
                            width = if (isEquipped) 2.dp else 1.dp,
                            color = if (isEquipped) NightPrimary else NightOnSurface.copy(alpha = 0.15f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(12.dp)
                        ) {
                            // Small accessory graphic
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .padding(4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                PreviewAccessoryIcon(acc.id, modifier = Modifier.fillMaxSize())
                            }

                            Text(
                                text = acc.name,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = NightOnSurface
                                ),
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Interactive Purchase / Selection buttons
                            if (acc.isUnlocked) {
                                Button(
                                    onClick = { onSelect(acc.id) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isEquipped) Color.Gray else NightSecondary
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(36.dp)
                                ) {
                                    Text(
                                        text = if (isEquipped) "EQUIPPED" else "EQUIP",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = NightBackground
                                        )
                                    )
                                }
                            } else {
                                Button(
                                    onClick = { onBuy(acc.id) },
                                    enabled = canAfford,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = NightTertiary,
                                        disabledContainerColor = NightSurface.copy(alpha = 0.5f)
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(36.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(if (canAfford) NightBackground else Color.Gray)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "${acc.cost}",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = if (canAfford) NightBackground else Color.Gray
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
    }
}


// --- SCREEN 3: TROPHIES & ACHIEVEMENTS ---
@Composable
fun AchievementsScreen(
    achievements: List<UnlockedAchievement>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormatter = remember { SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(NightBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(NightSurface)
                        .testTag("back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = NightSecondary
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = "TROPHY ROOM",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Black,
                        color = NightOnBackground
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Score Progress Bar summary
            val completedCount = achievements.count { it.isUnlocked }
            val progressPercent = if (achievements.isNotEmpty()) completedCount.toFloat() / achievements.size else 0f

            Card(
                colors = CardDefaults.cardColors(containerColor = NightSurface),
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                border = BorderStroke(1.dp, NightTertiary.copy(alpha = 0.25f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "COMPLETION STATUS",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = NightOnSurface.copy(alpha = 0.7f)
                            )
                        )
                        Text(
                            text = "$completedCount / ${achievements.size} UNLOCKED",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = NightTertiary
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    LinearProgressIndicator(
                        progress = progressPercent,
                        color = NightTertiary,
                        trackColor = Color(0xFF2C244C),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Scrollable achievements list
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(achievements) { ach ->
                    val dateStr = if (ach.unlockedAt > 0) dateFormatter.format(Date(ach.unlockedAt)) else ""

                    Surface(
                        color = if (ach.isUnlocked) NightSurface else NightSurface.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (ach.isUnlocked) NightSecondary.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.05f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Badge Icon
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (ach.isUnlocked) NightTertiary.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.05f)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (ach.isUnlocked) Icons.Default.MilitaryTech else Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = if (ach.isUnlocked) NightTertiary else Color.Gray,
                                    modifier = Modifier.size(26.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = ach.title,
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (ach.isUnlocked) NightOnSurface else Color.Gray
                                    )
                                )
                                Text(
                                    text = ach.description,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = if (ach.isUnlocked) NightOnSurface.copy(alpha = 0.8f) else Color.Gray.copy(alpha = 0.7f)
                                    )
                                )
                                if (ach.isUnlocked && dateStr.isNotEmpty()) {
                                    Text(
                                        text = "Unlocked on $dateStr",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = NightSecondary.copy(alpha = 0.7f),
                                            fontSize = 9.sp
                                        ),
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


// --- SCREEN 4: GAME SETTINGS ---
@Composable
fun SettingsScreen(
    isSoundEnabled: Boolean,
    isVibrationEnabled: Boolean,
    onToggleSound: () -> Unit,
    onToggleVibration: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(NightBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(NightSurface)
                        .testTag("back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = NightSecondary
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = "SETTINGS",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Black,
                        color = NightOnBackground
                    )
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Settings toggles in clean M3 styling
            Card(
                colors = CardDefaults.cardColors(containerColor = NightSurface),
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    // Sound effects toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onToggleSound() }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isSoundEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                                contentDescription = null,
                                tint = NightSecondary
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text("Sound Effects", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold, color = NightOnSurface))
                                Text("Chirp and spark audio triggers", style = MaterialTheme.typography.bodySmall.copy(color = NightOnSurface.copy(alpha = 0.6f)))
                            }
                        }
                        Switch(
                            checked = isSoundEnabled,
                            onCheckedChange = { onToggleSound() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = NightSecondary,
                                checkedTrackColor = NightSecondary.copy(alpha = 0.5f)
                            )
                        )
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.05f))

                    // Vibration toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onToggleVibration() }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Vibration,
                                contentDescription = null,
                                tint = NightPrimary
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text("Haptic Vibration", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold, color = NightOnSurface))
                                Text("Flapping and collision feedback", style = MaterialTheme.typography.bodySmall.copy(color = NightOnSurface.copy(alpha = 0.6f)))
                            }
                        }
                        Switch(
                            checked = isVibrationEnabled,
                            onCheckedChange = { onToggleVibration() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = NightPrimary,
                                checkedTrackColor = NightPrimary.copy(alpha = 0.5f)
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Fun facts card
            Card(
                colors = CardDefaults.cardColors(containerColor = NightSurface.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                border = BorderStroke(1.dp, NightOnSurface.copy(alpha = 0.05f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "ABOUT COCO THE CROW",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = NightSecondary,
                            letterSpacing = 1.2.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Coco is a mischievous but friendly bird with rare blue-purple feather highlights and a sharp golden beak. He loves flying through the moonlit city at night, collecting gleaming artifacts while steering clear of chimneys and sparking power lines!",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = NightOnSurface.copy(alpha = 0.8f),
                            lineHeight = 20.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = "FlappyCrow v1.0.0 • AI Studio Build",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = NightOnSurface.copy(alpha = 0.4f)
                ),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}


// --- SCREEN 5: GAME OVER SCOREBOARD HUB ---
@Composable
fun GameOverScreen(
    score: Int,
    highScore: Int,
    coinsCollected: Int,
    isNewHighScore: Boolean,
    onTryAgain: () -> Unit,
    onMainMenu: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Elegant pulsing animation for high score alert
    val infiniteTransition = rememberInfiniteTransition(label = "highscore_pulse")
    val alertScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF070414))
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .padding(24.dp)
        ) {
            // "Game Over" title
            Text(
                text = "GAME OVER",
                style = MaterialTheme.typography.displayMedium.copy(
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFFF3366),
                    letterSpacing = 4.sp,
                    textAlign = TextAlign.Center
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Coco flew too close to the chimneys!",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = NightOnSurface.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )
            )

            Spacer(modifier = Modifier.height(28.dp))

            // High Score alert
            if (isNewHighScore) {
                Surface(
                    color = NightTertiary.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, NightTertiary),
                    modifier = Modifier
                        .scale(alertScale)
                        .padding(bottom = 20.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = NightTertiary)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "NEW PERSONAL RECORD!",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Black,
                                color = NightTertiary
                            )
                        )
                    }
                }
            }

            // Results Details Box
            Card(
                colors = CardDefaults.cardColors(containerColor = NightSurface),
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Final Score
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "SCORE",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = NightOnSurface.copy(alpha = 0.7f)
                            )
                        )
                        Text(
                            text = "$score",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Black,
                                color = NightSecondary
                            )
                        )
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.05f))

                    // Coins collected
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "COINS ACQUIRED",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = NightOnSurface.copy(alpha = 0.7f)
                            )
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(NightTertiary)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "+$coinsCollected",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = NightTertiary
                                )
                            )
                        }
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.05f))

                    // Best Score
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "PERSONAL BEST",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = NightOnSurface.copy(alpha = 0.7f)
                            )
                        )
                        Text(
                            text = "$highScore",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = NightOnSurface
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Action Buttons
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = onTryAgain,
                    colors = ButtonDefaults.buttonColors(containerColor = NightPrimary),
                    shape = RoundedCornerShape(25.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("try_again_button")
                ) {
                    Text(
                        text = "TRY AGAIN",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = NightBackground
                        )
                    )
                }

                Button(
                    onClick = onMainMenu,
                    colors = ButtonDefaults.buttonColors(containerColor = NightSurface),
                    shape = RoundedCornerShape(25.dp),
                    border = BorderStroke(1.dp, NightOnSurface.copy(alpha = 0.2f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("main_menu_button")
                ) {
                    Text(
                        text = "MAIN MENU",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = NightOnSurface
                        )
                    )
                }
            }
        }
    }
}


// --- HELPER GRAPHICS DRAWING COMPONENTS ---

@Composable
fun PreviewCrow(accessoryId: String, modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "preview_crow")
    val wingAngle by infiniteTransition.animateFloat(
        initialValue = -15f,
        targetValue = 15f,
        animationSpec = infiniteRepeatable(
            animation = tween(450, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wing_angle"
    )

    Canvas(modifier = modifier) {
        val cx = size.width / 2f
        val cy = size.height * 0.52f
        val r = size.width * 0.26f

        // Draw tail feathers
        val tail = Path().apply {
            moveTo(cx - r * 0.9f, cy)
            lineTo(cx - r * 1.5f, cy - r * 0.3f)
            lineTo(cx - r * 1.4f, cy)
            lineTo(cx - r * 1.5f, cy + r * 0.3f)
            close()
        }
        drawPath(tail, color = FeatherBlack)
        drawPath(tail, color = FeatherHighlight.copy(alpha = 0.3f), style = Stroke(width = 2f * density))

        // Draw body
        drawCircle(color = FeatherBlack, radius = r, center = Offset(cx, cy))
        drawCircle(color = FeatherHighlight, radius = r * 0.9f, center = Offset(cx, cy), style = Stroke(width = 2.5f * density))

        // Draw beak
        val beak = Path().apply {
            moveTo(cx + r * 0.8f, cy - r * 0.25f)
            lineTo(cx + r * 1.6f, cy)
            lineTo(cx + r * 0.8f, cy + r * 0.25f)
            close()
        }
        drawPath(beak, color = NightBeakGold)

        // Eye
        drawCircle(color = Color.White, radius = r * 0.32f, center = Offset(cx + r * 0.35f, cy - r * 0.32f))
        drawCircle(color = Color.Black, radius = r * 0.16f, center = Offset(cx + r * 0.4f, cy - r * 0.32f))
        drawCircle(color = Color.White, radius = r * 0.05f, center = Offset(cx + r * 0.45f, cy - r * 0.36f))

        // Wing
        val wingPivotX = cx - r * 0.1f
        val wingPivotY = cy + r * 0.1f
        val wingW = r * 1.1f
        val wingH = r * 0.6f

        // Let's do simple rotate mapping for previews
        val wingRotAngle = wingAngle
        
        // Draw wing shadow / background
        drawCircle(color = FeatherBlack, radius = r * 0.55f, center = Offset(wingPivotX, wingPivotY))

        // Draw accessory on preview
        drawAccessoryPreview(accessoryId, cx, cy, r, density)
    }
}

// Draw accessory preview onto prebuilt avatar
private fun DrawScope.drawAccessoryPreview(id: String, cx: Float, cy: Float, r: Float, density: Float) {
    // Reuses the identical drawing logic from FlappyCrowGame to keep preview fully integrated!
    val scaleX = density
    val scaleY = density

    when (id) {
        "detective_hat" -> {
            val hatColor = Color(0xFF5D4037)
            val hatTop = cy - r * 0.9f
            
            drawRoundRect(
                color = hatColor,
                topLeft = Offset(cx - r * 0.7f, hatTop - 3f * scaleY),
                size = Size(r * 1.6f, 5f * scaleY),
                cornerRadius = CornerRadius(2f * scaleX)
            )
            val crownPath = Path().apply {
                moveTo(cx - r * 0.4f, hatTop - 3f * scaleY)
                quadraticTo(
                    cx, hatTop - r * 0.9f,
                    cx + r * 0.4f, hatTop - 3f * scaleY
                )
                close()
            }
            drawPath(crownPath, color = hatColor)
            drawRect(
                color = Color.Black,
                topLeft = Offset(cx - r * 0.4f, hatTop - 4f * scaleY),
                size = Size(r * 0.8f, 2.5f * scaleY)
            )
        }

        "red_scarf" -> {
            val scarfColor = Color(0xFFD32F2F)
            val neckY = cy + r * 0.7f
            val neckX = cx - r * 0.2f

            drawRoundRect(
                color = scarfColor,
                topLeft = Offset(neckX - 4f * scaleX, neckY - 2f * scaleY),
                size = Size(r * 0.8f, 6f * scaleY),
                cornerRadius = CornerRadius(3f * scaleX)
            )

            // Preview tail waving softly
            val tailPath = Path().apply {
                moveTo(neckX - 2f * scaleX, neckY + 2f * scaleY)
                quadraticTo(
                    neckX - r * 0.7f, neckY + 4f * scaleY,
                    neckX - r * 1.3f, neckY
                )
                lineTo(neckX - r * 1.2f, neckY + 6f * scaleY)
                quadraticTo(
                    neckX - r * 0.6f, neckY + 8f * scaleY,
                    neckX + 4f * scaleX, neckY + 4f * scaleY
                )
                close()
            }
            drawPath(tailPath, color = scarfColor)
        }

        "wizard_hat" -> {
            val wizardBlue = Color(0xFF1E3A8A)
            val hatTop = cy - r * 0.9f
            
            drawRoundRect(
                color = wizardBlue,
                topLeft = Offset(cx - r * 0.8f, hatTop - 3f * scaleY),
                size = Size(r * 1.7f, 5f * scaleY),
                cornerRadius = CornerRadius(2f * scaleX)
            )

            val conePath = Path().apply {
                moveTo(cx - r * 0.4f, hatTop - 3f * scaleY)
                lineTo(cx + r * 0.4f, hatTop - 3f * scaleY)
                lineTo(cx - r * 0.6f, hatTop - r * 1.5f)
                close()
            }
            drawPath(conePath, color = wizardBlue)

            drawCircle(
                color = Color.Yellow,
                radius = 3f * scaleX,
                center = Offset(cx - r * 0.6f, hatTop - r * 1.5f)
            )
        }

        "golden_crown" -> {
            val crownGold = NightBeakGold
            val hatTop = cy - r * 0.9f

            val crownPath = Path().apply {
                moveTo(cx - r * 0.6f, hatTop)
                lineTo(cx - r * 0.6f, hatTop - r * 0.6f)
                lineTo(cx - r * 0.3f, hatTop - r * 0.3f)
                lineTo(cx, hatTop - r * 0.8f)
                lineTo(cx + r * 0.3f, hatTop - r * 0.3f)
                lineTo(cx + r * 0.6f, hatTop - r * 0.6f)
                lineTo(cx + r * 0.6f, hatTop)
                close()
            }
            drawPath(crownPath, color = crownGold)

            drawCircle(color = Color.Red, radius = 2f * scaleX, center = Offset(cx - r * 0.6f, hatTop - r * 0.6f))
            drawCircle(color = Color.Red, radius = 2.5f * scaleX, center = Offset(cx, hatTop - r * 0.8f))
            drawCircle(color = Color.Red, radius = 2f * scaleX, center = Offset(cx + r * 0.6f, hatTop - r * 0.6f))
        }

        "pilot_goggles" -> {
            val goggleBrown = Color(0xFF4E342E)
            val eyeX = cx + r * 0.4f
            val eyeY = cy - r * 0.3f
            val eyeRadius = r * 0.38f

            drawLine(
                color = goggleBrown,
                start = Offset(cx - r * 0.9f, eyeY),
                end = Offset(eyeX, eyeY),
                strokeWidth = 4f * scaleY
            )

            drawCircle(
                color = goggleBrown,
                radius = eyeRadius,
                center = Offset(eyeX, eyeY),
                style = Stroke(width = 3f * scaleX)
            )
            drawCircle(
                color = NightSecondary.copy(alpha = 0.5f),
                radius = eyeRadius * 0.7f,
                center = Offset(eyeX, eyeY)
            )
        }
    }
}

// Draw individual accessory items inside shop grids mathematically
@Composable
fun PreviewAccessoryIcon(id: String, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val r = size.width * 0.32f

        // Draw a clean stand/pedestal
        drawRoundRect(
            color = Color(0xFF2C254C),
            topLeft = Offset(cx - r, cy + r * 0.4f),
            size = Size(r * 2f, 8f * density),
            cornerRadius = CornerRadius(4f * density)
        )

        // Draw the specific accessory on the pedestal
        val scaleX = density
        val scaleY = density

        when (id) {
            "none" -> {
                // Cross mark indicating empty wardrobe slot
                drawLine(
                    color = Color.Gray,
                    start = Offset(cx - r * 0.5f, cy - r * 0.5f),
                    end = Offset(cx + r * 0.5f, cy + r * 0.5f),
                    strokeWidth = 3f * density
                )
                drawLine(
                    color = Color.Gray,
                    start = Offset(cx + r * 0.5f, cy - r * 0.5f),
                    end = Offset(cx - r * 0.5f, cy + r * 0.5f),
                    strokeWidth = 3f * density
                )
            }

            "detective_hat" -> {
                val hatColor = Color(0xFF5D4037)
                val hatTop = cy - r * 0.3f
                
                drawRoundRect(
                    color = hatColor,
                    topLeft = Offset(cx - r * 0.8f, hatTop - 3f * scaleY),
                    size = Size(r * 1.6f, 5f * scaleY),
                    cornerRadius = CornerRadius(2f * scaleX)
                )
                val crownPath = Path().apply {
                    moveTo(cx - r * 0.45f, hatTop - 3f * scaleY)
                    quadraticTo(
                        cx, hatTop - r * 0.9f,
                        cx + r * 0.45f, hatTop - 3f * scaleY
                    )
                    close()
                }
                drawPath(crownPath, color = hatColor)
                drawRect(
                    color = Color.Black,
                    topLeft = Offset(cx - r * 0.45f, hatTop - 4f * scaleY),
                    size = Size(r * 0.9f, 2.5f * scaleY)
                )
            }

            "red_scarf" -> {
                val scarfColor = Color(0xFFD32F2F)
                val neckY = cy - r * 0.2f
                val neckX = cx - r * 0.3f

                drawRoundRect(
                    color = scarfColor,
                    topLeft = Offset(neckX, neckY),
                    size = Size(r * 0.6f, 6f * scaleY),
                    cornerRadius = CornerRadius(3f * scaleX)
                )

                val tailPath = Path().apply {
                    moveTo(neckX + 2f * scaleX, neckY + 4f * scaleY)
                    quadraticTo(
                        neckX - r * 0.4f, neckY + 12f * scaleY,
                        neckX - r * 0.8f, neckY + 8f * scaleY
                    )
                    lineTo(neckX - r * 0.7f, neckY + 14f * scaleY)
                    quadraticTo(
                        neckX - r * 0.3f, neckY + 16f * scaleY,
                        neckX + r * 0.4f, neckY + 6f * scaleY
                    )
                    close()
                }
                drawPath(tailPath, color = scarfColor)
            }

            "wizard_hat" -> {
                val wizardBlue = Color(0xFF1E3A8A)
                val hatTop = cy + r * 0.2f
                
                drawRoundRect(
                    color = wizardBlue,
                    topLeft = Offset(cx - r * 0.9f, hatTop - 3f * scaleY),
                    size = Size(r * 1.8f, 5f * scaleY),
                    cornerRadius = CornerRadius(2f * scaleX)
                )

                val conePath = Path().apply {
                    moveTo(cx - r * 0.5f, hatTop - 3f * scaleY)
                    lineTo(cx + r * 0.5f, hatTop - 3f * scaleY)
                    lineTo(cx - r * 0.4f, hatTop - r * 1.3f)
                    close()
                }
                drawPath(conePath, color = wizardBlue)

                drawCircle(
                    color = Color.Yellow,
                    radius = 3.5f * scaleX,
                    center = Offset(cx - r * 0.4f, hatTop - r * 1.3f)
                )
            }

            "golden_crown" -> {
                val crownGold = NightBeakGold
                val hatTop = cy + r * 0.2f

                val crownPath = Path().apply {
                    moveTo(cx - r * 0.7f, hatTop)
                    lineTo(cx - r * 0.7f, hatTop - r * 0.6f)
                    lineTo(cx - r * 0.35f, hatTop - r * 0.3f)
                    lineTo(cx, hatTop - r * 0.8f)
                    lineTo(cx + r * 0.35f, hatTop - r * 0.3f)
                    lineTo(cx + r * 0.7f, hatTop - r * 0.6f)
                    lineTo(cx + r * 0.7f, hatTop)
                    close()
                }
                drawPath(crownPath, color = crownGold)

                drawCircle(color = Color.Red, radius = 2f * scaleX, center = Offset(cx - r * 0.7f, hatTop - r * 0.6f))
                drawCircle(color = Color.Red, radius = 2.5f * scaleX, center = Offset(cx, hatTop - r * 0.8f))
                drawCircle(color = Color.Red, radius = 2f * scaleX, center = Offset(cx + r * 0.7f, hatTop - r * 0.6f))
            }

            "pilot_goggles" -> {
                val goggleBrown = Color(0xFF4E342E)
                val eyeX = cx
                val eyeY = cy - r * 0.2f
                val eyeRadius = r * 0.45f

                drawLine(
                    color = goggleBrown,
                    start = Offset(cx - r, eyeY),
                    end = Offset(cx + r, eyeY),
                    strokeWidth = 4f * scaleY
                )

                // Left Rim
                drawCircle(
                    color = goggleBrown,
                    radius = eyeRadius,
                    center = Offset(eyeX - eyeRadius * 1.1f, eyeY),
                    style = Stroke(width = 3.5f * scaleX)
                )
                // Left lens
                drawCircle(
                    color = NightSecondary.copy(alpha = 0.5f),
                    radius = eyeRadius * 0.75f,
                    center = Offset(eyeX - eyeRadius * 1.1f, eyeY)
                )

                // Right Rim
                drawCircle(
                    color = goggleBrown,
                    radius = eyeRadius,
                    center = Offset(eyeX + eyeRadius * 1.1f, eyeY),
                    style = Stroke(width = 3.5f * scaleX)
                )
                // Right lens
                drawCircle(
                    color = NightSecondary.copy(alpha = 0.5f),
                    radius = eyeRadius * 0.75f,
                    center = Offset(eyeX + eyeRadius * 1.1f, eyeY)
                )
            }
        }
    }
}
