package com.nowlsantos.nowlsolitaire.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class CardColor {
    RED,
    BLACK
}

@Serializable
enum class Suit(
    val symbol: String,
    val color: CardColor
) {
    SPADES(symbol = "♠", color = CardColor.BLACK),
    HEARTS(symbol = "♥", color = CardColor.RED),
    CLUBS(symbol = "♣", color = CardColor.BLACK),
    DIAMONDS(symbol = "♦", color = CardColor.RED)
}

@Serializable
enum class Rank(
    val value: Int,
    val symbol: String
) {
    ACE(1, "A"),
    TWO(2, "2"),
    THREE(3, "3"),
    FOUR(4, "4"),
    FIVE(5, "5"),
    SIX(6, "6"),
    SEVEN(7, "7"),
    EIGHT(8, "8"),
    NINE(9, "9"),
    TEN(10, "10"),
    JACK(11, "J"),
    QUEEN(12, "Q"),
    KING(13, "K")
}

@Serializable
data class Card(
    val suit: Suit,
    val rank: Rank,
    val isFaceUp: Boolean = false
) {
    val id: String
        get() = "${suit.name}_${rank.name}"

    val color: CardColor
        get() = suit.color

    fun flipped(faceUp: Boolean = !isFaceUp): Card = copy(isFaceUp = faceUp)
}
