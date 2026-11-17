package com.nowlsantos.nowlsolitaire.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nowlsantos.nowlsolitaire.domain.model.TableTheme
import com.nowlsantos.nowlsolitaire.ui.theme.NowlSolitaireTheme
import com.nowlsantos.nowlsolitaire.ui.theme.toColors

@Composable
fun LobbyScreen(
    onSelectKlondike: () -> Unit,
    onSelectPyramid: () -> Unit,
    modifier: Modifier = Modifier,
    tableTheme: TableTheme = TableTheme.CLASSIC_GREEN
) {
    val colors = tableTheme.toColors()
    val snackbarHostState = remember { SnackbarHostState() }

    val backgroundBrush = remember(colors.feltPrimary, colors.feltSecondary) {
        Brush.verticalGradient(
            colors = listOf(colors.feltPrimary, colors.feltSecondary)
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundBrush)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // App Header / Hero Section
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(Color(0x2EFFFFFF))
                    .border(2.dp, colors.slotStroke, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "♠",
                    fontSize = 38.sp,
                    color = colors.barContent,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "NowlSolitaire",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Black,
                color = colors.barContent,
                letterSpacing = 1.sp
            )

            Text(
                text = "Select a Solitaire game mode to play",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.barContent.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(36.dp))

            // Game Mode Cards
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Klondike Solitaire (Available)
                GameModeCard(
                    title = "Klondike Solitaire",
                    subtitle = "Classic 7-column tableau with stock & foundations. Draw 1 or Draw 3.",
                    icon = "♠ ♥ ♦ ♣",
                    statusTag = "PLAY NOW",
                    statusColor = Color(0xFF2E7D32),
                    actionIcon = Icons.Rounded.PlayArrow,
                    enabled = true,
                    onClick = onSelectKlondike
                )

                // 2. Pyramid Solitaire (Available)
                GameModeCard(
                    title = "Pyramid Solitaire",
                    subtitle = "Pair open cards totaling 13 to clear the 28-card pyramid. Play Now!",
                    icon = "▲ 13",
                    statusTag = "PLAY NOW",
                    statusColor = Color(0xFF2E7D32),
                    actionIcon = Icons.Rounded.PlayArrow,
                    enabled = true,
                    onClick = onSelectPyramid
                )
            }

            Spacer(modifier = Modifier.weight(1f, fill = false))
            Spacer(modifier = Modifier.height(32.dp))

            // Footer note
            Text(
                text = "More Solitaire variants coming soon",
                style = MaterialTheme.typography.labelSmall,
                color = colors.barContent.copy(alpha = 0.5f),
                letterSpacing = 0.5.sp
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 16.dp)
        )
    }
}

@Composable
private fun GameModeCard(
    title: String,
    subtitle: String,
    icon: String,
    statusTag: String,
    statusColor: Color,
    actionIcon: ImageVector,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (enabled) Color(0xF2FCFBF7) else Color(0xD9EAE8E3)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (enabled) 6.dp else 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Card icon box
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (enabled) Color(0xFF1D4E89) else Color(0xFF6B7280)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = icon,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Status Badge
                Surface(
                    shape = RoundedCornerShape(50),
                    color = statusColor.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, statusColor.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = statusTag,
                        color = statusColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = if (enabled) Color(0xFF1D1F24) else Color(0xFF616161)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = if (enabled) Color(0xFF555555) else Color(0xFF757575),
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = if (enabled) "Play Game" else "In Progress",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (enabled) Color(0xFF1D4E89) else Color(0xFF757575)
                    )
                    Icon(
                        imageVector = if (enabled) Icons.AutoMirrored.Rounded.ArrowForward else actionIcon,
                        contentDescription = null,
                        tint = if (enabled) Color(0xFF1D4E89) else Color(0xFF757575),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 915)
@Composable
private fun LobbyScreenPreview() {
    NowlSolitaireTheme {
        LobbyScreen(
            onSelectKlondike = {},
            onSelectPyramid = {}
        )
    }
}
