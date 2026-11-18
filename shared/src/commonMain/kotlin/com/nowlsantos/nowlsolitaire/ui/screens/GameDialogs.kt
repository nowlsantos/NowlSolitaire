package com.nowlsantos.nowlsolitaire.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nowlsantos.nowlsolitaire.domain.model.GameStats
import com.nowlsantos.nowlsolitaire.domain.model.PyramidRunRecord
import com.nowlsantos.nowlsolitaire.domain.model.PyramidRunSession
import com.nowlsantos.nowlsolitaire.ui.components.formatDuration
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun NewGameDialog(
    onNewDeal: () -> Unit,
    onReplayDeal: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Start a Deal") },
        text = {
            Text(text = "Deal a fresh shuffled deck or replay the current deal from the beginning.")
        },
        confirmButton = {
            Button(onClick = onNewDeal) {
                Text("New Deal")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
                OutlinedButton(onClick = onReplayDeal) {
                    Text("Replay")
                }
            }
        }
    )
}

@Composable
fun NoMovesDialog(
    canUndo: Boolean,
    onUndo: () -> Unit,
    onReplayDeal: () -> Unit,
    onNewDeal: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Round Over — No More Moves") },
        text = {
            Text(
                text = if (canUndo) {
                    "There are no more moves available to solve this deal. You have reached an impasse with no path to win. You can undo previous moves to try another path, replay this deal from the beginning, or start a new game."
                } else {
                    "No moves are available to solve this deal. You can replay this deal from the beginning or start a new game."
                }
            )
        },
        confirmButton = {
            Button(onClick = onNewDeal) {
                Text("New Deal")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (canUndo) {
                    TextButton(onClick = onUndo) {
                        Text("Undo")
                    }
                }
                OutlinedButton(onClick = onReplayDeal) {
                    Text("Replay")
                }
            }
        }
    )
}

@Composable
fun StatsDialog(
    stats: GameStats,
    onResetStats: () -> Unit,
    onDismiss: () -> Unit,
    pyramidTopRuns: List<PyramidRunRecord> = emptyList()
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Statistics") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                StatRow("Games Played", stats.gamesPlayed.toString())
                StatRow("Games Won", stats.gamesWon.toString())
                StatRow("Win Rate", "${stats.winRatePercent}%")
                StatRow("Current Streak", stats.currentStreak.toString())
                StatRow("Best Streak", stats.bestStreak.toString())
                StatRow("Best Score", stats.bestScore.toString())
                StatRow(
                    "Best Time",
                    stats.bestTimeSeconds?.let(::formatDuration) ?: "--:--"
                )

                if (pyramidTopRuns.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Top Pyramid Runs",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    pyramidTopRuns.forEachIndexed { index, run ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "#${index + 1}  Round ${run.roundReached}",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${run.totalScore} pts (${run.maxStreak} streak)",
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Close")
            }
        },
        dismissButton = {
            TextButton(onClick = onResetStats) {
                Text("Reset")
            }
        }
    )
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun WinOverlay(
    score: Int,
    elapsedSeconds: Int,
    moveCount: Int,
    onReplay: () -> Unit,
    onNewDeal: () -> Unit,
    showTimer: Boolean = false
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x99000000)),
        contentAlignment = Alignment.Center
    ) {
        ConfettiOverlay(modifier = Modifier.fillMaxSize())

        Surface(
            shape = RoundedCornerShape(20.dp),
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth(0.86f)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.EmojiEvents,
                    contentDescription = null,
                    tint = Color(0xFFFFB300),
                    modifier = Modifier.size(52.dp)
                )
                Text(
                    text = "You Won!",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    StatRow("Score", score.toString())
                    if (showTimer) {
                        StatRow("Time", formatDuration(elapsedSeconds))
                    }
                    StatRow("Moves", moveCount.toString())
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onReplay,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Replay")
                    }
                    Button(
                        onClick = onNewDeal,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("New Deal")
                    }
                }
            }
        }
    }
}

@Composable
fun PyramidRoundWinOverlay(
    round: Int,
    roundScore: Int,
    totalRunScore: Int,
    streak: Int,
    lives: Int,
    maxLives: Int,
    elapsedSeconds: Int,
    moveCount: Int,
    onNextRound: () -> Unit,
    onReplay: () -> Unit,
    showTimer: Boolean = false
) {
    val bonusLifeAwarded = streak == 3 || streak == 6 || (streak > 6 && streak % 3 == 0)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x99000000)),
        contentAlignment = Alignment.Center
    ) {
        ConfettiOverlay(modifier = Modifier.fillMaxSize())

        Surface(
            shape = RoundedCornerShape(20.dp),
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.EmojiEvents,
                    contentDescription = null,
                    tint = Color(0xFFFFB300),
                    modifier = Modifier.size(52.dp)
                )
                Text(
                    text = "Round $round Cleared! 🎉",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                if (bonusLifeAwarded) {
                    Text(
                        text = "❤️ Bonus Life Earned! ($lives/$maxLives Lives)",
                        color = Color(0xFFE53935),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else if (streak > 1) {
                    val needed = 3 - (streak % 3)
                    Text(
                        text = "🔥 Win Streak: $streak ($needed more for +1 Life)",
                        color = Color(0xFFFF9800),
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    StatRow("Round Score", "+$roundScore pts")
                    StatRow("Total Run Score", "$totalRunScore pts")
                    if (showTimer) {
                        StatRow("Time", formatDuration(elapsedSeconds))
                    }
                    StatRow("Moves", moveCount.toString())
                }

                val nextRound = round + 1
                val nextTier = PyramidRunSession.getDifficultyTier(nextRound)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                ) {
                    Text(
                        text = "Next: Round $nextRound • $nextTier",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onNextRound,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Next Round (Round $nextRound)")
                    }
                    OutlinedButton(
                        onClick = onReplay,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Replay Deal")
                    }
                }
            }
        }
    }
}

@Composable
fun PyramidLifeLostDialog(
    remainingLives: Int,
    canUndo: Boolean,
    onUndo: () -> Unit,
    onRetryDeal: () -> Unit,
    onContinueNextDeal: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Round Over — 1 Life Lost 💔") },
        text = {
            Text(
                text = "You reached an impasse with no available moves left.\n\nYou have $remainingLives ${if (remainingLives == 1) "life" else "lives"} remaining."
            )
        },
        confirmButton = {
            Button(onClick = onContinueNextDeal) {
                Text("Next Deal")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (canUndo) {
                    TextButton(onClick = onUndo) {
                        Text("Undo")
                    }
                }
                OutlinedButton(onClick = onRetryDeal) {
                    Text("Retry Deal")
                }
            }
        }
    )
}

@Composable
fun PyramidGameOverDialog(
    finalRound: Int,
    totalRunScore: Int,
    bestStreak: Int,
    onStartNewRun: () -> Unit,
    onDismiss: () -> Unit,
    canUndo: Boolean = false,
    onUndo: (() -> Unit)? = null
) {
    val tier = PyramidRunSession.getDifficultyTier(finalRound)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Game Over — Out of Lives 💔") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("All lives have been consumed.")
                Spacer(modifier = Modifier.height(4.dp))
                StatRow("Final Round Reached", "Round $finalRound")
                StatRow("Difficulty Tier", tier)
                StatRow("Total Run Score", "$totalRunScore pts")
                StatRow("Longest Streak", "$bestStreak wins")
            }
        },
        confirmButton = {
            Button(onClick = onStartNewRun) {
                Text("Start New Run (3 Lives)")
            }
        },
        dismissButton = if (canUndo && onUndo != null) {
            {
                TextButton(onClick = onUndo) {
                    Text("Undo")
                }
            }
        } else null
    )
}

private data class ConfettiPiece(
    val xFraction: Float,
    val yOffset: Float,
    val speed: Float,
    val sizeDp: Float,
    val phase: Float,
    val color: Color
)

@Composable
private fun ConfettiOverlay(modifier: Modifier = Modifier) {
    val palette = remember {
        listOf(
            Color(0xFFFFD54F),
            Color(0xFF4FC3F7),
            Color(0xFF81C784),
            Color(0xFFE57373),
            Color(0xFFBA68C8)
        )
    }
    val pieces = remember {
        val rng = Random(77)
        List(44) { idx ->
            ConfettiPiece(
                xFraction = rng.nextFloat(),
                yOffset = rng.nextFloat(),
                speed = 0.6f + rng.nextFloat() * 0.9f,
                sizeDp = 6f + rng.nextFloat() * 6f,
                phase = rng.nextFloat() * 6.28f,
                color = palette[idx % palette.size]
            )
        }
    }

    val transition = rememberInfiniteTransition(label = "winConfetti")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "confettiFall"
    )

    Canvas(modifier = modifier) {
        pieces.forEach { piece ->
            val yFrac = (piece.yOffset + progress * piece.speed) % 1.1f - 0.05f
            val sway = sin((progress * 6.28f * 2f + piece.phase).toDouble()).toFloat() * 14.dp.toPx()
            val cx = piece.xFraction * size.width + sway
            val cy = yFrac * size.height
            val sizePx = piece.sizeDp.dp.toPx()
            val angle = (progress * 360f * piece.speed + piece.phase * 57f) % 360f

            rotate(degrees = angle, pivot = Offset(cx, cy)) {
                drawRect(
                    color = piece.color.copy(alpha = 0.9f),
                    topLeft = Offset(cx - sizePx / 2f, cy - sizePx * 0.3f),
                    size = Size(sizePx, sizePx * 0.6f)
                )
            }
        }
    }
}
