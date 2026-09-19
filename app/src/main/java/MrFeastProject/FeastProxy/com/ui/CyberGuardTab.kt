package MrFeastProject.FeastProxy.com.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import MrFeastProject.FeastProxy.com.BlockedTrackerItem
import MrFeastProject.FeastProxy.com.CyberGuardEngine
import MrFeastProject.FeastProxy.com.SettingsStore

@Composable
fun CyberGuardTab(settingsStore: SettingsStore) {
    val context = LocalContext.current
    val isProtectionActive by CyberGuardEngine.isProtectionActive.collectAsStateWithLifecycle()
    val blockedList by CyberGuardEngine.blockedItems.collectAsStateWithLifecycle()
    val totalBlocked by CyberGuardEngine.totalBlockedCount.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        CyberGuardEngine.init(context)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "CyberGuard",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // Hero Card with Animated Google Protect style Shield & Rotating Green Aura
        item {
            GoogleProtectShieldHero(
                isActive = isProtectionActive,
                totalBlocked = totalBlocked,
                onToggle = { active ->
                    CyberGuardEngine.setProtectionActive(active, context)
                }
            )
        }

        // Header for Blocked Trackers List
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ЖУРНАЛ ЗАБЛОКИРОВАННЫХ ТРЕКЕРОВ",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Всего: $totalBlocked",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Infinite / continuous list of blocked trackers
        items(
            items = blockedList,
            key = { it.id }
        ) { item ->
            BlockedTrackerRow(item = item)
        }

        if (blockedList.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Ожидание трафика... Трекеры перехватываются автоматически.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

/**
 * Animated Google Protect-style Shield with rotating neon green aura.
 */
@Composable
private fun GoogleProtectShieldHero(
    isActive: Boolean,
    totalBlocked: Long,
    onToggle: (Boolean) -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "protect_aura")

    // Continuous smooth rotation for the green aura (Google Protect style)
    val auraRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "aura_rotation"
    )

    val auraCounterRotation by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "aura_counter_rotation"
    )

    val auraPulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isActive) 1.15f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "aura_pulse"
    )

    val activeGreen = Color(0xFF00E676)
    val darkGreen = Color(0xFF00B0FF)
    val inactiveGray = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)

    Surface(
        shape = RoundedCornerShape(26.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            if (isActive) activeGreen.copy(alpha = 0.35f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Shield Container with Google Protect rotating aura
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(150.dp)
                    .padding(8.dp)
            ) {
                if (isActive) {
                    // Outer Pulsing Glow
                    Box(
                        modifier = Modifier
                            .size(135.dp)
                            .scale(auraPulse)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(activeGreen.copy(alpha = 0.25f), Color.Transparent)
                                )
                            )
                    )

                    // Outer Rotating Aura Ring 1
                    Box(
                        modifier = Modifier
                            .size(130.dp)
                            .rotate(auraRotation)
                            .clip(CircleShape)
                            .border(
                                BorderStroke(
                                    3.5.dp,
                                    Brush.sweepGradient(
                                        listOf(
                                            Color.Transparent,
                                            activeGreen.copy(alpha = 0.2f),
                                            activeGreen,
                                            Color.Transparent
                                        )
                                    )
                                ),
                                shape = CircleShape
                            )
                    )

                    // Inner Counter-Rotating Aura Ring 2
                    Box(
                        modifier = Modifier
                            .size(112.dp)
                            .rotate(auraCounterRotation)
                            .clip(CircleShape)
                            .border(
                                BorderStroke(
                                    2.dp,
                                    Brush.sweepGradient(
                                        listOf(
                                            Color.Transparent,
                                            activeGreen,
                                            Color(0xFF69F0AE),
                                            Color.Transparent
                                        )
                                    )
                                ),
                                shape = CircleShape
                            )
                    )
                }

                // Center Shield Core
                Surface(
                    shape = CircleShape,
                    color = if (isActive) activeGreen.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    border = BorderStroke(
                        1.5.dp,
                        if (isActive) activeGreen.copy(alpha = 0.7f) else inactiveGray
                    ),
                    modifier = Modifier.size(86.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "CyberGuard Shield",
                            tint = if (isActive) activeGreen else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            modifier = Modifier.size(46.dp)
                        )
                    }
                }
            }

            // Name
            Text(
                text = "CyberGuard",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.5.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            // Description
            Text(
                text = "Протокол CyberGuard блокирует на вашем устройстве популярные сервисы для отслеживания IP адеросов для приватности",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 4.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
            )

            // Control Bar with Switch and Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (isActive) activeGreen else MaterialTheme.colorScheme.error)
                    )
                    Text(
                        text = if (isActive) "Защита активна (127.0.0.1)" else "Защита отключена",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = if (isActive) activeGreen else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Switch(
                    checked = isActive,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = activeGreen
                    )
                )
            }
        }
    }
}

/**
 * List Item for each blocked tracker:
 * [App Avatar] [App Name & Domain] [Tracker Badge] [Time & Date: 00:00 - 00.00.0000]
 */
@Composable
private fun BlockedTrackerRow(item: BlockedTrackerItem) {
    val context = LocalContext.current
    val appDrawable = remember(item.packageName) {
        CyberGuardEngine.getAppIcon(context, item.packageName)
    }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Left: App Avatar / Icon
            AppAvatar(drawable = appDrawable, appName = item.appName)

            // Middle: App Name & Blocked Domain
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = item.appName,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = item.domain,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Right-Center: "Tracker" badge
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFFFF5252).copy(alpha = 0.12f),
                border = BorderStroke(1.dp, Color(0xFFFF5252).copy(alpha = 0.35f))
            ) {
                Text(
                    text = "Tracker",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    ),
                    color = Color(0xFFFF5252),
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                )
            }

            // Far Right: Date & Time in format 00:00 - 00.00.0000
            Text(
                text = item.formattedDateTime,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                textAlign = TextAlign.End
            )
        }
    }
}

@Composable
private fun AppAvatar(drawable: Drawable?, appName: String) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
        contentAlignment = Alignment.Center
    ) {
        if (drawable != null) {
            val bitmap = remember(drawable) {
                drawableToBitmap(drawable)
            }
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = appName,
                    modifier = Modifier.size(32.dp)
                )
            } else {
                FallbackAvatar(appName)
            }
        } else {
            FallbackAvatar(appName)
        }
    }
}

@Composable
private fun FallbackAvatar(appName: String) {
    val initial = appName.firstOrNull()?.uppercase() ?: "A"
    Text(
        text = initial,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.primary
    )
}

private fun drawableToBitmap(drawable: Drawable): Bitmap? {
    return try {
        if (drawable is BitmapDrawable && drawable.bitmap != null) {
            return drawable.bitmap
        }
        val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 72
        val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 72
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        bitmap
    } catch (e: Exception) {
        null
    }
}
