package com.example.ui.game

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
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
import kotlin.math.absoluteValue
import kotlin.math.sin
import kotlin.math.cos
import kotlin.math.PI
import kotlin.random.Random

// --- SCREEN 1: MAIN MENU ---
@Composable
fun MainMenuScreen(
    stats: UserStats,
    onNavigate: (GameScreen) -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "menu_bobbing")
    val bobbingOffset by infiniteTransition.animateFloat(
        initialValue = -12f,
        targetValue = 12f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bobbing"
    )

    val starsOpacity by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "twinkle"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(VoidColor, NightColor)
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        // Starry Night and Large Moon Background
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width
            val cy = size.height * 0.25f
            val r = 130f * density

            // 1. Twinkling Stars
            val starPositions = listOf(
                Offset(size.width * 0.15f, size.height * 0.12f),
                Offset(size.width * 0.35f, size.height * 0.08f),
                Offset(size.width * 0.78f, size.height * 0.15f),
                Offset(size.width * 0.25f, size.height * 0.35f),
                Offset(size.width * 0.88f, size.height * 0.45f),
                Offset(size.width * 0.08f, size.height * 0.55f),
                Offset(size.width * 0.65f, size.height * 0.28f),
                Offset(size.width * 0.45f, size.height * 0.52f)
            )
            starPositions.forEachIndexed { i, pos ->
                drawCircle(
                    color = SparkleWhite.copy(alpha = if (i % 2 == 0) starsOpacity else 1f - starsOpacity),
                    radius = (2f + (i % 3)) * density,
                    center = pos
                )
            }

            // 2. Large Moon
            drawCircle(
                color = AmethystColor.copy(alpha = 0.05f),
                radius = r + 40f * density,
                center = Offset(cx * 0.5f, cy + 40f * density)
            )
            drawCircle(
                color = MoonColor.copy(alpha = 0.12f),
                radius = r,
                center = Offset(cx * 0.5f, cy + 40f * density)
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 32.dp)
        ) {
            // Header kicker
            Text(
                text = "MOBILE ARCADE ADVENTURE",
                style = MaterialTheme.typography.labelMedium.copy(
                    color = AmethystColor.copy(alpha = 0.8f),
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 3.sp
                ),
                modifier = Modifier.padding(top = 16.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Game Title with Neon text effect
            Text(
                text = "FlappyCrow",
                style = MaterialTheme.typography.displayLarge.copy(
                    fontWeight = FontWeight.Black,
                    color = MoonColor,
                    fontSize = 42.sp,
                    letterSpacing = 1.sp,
                    textAlign = TextAlign.Center
                ),
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.shadow(0.dp)
            )

            Text(
                text = "A Moonlit Adventure",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = CyanColor,
                    letterSpacing = 2.sp
                ),
                modifier = Modifier.padding(top = 4.dp)
            )

            // High Score banner
            Surface(
                color = Navy2Color.copy(alpha = 0.6f),
                shape = RoundedCornerShape(100),
                border = BorderStroke(1.dp, AmethystColor.copy(alpha = 0.4f)),
                modifier = Modifier.padding(top = 18.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = "High Score",
                        tint = GoldColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "BEST: ${stats.highScore}",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = MoonColor,
                            letterSpacing = 1.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Animated Bobbing Crow Avatar sitting on a Chimney
            Box(
                modifier = Modifier
                    .size(190.dp)
                    .offset(y = bobbingOffset.dp),
                contentAlignment = Alignment.Center
            ) {
                // Pedestal/Chimney base
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val cx = size.width / 2f
                    val cy = size.height * 0.52f
                    val r = size.width * 0.26f

                    drawRoundRect(
                        color = Navy2Color,
                        topLeft = Offset(cx - r * 0.9f, cy + r * 0.75f),
                        size = Size(r * 1.8f, r * 1.2f),
                        cornerRadius = CornerRadius(6f * density)
                    )
                    drawRect(
                        color = MagentaColor,
                        topLeft = Offset(cx - r * 1.05f, cy + r * 0.75f),
                        size = Size(r * 2.1f, 8f * density)
                    )
                }

                // High quality animated Coco with selected accessory
                PreviewCrow(
                    accessoryId = stats.selectedAccessory,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Menu Buttons: Overhauled visual hierarchy
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxWidth(0.9f)
            ) {
                // Play Button (Neon Gradient)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(MagentaColor, PurpleColor)
                            )
                        )
                        .clickable { onNavigate(GameScreen.Game) }
                        .testTag("play_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = MoonColor, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "PLAY GAME",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = MoonColor,
                                letterSpacing = 2.sp
                            )
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Customize button
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .clip(RoundedCornerShape(25.dp))
                            .background(Navy2Color.copy(alpha = 0.8f))
                            .border(BorderStroke(1.dp, AmethystColor.copy(alpha = 0.4f)), RoundedCornerShape(25.dp))
                            .clickable { onNavigate(GameScreen.Customize) }
                            .testTag("customize_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.Checkroom, contentDescription = null, tint = CyanColor, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "WARDROBE",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = CyanColor,
                                    letterSpacing = 1.sp
                                )
                            )
                        }
                    }

                    // Achievements button
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .clip(RoundedCornerShape(25.dp))
                            .background(Navy2Color.copy(alpha = 0.8f))
                            .border(BorderStroke(1.dp, AmethystColor.copy(alpha = 0.4f)), RoundedCornerShape(25.dp))
                            .clickable { onNavigate(GameScreen.Achievements) }
                            .testTag("achievements_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.MilitaryTech, contentDescription = null, tint = GoldColor, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "TROPHIES",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = GoldColor,
                                    letterSpacing = 1.sp
                                )
                            )
                        }
                    }
                }

                // Settings button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(VoidColor.copy(alpha = 0.5f))
                        .border(BorderStroke(1.dp, DimColor.copy(alpha = 0.2f)), RoundedCornerShape(24.dp))
                        .clickable { onNavigate(GameScreen.Settings) }
                        .testTag("settings_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = null, tint = DimColor, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "SETTINGS",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = DimColor,
                                letterSpacing = 1.sp
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
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
    val currentSelected = stats.selectedAccessory
    var selectedTab by remember { mutableStateOf("Hats") }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(VoidColor, NightColor)
                )
            )
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
                        .background(Navy2Color.copy(alpha = 0.6f))
                        .border(BorderStroke(1.dp, AmethystColor.copy(alpha = 0.3f)), CircleShape)
                        .size(40.dp)
                        .testTag("back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = MoonColor
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = "Coco's Closet",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Black,
                        color = MoonColor,
                        letterSpacing = 1.sp
                    )
                )

                Spacer(modifier = Modifier.weight(1f))

                // Coin balance display
                Surface(
                    color = Navy2Color.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(100),
                    border = BorderStroke(1.dp, GoldColor.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(GoldColor)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${stats.totalCoins}",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = GoldColor
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Premium Preview Panel with Glowing border and platform
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.radialGradient(
                            colors = listOf(Navy2Color.copy(alpha = 0.9f), VoidColor)
                        )
                    )
                    .border(BorderStroke(1.5.dp, PurpleColor.copy(alpha = 0.5f)), RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                // Platform platform glow
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 24.dp)
                        .width(160.dp)
                        .height(16.dp)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(MagentaColor.copy(alpha = 0.4f), Color.Transparent)
                            )
                        )
                )

                // Mathematically drawn custom preview
                PreviewCrow(accessoryId = currentSelected, modifier = Modifier.size(130.dp))

                // "Equipped" badge
                Surface(
                    color = CyanColor,
                    shape = RoundedCornerShape(100),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(14.dp)
                ) {
                    Text(
                        text = "EQUIPPED",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Black,
                            color = VoidColor,
                            letterSpacing = 1.sp
                        ),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tab selection (Hats, Glasses, Extras, Trails)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Hats", "Glasses", "Extras", "Trails").forEach { tab ->
                    val isSelected = selectedTab == tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) PurpleColor else Navy2Color.copy(alpha = 0.4f))
                            .border(
                                BorderStroke(
                                    width = 1.dp,
                                    color = if (isSelected) AmethystColor else AmethystColor.copy(alpha = 0.15f)
                                ),
                                RoundedCornerShape(10.dp)
                            )
                            .clickable { selectedTab = tab },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tab,
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) MoonColor else DimColor
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Filter items based on selected tab
            val filteredAccessories = when (selectedTab) {
                "Hats" -> accessories.filter { it.id in listOf("wizard_hat", "golden_crown", "detective_hat") }
                "Glasses" -> accessories.filter { it.id == "pilot_goggles" }
                "Extras" -> accessories.filter { it.id == "red_scarf" }
                else -> accessories.filter { it.id == "none" }
            }

            // Grid of accessories shop items
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(filteredAccessories) { acc ->
                    val isEquipped = acc.id == currentSelected
                    val canAfford = stats.totalCoins >= acc.cost

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(Navy2Color.copy(alpha = 0.6f))
                            .border(
                                BorderStroke(
                                    width = if (isEquipped) 2.dp else 1.dp,
                                    color = if (isEquipped) CyanColor else AmethystColor.copy(alpha = 0.2f)
                                ),
                                RoundedCornerShape(20.dp)
                            )
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(12.dp)
                        ) {
                            // Small accessory graphic
                            Box(
                                modifier = Modifier
                                    .size(76.dp)
                                    .padding(4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                PreviewAccessoryIcon(acc.id, modifier = Modifier.fillMaxSize())
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = acc.name,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MoonColor
                                ),
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Interactive Purchase / Selection buttons
                            if (acc.isUnlocked) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(36.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isEquipped) Color.Gray.copy(alpha = 0.3f) else CyanColor)
                                        .clickable { onSelect(acc.id) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (isEquipped) "EQUIPPED" else "EQUIP",
                                        style = MaterialTheme.typography.labelLarge.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (isEquipped) MoonColor.copy(alpha = 0.5f) else VoidColor
                                        )
                                    )
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(36.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (canAfford) GoldColor else Color.Gray.copy(alpha = 0.2f))
                                        .clickable(enabled = canAfford) { onBuy(acc.id) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(if (canAfford) VoidColor else Color.Gray)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "${acc.cost}",
                                            style = MaterialTheme.typography.labelLarge.copy(
                                                fontWeight = FontWeight.ExtraBold,
                                                color = if (canAfford) VoidColor else Color.Gray
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
            .background(
                Brush.verticalGradient(
                    colors = listOf(VoidColor, NightColor)
                )
            )
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
                        .background(Navy2Color.copy(alpha = 0.6f))
                        .border(BorderStroke(1.dp, AmethystColor.copy(alpha = 0.3f)), CircleShape)
                        .size(40.dp)
                        .testTag("back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = MoonColor
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = "Trophies",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Black,
                            color = MoonColor,
                            letterSpacing = 1.sp
                        )
                    )
                    Text(
                        text = "COCO'S JOURNEY",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = AmethystColor,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Score Progress Bar summary
            val completedCount = achievements.count { it.isUnlocked }
            val progressPercent = if (achievements.isNotEmpty()) completedCount.toFloat() / achievements.size else 0f

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Navy2Color.copy(alpha = 0.6f))
                    .border(BorderStroke(1.dp, AmethystColor.copy(alpha = 0.25f)), RoundedCornerShape(20.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "TOTAL PROGRESS",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = DimColor,
                                letterSpacing = 1.sp
                            )
                        )
                        Text(
                            text = "$completedCount / ${achievements.size}",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                color = GoldColor
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    // Overhauled custom progress indicator with double-gradient
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(VoidColor)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(progressPercent)
                                .clip(RoundedCornerShape(5.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(CyanColor, MagentaColor)
                                    )
                                )
                        )
                    }
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

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(Navy2Color.copy(alpha = if (ach.isUnlocked) 0.6f else 0.3f))
                            .border(
                                BorderStroke(
                                    width = 1.dp,
                                    color = if (ach.isUnlocked) GoldColor.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.05f)
                                ),
                                RoundedCornerShape(18.dp)
                            )
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Badge Icon on Left
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        if (ach.isUnlocked) {
                                            Brush.radialGradient(
                                                colors = listOf(GoldColor, Color(0xFFC48A10))
                                            )
                                        } else {
                                            Brush.verticalGradient(
                                                colors = listOf(Navy2Color, VoidColor)
                                            )
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (ach.isUnlocked) Icons.Default.MilitaryTech else Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = if (ach.isUnlocked) VoidColor else DimColor,
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = ach.title,
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (ach.isUnlocked) MoonColor else DimColor
                                    )
                                )
                                Text(
                                    text = ach.description,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = if (ach.isUnlocked) DimColor else DimColor.copy(alpha = 0.6f)
                                    )
                                )
                                if (ach.isUnlocked && dateStr.isNotEmpty()) {
                                    Text(
                                        text = "Unlocked on $dateStr",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = CyanColor.copy(alpha = 0.7f),
                                            fontSize = 9.sp
                                        ),
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // Claimed / Reward Indicator
                            Column(
                                horizontalAlignment = Alignment.End,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(GoldColor)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (ach.id == "golden_wings") "+5" else "+50",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Black,
                                            color = if (ach.isUnlocked) GoldColor else DimColor
                                        )
                                    )
                                }
                                Text(
                                    text = if (ach.isUnlocked) "Claimed" else "Locked",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = if (ach.isUnlocked) CyanColor else DimColor.copy(alpha = 0.5f),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
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
    var sfxVolume by remember { mutableStateOf(0.7f) }
    var musicVolume by remember { mutableStateOf(0.45f) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(VoidColor, NightColor)
                )
            )
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
                        .background(Navy2Color.copy(alpha = 0.6f))
                        .border(BorderStroke(1.dp, AmethystColor.copy(alpha = 0.3f)), CircleShape)
                        .size(40.dp)
                        .testTag("back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = MoonColor
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Black,
                        color = MoonColor,
                        letterSpacing = 1.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // AUDIO CONTROLS GROUP
            Text(
                text = "AUDIO",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = AmethystColor,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                ),
                modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Navy2Color.copy(alpha = 0.6f))
                    .border(BorderStroke(1.dp, AmethystColor.copy(alpha = 0.15f)), RoundedCornerShape(20.dp))
            ) {
                Column {
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
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(PurpleColor, VoidColor)
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isSoundEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                                    contentDescription = null,
                                    tint = AmethystColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text("Sound Effects", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold, color = MoonColor))
                                Text("Coin chimes & flap sounds", style = MaterialTheme.typography.bodySmall.copy(color = DimColor))
                            }
                        }
                        Switch(
                            checked = isSoundEnabled,
                            onCheckedChange = { onToggleSound() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = CyanColor,
                                checkedTrackColor = CyanColor.copy(alpha = 0.4f),
                                uncheckedThumbColor = Color.Gray,
                                uncheckedTrackColor = Color.Gray.copy(alpha = 0.3f)
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
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(PurpleColor, VoidColor)
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Vibration,
                                    contentDescription = null,
                                    tint = AmethystColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text("Vibration", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold, color = MoonColor))
                                Text("Haptic feedback on taps", style = MaterialTheme.typography.bodySmall.copy(color = DimColor))
                            }
                        }
                        Switch(
                            checked = isVibrationEnabled,
                            onCheckedChange = { onToggleVibration() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MagentaColor,
                                checkedTrackColor = MagentaColor.copy(alpha = 0.4f),
                                uncheckedThumbColor = Color.Gray,
                                uncheckedTrackColor = Color.Gray.copy(alpha = 0.3f)
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // SLIDERS/LEVELS GROUP
            Text(
                text = "LEVELS",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = AmethystColor,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                ),
                modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Navy2Color.copy(alpha = 0.6f))
                    .border(BorderStroke(1.dp, AmethystColor.copy(alpha = 0.15f)), RoundedCornerShape(20.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // SFX Slider
                    Text("SFX Volume", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = MoonColor))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Slider(
                            value = sfxVolume,
                            onValueChange = { sfxVolume = it },
                            colors = SliderDefaults.colors(
                                thumbColor = CyanColor,
                                activeTrackColor = CyanColor,
                                inactiveTrackColor = VoidColor
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "${(sfxVolume * 100).toInt()}",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = MoonColor),
                            modifier = Modifier.width(28.dp),
                            textAlign = TextAlign.End
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Music Slider
                    Text("Music Volume", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = MoonColor))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Slider(
                            value = musicVolume,
                            onValueChange = { musicVolume = it },
                            colors = SliderDefaults.colors(
                                thumbColor = MagentaColor,
                                activeTrackColor = MagentaColor,
                                inactiveTrackColor = VoidColor
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "${(musicVolume * 100).toInt()}",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = MoonColor),
                            modifier = Modifier.width(28.dp),
                            textAlign = TextAlign.End
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // GENERAL GROUP
            Text(
                text = "GENERAL",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = AmethystColor,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                ),
                modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Navy2Color.copy(alpha = 0.6f))
                    .border(BorderStroke(1.dp, AmethystColor.copy(alpha = 0.15f)), RoundedCornerShape(20.dp))
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Language, contentDescription = null, tint = DimColor)
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text("Language", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold, color = MoonColor))
                                Text("English", style = MaterialTheme.typography.bodySmall.copy(color = DimColor))
                            }
                        }
                        Icon(Icons.Default.NavigateNext, contentDescription = null, tint = AmethystColor)
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.05f))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Mail, contentDescription = null, tint = DimColor)
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text("Contact Support", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold, color = MoonColor))
                                Text("We answer within a moonrise", style = MaterialTheme.typography.bodySmall.copy(color = DimColor))
                            }
                        }
                        Icon(Icons.Default.NavigateNext, contentDescription = null, tint = AmethystColor)
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Footer
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(VoidColor.copy(alpha = 0.5f))
                        .border(BorderStroke(1.dp, AmethystColor.copy(alpha = 0.3f)), RoundedCornerShape(22.dp))
                        .clickable { /* mock restore */ },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Restore Purchases",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MoonColor
                        )
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "FlappyCrow v1.4.2 · Coco says hi 🌙",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = DimColor.copy(alpha = 0.6f),
                        letterSpacing = 1.sp
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}


// --- SCREEN 5: GAME OVER SCOREBOARD ---
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
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF240A3F), Color(0xFF0A0620))
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(24.dp)
        ) {
            // High Score alert badge
            if (isNewHighScore) {
                Surface(
                    color = GoldColor,
                    shape = RoundedCornerShape(100),
                    modifier = Modifier
                        .scale(alertScale)
                        .padding(bottom = 12.dp)
                ) {
                    Text(
                        text = "★ NEW BEST!",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Black,
                            color = VoidColor,
                            letterSpacing = 1.5.sp
                        ),
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }

            Text(
                text = "Oh Feathers",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = MagentaColor,
                    letterSpacing = 4.sp
                )
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "GAME OVER",
                style = MaterialTheme.typography.displayLarge.copy(
                    fontWeight = FontWeight.Black,
                    color = MoonColor,
                    textAlign = TextAlign.Center
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Sad math-drawn Coco with tear on canvas
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .padding(bottom = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    val r = 28f * density

                    // 0. Aura
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                AmethystColor.copy(alpha = 0.2f),
                                Color.Transparent
                            ),
                            center = Offset(cx, cy),
                            radius = r * 1.6f
                        ),
                        radius = r * 1.6f,
                        center = Offset(cx, cy)
                    )

                    // 1. Tail Feathers
                    val tailPath = Path().apply {
                        moveTo(cx - r * 0.7f, cy)
                        lineTo(cx - r * 1.5f, cy - r * 0.2f)
                        quadraticTo(cx - r * 1.7f, cy, cx - r * 1.5f, cy + r * 0.2f)
                        close()
                    }
                    drawPath(tailPath, color = Color(0xFF141121))
                    drawPath(tailPath, color = AmethystColor.copy(alpha = 0.5f), style = Stroke(1.5f * density))

                    // 2. Body Gradient
                    val bodyPath = Path().apply {
                        moveTo(cx + r * 0.85f, cy - r * 0.1f)
                        quadraticTo(cx + r * 1.05f, cy + r * 0.4f, cx + r * 0.4f, cy + r * 0.95f)
                        quadraticTo(cx - r * 0.6f, cy + r * 1.05f, cx - r * 0.95f, cy + r * 0.2f)
                        quadraticTo(cx - r * 0.85f, cy - r * 0.85f, cx + r * 0.1f, cy - r * 0.95f)
                        close()
                    }
                    drawPath(
                        bodyPath,
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFF3B2A63), Color(0xFF1F1836), Color(0xFF0F0B1A)),
                            center = Offset(cx + r * 0.2f, cy - r * 0.2f),
                            radius = r * 1.35f
                        )
                    )
                    drawPath(bodyPath, color = AmethystColor.copy(alpha = 0.6f), style = Stroke(width = 2f * density))

                    // Beak facing down
                    val beak = Path().apply {
                        moveTo(cx + r * 0.7f, cy)
                        lineTo(cx + r * 1.35f, cy + r * 0.45f)
                        lineTo(cx + r * 0.7f, cy + r * 0.3f)
                        close()
                    }
                    val beakGradient = Brush.verticalGradient(
                        colors = listOf(Color(0xFFFFF59D), Color(0xFFFFB300), Color(0xFFE65100)),
                        startY = cy,
                        endY = cy + r * 0.45f
                    )
                    drawPath(beak, brush = beakGradient)

                    // Sad Closed Eyes (sleeping curved lines)
                    val eyeX = cx + r * 0.35f
                    val eyeY = cy - r * 0.2f
                    val eyeW = r * 0.4f
                    val eyePath = Path().apply {
                        moveTo(eyeX - eyeW / 2f, eyeY)
                        quadraticTo(eyeX, eyeY + eyeW / 2f, eyeX + eyeW / 2f, eyeY)
                    }
                    drawPath(eyePath, color = Color.White, style = Stroke(width = 2.5f * density, cap = StrokeCap.Round))

                    // Crying tear rolling down
                    drawCircle(
                        color = CyanColor,
                        radius = 4f * density,
                        center = Offset(eyeX + 4f * density, eyeY + 12f * density)
                    )
                }
            }

            // Results Details Box (Overhauled to look premium)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Navy2Color.copy(alpha = 0.8f))
                    .border(BorderStroke(1.5.dp, MagentaColor.copy(alpha = 0.4f)), RoundedCornerShape(24.dp))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Final Score
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "FINAL SCORE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = AmethystColor,
                                letterSpacing = 2.sp
                            )
                        )
                        Text(
                            text = "$score",
                            style = MaterialTheme.typography.displayLarge.copy(
                                fontWeight = FontWeight.Black,
                                color = MoonColor
                            )
                        )
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.05f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Personal Best
                        Column(horizontalAlignment = Alignment.Start) {
                            Text("BEST", style = MaterialTheme.typography.labelSmall.copy(color = DimColor, fontWeight = FontWeight.Bold))
                            Text("$highScore", style = MaterialTheme.typography.titleMedium.copy(color = MoonColor, fontWeight = FontWeight.ExtraBold))
                        }

                        // Coins acquired
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("COINS", style = MaterialTheme.typography.labelSmall.copy(color = DimColor, fontWeight = FontWeight.Bold))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(GoldColor))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("+$coinsCollected", style = MaterialTheme.typography.titleMedium.copy(color = GoldColor, fontWeight = FontWeight.ExtraBold))
                            }
                        }

                        // Gems acquired (relative gems)
                        Column(horizontalAlignment = Alignment.End) {
                            Text("GEMS", style = MaterialTheme.typography.labelSmall.copy(color = DimColor, fontWeight = FontWeight.Bold))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(AmethystColor)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("+${(score / 12).coerceAtLeast(0)}", style = MaterialTheme.typography.titleMedium.copy(color = AmethystColor, fontWeight = FontWeight.ExtraBold))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Action Buttons
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Retry button (Magenta)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .clip(RoundedCornerShape(27.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(MagentaColor, PurpleColor)
                            )
                        )
                        .clickable { onTryAgain() }
                        .testTag("try_again_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "RETRY FLIGHT",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = MoonColor,
                            letterSpacing = 1.5.sp
                        )
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Home button
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .clip(RoundedCornerShape(25.dp))
                            .background(Navy2Color.copy(alpha = 0.7f))
                            .border(BorderStroke(1.dp, AmethystColor.copy(alpha = 0.3f)), RoundedCornerShape(25.dp))
                            .clickable { onMainMenu() }
                            .testTag("main_menu_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.Home, contentDescription = null, tint = MoonColor, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Home",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MoonColor
                                )
                            )
                        }
                    }

                    // Share button
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .clip(RoundedCornerShape(25.dp))
                            .background(Navy2Color.copy(alpha = 0.7f))
                            .border(BorderStroke(1.dp, AmethystColor.copy(alpha = 0.3f)), RoundedCornerShape(25.dp))
                            .clickable { /* share mock */ },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, tint = MoonColor, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Share",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MoonColor
                                )
                            )
                        }
                    }
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

        // 0. Outer Magical Aura Halo
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    CyanColor.copy(alpha = 0.25f),
                    AmethystColor.copy(alpha = 0.15f),
                    Color.Transparent
                ),
                center = Offset(cx, cy),
                radius = r * 1.8f
            ),
            radius = r * 1.8f,
            center = Offset(cx, cy)
        )

        // 1. Multi-Layered Tail Feathers Fan
        for (i in -1..1) {
            val spreadAngle = i * 16f
            rotate(degrees = spreadAngle, pivot = Offset(cx - r * 0.7f, cy)) {
                val tailPath = Path().apply {
                    moveTo(cx - r * 0.7f, cy)
                    lineTo(cx - r * 1.65f, cy - r * 0.22f)
                    quadraticTo(
                        cx - r * 1.85f, cy,
                        cx - r * 1.65f, cy + r * 0.22f
                    )
                    close()
                }
                drawPath(
                    tailPath,
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFF141121), Color(0xFF261D42), Color(0xFF382963)),
                        start = Offset(cx - r * 0.7f, cy),
                        end = Offset(cx - r * 1.8f, cy)
                    )
                )
                drawPath(
                    tailPath,
                    color = CyanColor.copy(alpha = 0.4f),
                    style = Stroke(width = 1.5f * density)
                )
            }
        }

        // 2. Feet Claws
        val feetColor = Color(0xFFFF9800)
        val leftFootPath = Path().apply {
            moveTo(cx - r * 0.2f, cy + r * 0.85f)
            lineTo(cx - r * 0.35f, cy + r * 1.15f)
            lineTo(cx - r * 0.15f, cy + r * 1.12f)
            lineTo(cx - r * 0.05f, cy + r * 1.15f)
            close()
        }
        val rightFootPath = Path().apply {
            moveTo(cx + r * 0.15f, cy + r * 0.85f)
            lineTo(cx + r * 0.05f, cy + r * 1.15f)
            lineTo(cx + r * 0.2f, cy + r * 1.12f)
            lineTo(cx + r * 0.3f, cy + r * 1.15f)
            close()
        }
        drawPath(leftFootPath, color = feetColor)
        drawPath(rightFootPath, color = feetColor)

        // 3. Crow Main Body (Organic teardrop shape + Radial Obsidian/Amethyst Gradient)
        val bodyPath = Path().apply {
            moveTo(cx + r * 0.85f, cy - r * 0.1f)
            quadraticTo(
                cx + r * 1.05f, cy + r * 0.4f,
                cx + r * 0.4f, cy + r * 0.95f
            )
            quadraticTo(
                cx - r * 0.6f, cy + r * 1.05f,
                cx - r * 0.95f, cy + r * 0.2f
            )
            quadraticTo(
                cx - r * 0.85f, cy - r * 0.85f,
                cx + r * 0.1f, cy - r * 0.95f
            )
            close()
        }

        drawPath(
            bodyPath,
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF3B2A63),
                    Color(0xFF1F1836),
                    Color(0xFF0F0B1A)
                ),
                center = Offset(cx + r * 0.2f, cy - r * 0.2f),
                radius = r * 1.35f
            )
        )

        // Scruffy Feather Crest Tufts on top/back of head
        val headTufts = Path().apply {
            moveTo(cx - r * 0.1f, cy - r * 0.92f)
            quadraticTo(cx - r * 0.25f, cy - r * 1.35f, cx - r * 0.02f, cy - r * 1.05f)
            quadraticTo(cx - r * 0.45f, cy - r * 1.25f, cx - r * 0.25f, cy - r * 0.85f)
            quadraticTo(cx - r * 0.75f, cy - r * 1.05f, cx - r * 0.55f, cy - r * 0.65f)
            close()
        }
        drawPath(
            headTufts,
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF4A3875), Color(0xFF1A132C)),
                startY = cy - r * 1.35f,
                endY = cy - r * 0.65f
            )
        )
        drawPath(headTufts, color = CyanColor.copy(alpha = 0.6f), style = Stroke(width = 1.6f * density))

        // Cute Scruffy Cheek Feather Fluff
        val cheekFluff = Path().apply {
            moveTo(cx - r * 0.15f, cy + r * 0.15f)
            quadraticTo(cx - r * 0.55f, cy + r * 0.25f, cx - r * 0.25f, cy + r * 0.45f)
            quadraticTo(cx - r * 0.6f, cy + r * 0.55f, cx - r * 0.15f, cy + r * 0.65f)
            close()
        }
        drawPath(cheekFluff, color = Color(0xFF231A3D))
        drawPath(cheekFluff, color = AmethystColor.copy(alpha = 0.5f), style = Stroke(width = 1.4f * density))

        drawPath(
            bodyPath,
            brush = Brush.sweepGradient(
                colors = listOf(
                    CyanColor.copy(alpha = 0.85f),
                    Color(0xFFE2D6FF).copy(alpha = 0.7f),
                    AmethystColor.copy(alpha = 0.65f),
                    CyanColor.copy(alpha = 0.85f)
                ),
                center = Offset(cx, cy)
            ),
            style = Stroke(width = 2.5f * density)
        )

        val chestFluff = Path().apply {
            moveTo(cx + r * 0.45f, cy + r * 0.25f)
            quadraticTo(cx + r * 0.25f, cy + r * 0.45f, cx + r * 0.5f, cy + r * 0.6f)
            quadraticTo(cx + r * 0.25f, cy + r * 0.75f, cx + r * 0.35f, cy + r * 0.85f)
        }
        drawPath(chestFluff, color = AmethystColor.copy(alpha = 0.5f), style = Stroke(width = 2f * density))

        // 4. 3D Sculpted Golden Beak
        val upperBeak = Path().apply {
            moveTo(cx + r * 0.75f, cy - r * 0.25f)
            quadraticTo(
                cx + r * 1.35f, cy - r * 0.28f,
                cx + r * 1.85f, cy + r * 0.05f
            )
            lineTo(cx + r * 0.78f, cy + r * 0.05f)
            close()
        }
        val lowerBeak = Path().apply {
            moveTo(cx + r * 0.78f, cy + r * 0.05f)
            lineTo(cx + r * 1.85f, cy + r * 0.05f)
            quadraticTo(
                cx + r * 1.25f, cy + r * 0.38f,
                cx + r * 0.75f, cy + r * 0.28f
            )
            close()
        }

        val beakGradient = Brush.verticalGradient(
            colors = listOf(Color(0xFFFFF59D), Color(0xFFFFB300), Color(0xFFE65100)),
            startY = cy - r * 0.3f,
            endY = cy + r * 0.3f
        )
        drawPath(upperBeak, brush = beakGradient)
        drawPath(lowerBeak, brush = beakGradient)

        drawLine(
            color = Color.White.copy(alpha = 0.9f),
            start = Offset(cx + r * 0.85f, cy - r * 0.22f),
            end = Offset(cx + r * 1.6f, cy - r * 0.02f),
            strokeWidth = 1.8f * density,
            cap = StrokeCap.Round
        )

        drawCircle(
            color = Color(0xFF422100),
            radius = r * 0.05f,
            center = Offset(cx + r * 0.95f, cy - r * 0.16f)
        )

        // 5. Expressive High-Quality Anime Eyes (Glowing Amber Gold Iris)
        val eyeX = cx + r * 0.42f
        val eyeY = cy - r * 0.32f
        val eyeRadius = r * 0.38f

        drawCircle(
            color = Color.White,
            radius = eyeRadius,
            center = Offset(eyeX, eyeY)
        )

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFFFF176), Color(0xFFFFB300), Color(0xFFE65100), Color(0xFF4E1D00)),
                center = Offset(eyeX + eyeRadius * 0.1f, eyeY - eyeRadius * 0.05f),
                radius = eyeRadius * 0.72f
            ),
            radius = eyeRadius * 0.72f,
            center = Offset(eyeX + eyeRadius * 0.15f, eyeY)
        )

        drawCircle(
            color = Color(0xFF100A03),
            radius = eyeRadius * 0.42f,
            center = Offset(eyeX + eyeRadius * 0.18f, eyeY)
        )

        drawCircle(
            color = Color.White,
            radius = eyeRadius * 0.26f,
            center = Offset(eyeX + eyeRadius * 0.32f, eyeY - eyeRadius * 0.25f)
        )

        drawCircle(
            color = Color.White.copy(alpha = 0.85f),
            radius = eyeRadius * 0.13f,
            center = Offset(eyeX - eyeRadius * 0.08f, eyeY + eyeRadius * 0.25f)
        )

        // Cheeky Eyebrow / Brow Feather Ridge
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
            style = Stroke(width = 3.5f * density, cap = StrokeCap.Round)
        )
        drawPath(
            browRidge,
            color = CyanColor.copy(alpha = 0.75f),
            style = Stroke(width = 1.5f * density, cap = StrokeCap.Round)
        )

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFFF5252).copy(alpha = 0.55f), Color.Transparent),
                center = Offset(eyeX + eyeRadius * 0.1f, eyeY + eyeRadius * 1.15f),
                radius = r * 0.28f
            ),
            radius = r * 0.28f,
            center = Offset(eyeX + eyeRadius * 0.1f, eyeY + eyeRadius * 1.15f)
        )

        // 6. Multi-Layered Feather Wings
        val wingWidth = r * 1.35f
        val wingHeight = r * 0.8f
        val wingPivotX = cx - r * 0.1f
        val wingPivotY = cy + r * 0.12f

        rotate(degrees = wingAngle, pivot = Offset(wingPivotX, wingPivotY)) {
            val spread = ((wingAngle + 15f) / 30f).coerceIn(0f, 1f)

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
            drawPath(feather1, color = CyanColor.copy(alpha = 0.85f), style = Stroke(width = 2f * density))

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
            drawPath(feather2, color = AmethystColor.copy(alpha = 0.8f), style = Stroke(width = 1.6f * density))

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
            drawPath(feather3, color = CyanColor.copy(alpha = 0.6f), style = Stroke(width = 1.3f * density))
        }

        // Draw accessory on preview
        drawAccessoryPreview(accessoryId, cx, cy, r, density)
    }
}

// Draw accessory preview onto prebuilt avatar
private fun DrawScope.drawAccessoryPreview(id: String, cx: Float, cy: Float, r: Float, density: Float) {
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
            val crownGold = GoldColor
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
                color = CyanColor.copy(alpha = 0.5f),
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

        // Draw a clean pedestal
        drawRoundRect(
            color = VoidColor,
            topLeft = Offset(cx - r, cy + r * 0.4f),
            size = Size(r * 2f, 8f * density),
            cornerRadius = CornerRadius(4f * density)
        )

        val scaleX = density
        val scaleY = density

        when (id) {
            "none" -> {
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
                val crownGold = GoldColor
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

                drawCircle(
                    color = goggleBrown,
                    radius = eyeRadius,
                    center = Offset(eyeX - eyeRadius * 1.1f, eyeY),
                    style = Stroke(width = 3.5f * scaleX)
                )
                drawCircle(
                    color = CyanColor.copy(alpha = 0.5f),
                    radius = eyeRadius * 0.75f,
                    center = Offset(eyeX - eyeRadius * 1.1f, eyeY)
                )

                drawCircle(
                    color = goggleBrown,
                    radius = eyeRadius,
                    center = Offset(eyeX + eyeRadius * 1.1f, eyeY),
                    style = Stroke(width = 3.5f * scaleX)
                )
                drawCircle(
                    color = CyanColor.copy(alpha = 0.5f),
                    radius = eyeRadius * 0.75f,
                    center = Offset(eyeX + eyeRadius * 1.1f, eyeY)
                )
            }
        }
    }
}
