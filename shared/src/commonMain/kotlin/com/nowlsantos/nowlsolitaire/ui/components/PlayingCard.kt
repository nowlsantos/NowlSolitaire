package com.nowlsantos.nowlsolitaire.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.TargetBasedAnimation
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import org.jetbrains.compose.resources.painterResource
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nowlsantos.nowlsolitaire.domain.model.Card
import com.nowlsantos.nowlsolitaire.domain.model.CardBackTheme
import com.nowlsantos.nowlsolitaire.domain.model.Rank
import com.nowlsantos.nowlsolitaire.domain.model.Suit
import com.nowlsantos.nowlsolitaire.domain.model.TableTheme
import com.nowlsantos.nowlsolitaire.ui.theme.TableColors
import com.nowlsantos.nowlsolitaire.ui.theme.toColors
import org.jetbrains.compose.resources.imageResource
import kotlin.math.roundToInt

val LocalCardBackTheme = compositionLocalOf { CardBackTheme.CLASSIC }

private class InPlaceFlipHolder(var lastFaceUp: Boolean) {
    var currentAngle: Float = if (lastFaceUp) 180f else 0f
}

val CardBorderColor: Color = Color(0x80000000)
val CardBorderWidth: Dp = 1.dp
val CardCornerRadius: Dp = 5.dp

private val CardShape = RoundedCornerShape(CardCornerRadius)

fun DrawScope.drawCardOutline(
    color: Color = CardBorderColor,
    strokeWidthDp: Dp = CardBorderWidth,
    cornerDp: Dp = CardCornerRadius
) {
    val strokeWidth = strokeWidthDp.toPx()
    val half = strokeWidth * 0.5f
    val corner = cornerDp.toPx()
    drawRoundRect(
        color = color,
        topLeft = Offset(half, half),
        size = Size(
            (size.width - strokeWidth).coerceAtLeast(1f),
            (size.height - strokeWidth).coerceAtLeast(1f)
        ),
        cornerRadius = CornerRadius(corner, corner),
        style = Stroke(width = strokeWidth)
    )
}

@Suppress("UNUSED_PARAMETER")
@Composable
fun PlayingCardView(
    card: Card,
    cardWidth: Dp,
    cardHeight: Dp,
    colors: TableColors,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    isHinted: Boolean = false,
    isElevated: Boolean = false,
    elevation: Dp = 6.dp,
    flipOnAppear: Boolean = false,
    flipDelayMillis: Int = 0,
    externalFlipProgress: State<Float>? = null,
    cardBackTheme: CardBackTheme = LocalCardBackTheme.current,
    textMeasurer: TextMeasurer? = null,
    onClick: (() -> Unit)? = null
) {
    val externalFlip = if (flipOnAppear) externalFlipProgress else null

    val flipHolder = remember(card.id) { InPlaceFlipHolder(card.isFaceUp) }
    var isFlippingInPlace by remember(card.id) { mutableStateOf(false) }

    val flipModifier: Modifier
    val flipAngleProvider: (() -> Pair<Float, Boolean>)?

    if (externalFlip != null) {
        flipModifier = Modifier.graphicsLayer {
            val angle = FastOutSlowInEasing.transform(externalFlip.value) * 180f
            if (angle in 0.5f..179.5f) {
                rotationY = angle
                cameraDistance = 12f * density
            }
        }
        flipAngleProvider = {
            val angle = FastOutSlowInEasing.transform(externalFlip.value) * 180f
            angle to (angle in 0.5f..179.5f)
        }
    } else if (flipOnAppear) {
        val appearAnim = remember(card.id) { Animatable(0f) }
        LaunchedEffect(card.id) {
            appearAnim.animateTo(
                targetValue = 180f,
                animationSpec = tween(durationMillis = 220, delayMillis = flipDelayMillis)
            )
        }
        flipModifier = Modifier.graphicsLayer {
            val angle = appearAnim.value
            if (angle in 0.5f..179.5f) {
                rotationY = angle
                cameraDistance = 12f * density
            }
        }
        flipAngleProvider = {
            val angle = appearAnim.value
            angle to (angle in 0.5f..179.5f)
        }
    } else if (isElevated) {
        flipModifier = Modifier
        flipAngleProvider = null
    } else {
        if (card.isFaceUp != flipHolder.lastFaceUp) {
            flipHolder.lastFaceUp = card.isFaceUp
            isFlippingInPlace = true
        }
        if (isFlippingInPlace) {
            val angleState = remember(card.id) { mutableFloatStateOf(flipHolder.currentAngle) }
            LaunchedEffect(card.isFaceUp) {
                val targetAngle = if (card.isFaceUp) 180f else 0f
                val anim = TargetBasedAnimation(
                    animationSpec = tween(durationMillis = 240),
                    typeConverter = Float.VectorConverter,
                    initialValue = flipHolder.currentAngle,
                    targetValue = targetAngle
                )
                var startNanos = -1L
                while (true) {
                    val finished = withFrameNanos { frameNanos ->
                        if (startNanos < 0L) {
                            startNanos = frameNanos - 16_666_667L
                        }
                        val playNanos = frameNanos - startNanos
                        val value = anim.getValueFromNanos(playNanos)
                        flipHolder.currentAngle = value
                        angleState.floatValue = value
                        anim.isFinishedFromNanos(playNanos)
                    }
                    if (finished) break
                }
                flipHolder.currentAngle = targetAngle
                angleState.floatValue = targetAngle
                isFlippingInPlace = false
            }
            flipModifier = Modifier.graphicsLayer {
                rotationY = angleState.floatValue
                cameraDistance = 12f * density
            }
            flipAngleProvider = {
                angleState.floatValue to true
            }
        } else {
            flipHolder.currentAngle = if (card.isFaceUp) 180f else 0f
            flipModifier = Modifier
            flipAngleProvider = null
        }
    }

    val cardDescription = if (card.isFaceUp) {
        "${card.rank.name.lowercase()} of ${card.suit.name.lowercase()}"
    } else {
        "Face-down card"
    }

    val clickableModifier = if (onClick != null) {
        Modifier.clickable(
            role = Role.Button,
            onClick = onClick
        )
    } else {
        Modifier
    }

    val elevationModifier = when {
        isElevated && elevation > 0.dp -> Modifier.shadow(elevation = elevation, shape = CardShape, clip = false)
        isSelected -> Modifier.shadow(elevation = 3.dp, shape = CardShape, clip = false)
        else -> Modifier
    }

    val borderStroke = when {
        isSelected -> BorderStroke(2.2.dp, colors.selectionGlow)
        isHinted -> BorderStroke(2.2.dp, colors.hintGlow)
        else -> BorderStroke(CardBorderWidth, colors.cardBorder)
    }

    Box(
        modifier = modifier
            .width(cardWidth)
            .height(cardHeight)
            .then(flipModifier)
            .then(elevationModifier)
            .clip(CardShape)
            .border(borderStroke, CardShape)
            .semantics { contentDescription = cardDescription }
            .then(clickableModifier)
    ) {
        if (flipAngleProvider != null) {
            val frontBitmap = imageResource(getCardDrawableRes(card))
            val backBitmap = imageResource(getCardBackDrawableRes(cardBackTheme))
            Canvas(modifier = Modifier.fillMaxSize()) {
                val (angle, parentRotated) = flipAngleProvider()
                val showFront = angle > 90f
                val bitmapToDraw = if (showFront) frontBitmap else backBitmap
                val w = size.width.roundToInt()
                val h = size.height.roundToInt()
                if (w > 0 && h > 0) {
                    if (showFront && parentRotated) {
                        withTransform({
                            scale(scaleX = -1f, scaleY = 1f, pivot = center)
                        }) {
                            drawImage(
                                image = bitmapToDraw,
                                dstSize = IntSize(w, h)
                            )
                        }
                    } else {
                        drawImage(
                            image = bitmapToDraw,
                            dstSize = IntSize(w, h)
                        )
                    }
                }
            }
        } else if (card.isFaceUp) {
            CardFrontContent(card = card, modifier = Modifier.fillMaxSize())
        } else {
            CardBackContent(modifier = Modifier.fillMaxSize(), cardBackTheme = cardBackTheme)
        }
    }
}

@Composable
fun CardFrontContent(
    card: Card,
    modifier: Modifier = Modifier
) {
    Image(
        painter = painterResource(getCardDrawableRes(card)),
        contentDescription = null,
        contentScale = ContentScale.FillBounds,
        modifier = modifier
    )
}

@Composable
fun CardBackContent(
    modifier: Modifier = Modifier,
    cardBackTheme: CardBackTheme = LocalCardBackTheme.current
) {
    Image(
        painter = painterResource(getCardBackDrawableRes(cardBackTheme)),
        contentDescription = null,
        contentScale = ContentScale.FillBounds,
        modifier = modifier
    )
}

@Composable
fun EmptyPileSlot(
    cardWidth: Dp,
    cardHeight: Dp,
    colors: TableColors,
    modifier: Modifier = Modifier,
    label: String? = null,
    labelFontSize: TextUnit? = null,
    showRefreshIcon: Boolean = false,
    isHighlighted: Boolean = false,
    isDropTarget: Boolean = false,
    contentDescriptionText: String? = "Empty pile",
    onClick: (() -> Unit)? = null
) {
    val shape = CardShape
    val strokeColor = when {
        isDropTarget -> colors.selectionGlow
        isHighlighted -> colors.hintGlow
        else -> colors.slotStroke
    }
    val strokeWidth = if (isHighlighted || isDropTarget) 2.2.dp else 1.2.dp

    val semanticsModifier = if (contentDescriptionText != null) {
        Modifier.semantics { contentDescription = contentDescriptionText }
    } else {
        Modifier
    }

    val clickableModifier = if (onClick != null) {
        Modifier.clickable(
            role = Role.Button,
            onClick = onClick
        )
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .width(cardWidth)
            .height(cardHeight)
            .background(colors.slotFill, shape)
            .border(strokeWidth, strokeColor, shape)
            .then(semanticsModifier)
            .then(clickableModifier),
        contentAlignment = Alignment.Center
    ) {
        when {
            showRefreshIcon -> Icon(
                imageVector = Icons.Rounded.Refresh,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.65f),
                modifier = Modifier.size((cardWidth * 0.45f).coerceIn(18.dp, 28.dp))
            )
            label != null -> {
                val effectiveFontSize = labelFontSize ?: run {
                    val scaleFactor = if (label.length > 2) 0.22f else 0.34f
                    val minSize = if (label.length > 2) 11f else 13f
                    (cardWidth.value * scaleFactor).coerceIn(minSize, 22f).sp
                }
                val labelStyle = remember(effectiveFontSize) {
                    TextStyle(
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = effectiveFontSize,
                        fontWeight = FontWeight.Bold
                    )
                }
                BasicText(
                    text = label,
                    style = labelStyle
                )
            }
        }
    }
}