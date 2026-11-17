package com.nowlsantos.nowlsolitaire.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.nowlsantos.nowlsolitaire.domain.model.TableTheme

@Immutable
data class TableColors(
    val feltPrimary: Color,
    val feltSecondary: Color,
    val slotStroke: Color,
    val slotFill: Color,
    val barSurface: Color,
    val barContent: Color,
    val cardFront: Color,
    val cardBorder: Color,
    val cardRed: Color,
    val cardBlack: Color,
    val cardBackPrimary: Color,
    val cardBackAccent: Color,
    val selectionGlow: Color,
    val hintGlow: Color
)

fun TableTheme.toColors(): TableColors = when (this) {
    TableTheme.CLASSIC_GREEN -> TableColors(
        feltPrimary = Color(0xFF145A32),
        feltSecondary = Color(0xFF0E4224),
        slotStroke = Color(0x66FFFFFF),
        slotFill = Color(0x1F000000),
        barSurface = Color(0xCC0A2E1A),
        barContent = Color(0xFFF7F9F7),
        cardFront = Color(0xFFFCFBF7),
        cardBorder = Color(0xFFCFCAC0),
        cardRed = Color(0xFFC62828),
        cardBlack = Color(0xFF1D1F24),
        cardBackPrimary = Color(0xFF1D4E89),
        cardBackAccent = Color(0xFF4D80B8),
        selectionGlow = Color(0xFFFFD54F),
        hintGlow = Color(0xFF69F0AE)
    )

    TableTheme.MIDNIGHT_BLUE -> TableColors(
        feltPrimary = Color(0xFF16324F),
        feltSecondary = Color(0xFF0E2136),
        slotStroke = Color(0x66DCE8F5),
        slotFill = Color(0x24050D17),
        barSurface = Color(0xCC0A1828),
        barContent = Color(0xFFF0F6FC),
        cardFront = Color(0xFFFCFBF7),
        cardBorder = Color(0xFFCFCAC0),
        cardRed = Color(0xFFD32F2F),
        cardBlack = Color(0xFF1B1F27),
        cardBackPrimary = Color(0xFF8B2635),
        cardBackAccent = Color(0xFFC05663),
        selectionGlow = Color(0xFFFFD54F),
        hintGlow = Color(0xFF40C4FF)
    )

    TableTheme.ROYAL_BURGUNDY -> TableColors(
        feltPrimary = Color(0xFF5D1A28),
        feltSecondary = Color(0xFF3E1019),
        slotStroke = Color(0x66FCE4EC),
        slotFill = Color(0x26000000),
        barSurface = Color(0xCC2C0A11),
        barContent = Color(0xFFFFF5F7),
        cardFront = Color(0xFFFCFBF7),
        cardBorder = Color(0xFFCFCAC0),
        cardRed = Color(0xFFC62828),
        cardBlack = Color(0xFF1D1F24),
        cardBackPrimary = Color(0xFF1F4E5F),
        cardBackAccent = Color(0xFF4B8499),
        selectionGlow = Color(0xFFFFD54F),
        hintGlow = Color(0xFFFFAB40)
    )

    TableTheme.SLATE_DARK -> TableColors(
        feltPrimary = Color(0xFF23272E),
        feltSecondary = Color(0xFF16191E),
        slotStroke = Color(0x55E0E0E0),
        slotFill = Color(0x29000000),
        barSurface = Color(0xCC111317),
        barContent = Color(0xFFECEFF4),
        cardFront = Color(0xFFF6F6F2),
        cardBorder = Color(0xFFBDB9B0),
        cardRed = Color(0xFFD32F2F),
        cardBlack = Color(0xFF181A1F),
        cardBackPrimary = Color(0xFF37474F),
        cardBackAccent = Color(0xFF607D8B),
        selectionGlow = Color(0xFFFFD54F),
        hintGlow = Color(0xFF80CBC4)
    )
}
