package com.nowlsantos.nowlsolitaire.ui.components.pyramid

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nowlsantos.nowlsolitaire.domain.model.CardBackTheme
import com.nowlsantos.nowlsolitaire.domain.model.PyramidSettings
import com.nowlsantos.nowlsolitaire.domain.model.TableTheme
import com.nowlsantos.nowlsolitaire.ui.components.getCardBackDrawableRes
import com.nowlsantos.nowlsolitaire.ui.state.PyramidIntent
import com.nowlsantos.nowlsolitaire.ui.theme.toColors
import org.jetbrains.compose.resources.painterResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PyramidSettingsBottomSheet(
    settings: PyramidSettings,
    onIntent: (PyramidIntent) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
    )
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Pyramid Settings",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            // Redeal limit
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Stock Redeals",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FilterChip(
                        selected = settings.maxRecycles == 2,
                        onClick = { onIntent(PyramidIntent.UpdateMaxRecycles(2)) },
                        label = { Text("2 Redeals (Standard)") }
                    )
                    FilterChip(
                        selected = settings.maxRecycles == 1,
                        onClick = { onIntent(PyramidIntent.UpdateMaxRecycles(1)) },
                        label = { Text("1 Redeal") }
                    )
                    FilterChip(
                        selected = settings.maxRecycles < 0,
                        onClick = { onIntent(PyramidIntent.UpdateMaxRecycles(-1)) },
                        label = { Text("Unlimited") }
                    )
                }
            }

            // Table Felt
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Table Felt",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    TableTheme.entries.forEach { theme ->
                        val palette = theme.toColors()
                        val selected = settings.tableTheme == theme
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable {
                                onIntent(PyramidIntent.UpdateTableTheme(theme))
                            }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(palette.feltPrimary)
                                    .border(
                                        width = if (selected) 3.dp else 1.dp,
                                        color = if (selected) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            Color.Gray.copy(alpha = 0.5f)
                                        },
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (selected) {
                                    Icon(
                                        imageVector = Icons.Rounded.Check,
                                        contentDescription = theme.displayName,
                                        tint = Color.White
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = theme.displayName,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }

            // Card Back Theme
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Card Back",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CardBackTheme.entries.forEach { theme ->
                        val selected = settings.cardBackTheme == theme
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable {
                                onIntent(PyramidIntent.UpdateCardBackTheme(theme))
                            }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(width = 48.dp, height = 66.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .border(
                                        width = if (selected) 2.5.dp else 1.dp,
                                        color = if (selected) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            Color.Gray.copy(alpha = 0.4f)
                                        },
                                        shape = RoundedCornerShape(6.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = painterResource(getCardBackDrawableRes(theme)),
                                    contentDescription = theme.displayName,
                                    modifier = Modifier.fillMaxSize()
                                )
                                if (selected) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = 0.28f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(22.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primary),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Check,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = theme.displayName,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }

            HorizontalDivider()

            // Timer Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Show Timer",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Display elapsed game time in the top bar",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = settings.showTimer,
                    onCheckedChange = { onIntent(PyramidIntent.UpdateShowTimer(it)) }
                )
            }
        }
    }
}
