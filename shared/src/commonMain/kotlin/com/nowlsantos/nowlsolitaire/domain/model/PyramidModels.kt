package com.nowlsantos.nowlsolitaire.domain.model

import kotlinx.serialization.Serializable

@Serializable
sealed interface PyramidLocation {
    @Serializable
    data class PyramidSlot(val row: Int, val col: Int) : PyramidLocation

    @Serializable
    data object Waste : PyramidLocation

    @Serializable
    data object Stock : PyramidLocation
}

@Serializable
data class PyramidCard(
    val row: Int,
    val col: Int,
    val card: Card,
    val isRemoved: Boolean = false
) {
    val id: String
        get() = "pyr_${row}_${col}_${card.id}"
}

sealed interface PyramidMatch {
    val cards: List<Card>

    data class KingMatch(
        val location: PyramidLocation,
        val card: Card
    ) : PyramidMatch {
        override val cards: List<Card> = listOf(card)
    }

    data class PairMatch(
        val firstLocation: PyramidLocation,
        val firstCard: Card,
        val secondLocation: PyramidLocation,
        val secondCard: Card
    ) : PyramidMatch {
        override val cards: List<Card> = listOf(firstCard, secondCard)
    }
}

@Serializable
data class PyramidGameState(
    val pyramid: List<PyramidCard> = emptyList(),
    val stock: List<Card> = emptyList(),
    val waste: List<Card> = emptyList(),
    val discardPile: List<Card> = emptyList(),
    val selectedLocation: PyramidLocation? = null,
    val maxRecycles: Int = DEFAULT_MAX_RECYCLES,
    val recyclesUsed: Int = 0,
    val score: Int = 0,
    val moveCount: Int = 0,
    val seed: Long = 0L
) {
    val isWon: Boolean
        get() = pyramid.isNotEmpty() && pyramid.all { it.isRemoved }

    val remainingPyramidCount: Int
        get() = pyramid.count { !it.isRemoved }

    val topWasteCard: Card?
        get() = waste.lastOrNull()

    val topStockCard: Card?
        get() = stock.lastOrNull()

    val canRecycleStock: Boolean
        get() = stock.isEmpty() && waste.isNotEmpty() && (maxRecycles !in 0..recyclesUsed)

    val remainingRecycles: Int
        get() = if (maxRecycles < 0) Int.MAX_VALUE else (maxRecycles - recyclesUsed).coerceAtLeast(0)

    companion object {
        const val PYRAMID_ROW_COUNT = 7
        const val PYRAMID_CARD_COUNT = 28 // 1 + 2 + 3 + 4 + 5 + 6 + 7
        const val STOCK_CARD_COUNT = 24   // 52 - 28
        const val TARGET_SUM = 13
        const val DEFAULT_MAX_RECYCLES = 2 // 3 passes total through deck
    }
}

@Serializable
data class PyramidSettings(
    val tableTheme: TableTheme = TableTheme.CLASSIC_GREEN,
    val cardBackTheme: CardBackTheme = CardBackTheme.CLASSIC,
    val maxRecycles: Int = PyramidGameState.DEFAULT_MAX_RECYCLES,
    val dealMode: DealMode = DealMode.WINNABLE,
    val showTimer: Boolean = false,
    val tapToAutoMove: Boolean = true
)

@Serializable
data class PyramidRunRecord(
    val roundReached: Int,
    val totalScore: Int,
    val maxStreak: Int
)

@Serializable
data class PyramidRunSession(
    val currentRound: Int = 1,
    val lives: Int = INITIAL_LIVES,
    val maxLives: Int = MAX_LIVES,
    val consecutiveWins: Int = 0,
    val totalRunScore: Int = 0,
    val bestRoundRecord: Int = 1,
    val bestStreakRecord: Int = 0,
    val isGameOver: Boolean = false,
    val topRuns: List<PyramidRunRecord> = emptyList()
) {
    val isMaxLives: Boolean
        get() = lives >= maxLives

    val currentDifficultyTier: String
        get() = getDifficultyTier(currentRound)

    fun withRunRecorded(): PyramidRunSession {
        if (currentRound <= 1 && totalRunScore == 0) return this
        val newRecord = PyramidRunRecord(
            roundReached = currentRound,
            totalScore = totalRunScore,
            maxStreak = maxOf(bestStreakRecord, consecutiveWins)
        )
        val alreadyRecorded = topRuns.any {
            it.roundReached == newRecord.roundReached &&
            it.totalScore == newRecord.totalScore &&
            it.maxStreak == newRecord.maxStreak
        }
        if (alreadyRecorded) return this
        val updatedTopRuns = (topRuns + newRecord)
            .sortedWith(compareByDescending<PyramidRunRecord> { it.roundReached }.thenByDescending { it.totalScore })
            .take(5)
        return copy(topRuns = updatedTopRuns)
    }

    companion object {
        const val INITIAL_LIVES = 3
        const val MAX_LIVES = 5
        const val STREAK_MILESTONE_1 = 3 // award +1 life
        const val STREAK_MILESTONE_2 = 6 // award +2 lives

        fun getEffectiveMaxRecycles(currentRound: Int, settingsMaxRecycles: Int): Int {
            return when {
                settingsMaxRecycles != PyramidGameState.DEFAULT_MAX_RECYCLES -> settingsMaxRecycles
                currentRound <= 3 -> 2 // Novice: 3 passes
                currentRound <= 6 -> 1 // Veteran: 2 passes
                else -> 0              // Master: 1 pass
            }
        }

        fun getDifficultyTier(round: Int): String {
            return when {
                round <= 3 -> "Novice (3 passes)"
                round <= 6 -> "Veteran (2 passes)"
                else -> "Master (1 pass)"
            }
        }
    }
}

@Serializable
data class SavedPyramidGameSnapshot(
    val gameState: PyramidGameState,
    val undoStack: List<PyramidGameState> = emptyList(),
    val elapsedSeconds: Int = 0,
    val hasStartedMoving: Boolean = false,
    val recordedAsPlayed: Boolean = false,
    val runSession: PyramidRunSession = PyramidRunSession()
)
