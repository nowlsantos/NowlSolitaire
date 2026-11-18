package com.nowlsantos.nowlsolitaire.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nowlsantos.nowlsolitaire.ui.theme.TableColors

@Composable
fun GameTopBar(
    score: Int,
    elapsedSeconds: Int,
    moveCount: Int,
    colors: TableColors,
    onOpenStats: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    showTimer: Boolean = false,
    roundNumber: Int? = null,
    lives: Int? = null,
    maxLives: Int = 3,
    streak: Int? = null
) {
    val containerSize = LocalWindowInfo.current.containerSize
    val isLandscape = containerSize.width > containerSize.height
    val vPadding = if (isLandscape) 3.dp else 6.dp
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.barSurface)
            .padding(horizontal = 12.dp, vertical = vPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back to Lobby",
                        tint = colors.barContent
                    )
                }
            }
            if (roundNumber != null) {
                HudMetric(label = "ROUND", value = roundNumber.toString(), contentColor = colors.barContent)
            }
            if (lives != null) {
                HudLives(lives = lives, maxLives = maxLives, contentColor = colors.barContent)
            }
            if (streak != null && streak > 0) {
                HudStreak(streak = streak, contentColor = colors.barContent)
            }
            HudMetric(label = "SCORE", value = score.toString(), contentColor = colors.barContent)
            if (showTimer) {
                HudMetric(label = "TIME", value = formatDuration(elapsedSeconds), contentColor = colors.barContent)
            }
            HudMetric(label = "MOVES", value = moveCount.toString(), contentColor = colors.barContent)
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onOpenStats) {
                Icon(
                    imageVector = Icons.Rounded.BarChart,
                    contentDescription = "Statistics",
                    tint = colors.barContent
                )
            }
            IconButton(onClick = onOpenSettings) {
                Icon(
                    imageVector = Icons.Rounded.Settings,
                    contentDescription = "Settings",
                    tint = colors.barContent
                )
            }
        }
    }
}

@Composable
fun GameBottomBar(
    canUndo: Boolean,
    isAutoCompleteAvailable: Boolean,
    colors: TableColors,
    onNewDeal: () -> Unit,
    onUndo: () -> Unit,
    onHint: () -> Unit,
    onAutoComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerSize = LocalWindowInfo.current.containerSize
    val isLandscape = containerSize.width > containerSize.height
    val vPadding = if (isLandscape) 3.dp else 6.dp
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AnimatedVisibility(
            visible = isAutoCompleteAvailable,
            enter = fadeIn() + expandVertically() + scaleIn(),
            exit = fadeOut() + shrinkVertically() + scaleOut()
        ) {
            Button(
                onClick = onAutoComplete,
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.selectionGlow,
                    contentColor = Color(0xFF1A1A1A)
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 6.dp,
                    pressedElevation = 2.dp
                ),
                modifier = Modifier.padding(bottom = if (isLandscape) 4.dp else 10.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Auto-Complete",
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = colors.barSurface
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = vPadding),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BottomBarAction(
                    icon = Icons.Rounded.Refresh,
                    label = "New Deal",
                    enabled = true,
                    contentColor = colors.barContent,
                    onClick = onNewDeal
                )
                BottomBarAction(
                    icon = Icons.AutoMirrored.Rounded.Undo,
                    label = "Undo",
                    enabled = canUndo,
                    contentColor = colors.barContent,
                    onClick = onUndo
                )
                BottomBarAction(
                    icon = Icons.Rounded.Lightbulb,
                    label = "Hint",
                    enabled = true,
                    contentColor = colors.barContent,
                    onClick = onHint
                )
            }
        }
    }
}

@Composable
private fun BottomBarAction(
    icon: ImageVector,
    label: String,
    enabled: Boolean,
    contentColor: Color,
    onClick: () -> Unit
) {
    val effectiveColor = if (enabled) contentColor else contentColor.copy(alpha = 0.35f)
    TextButton(
        onClick = onClick,
        enabled = enabled
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = effectiveColor,
                modifier = Modifier.size(22.dp)
            )
            Text(
                text = label,
                color = effectiveColor,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun HudMetric(
    label: String,
    value: String,
    contentColor: Color
) {
    Column {
        Text(
            text = label,
            color = contentColor.copy(alpha = 0.65f),
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.8.sp
        )
        Text(
            text = value,
            color = contentColor,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun HudLives(
    lives: Int,
    maxLives: Int,
    contentColor: Color
) {
    Column {
        Text(
            text = "LIVES",
            color = contentColor.copy(alpha = 0.65f),
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.8.sp
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(1.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(maxLives) { index ->
                val isFilled = index < lives
                Text(
                    text = if (isFilled) "❤️" else "🖤",
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun HudStreak(
    streak: Int,
    contentColor: Color
) {
    Column {
        Text(
            text = "STREAK",
            color = Color(0xFFFFA000),
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.8.sp
        )
        Text(
            text = "🔥$streak",
            color = Color(0xFFFFB300),
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

fun formatDuration(totalSeconds: Int): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val minStr = minutes.toString().padStart(2, '0')
    val secStr = seconds.toString().padStart(2, '0')
    return "$minStr:$secStr"
}
