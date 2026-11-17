package com.nowlsantos.nowlsolitaire.domain.model

import kotlin.random.Random

object PyramidWinnableSeeds {
    val SEEDS: LongArray = longArrayOf(
        3L, 4L, 6L, 24L, 82L, 105L, 138L, 163L, 167L, 214L,
        230L, 256L, 263L, 274L, 276L, 282L, 290L, 318L, 382L, 400L,
        403L, 484L, 491L, 516L, 522L, 524L, 530L, 537L, 560L, 571L,
        650L, 680L, 687L, 715L, 767L, 853L, 904L, 910L, 913L, 915L,
        918L, 924L, 952L, 1023L, 1038L, 1058L, 1060L, 1063L, 1088L, 1090L,
        1117L, 1166L, 1209L, 1214L, 1222L, 1243L, 1253L, 1255L, 1256L, 1290L,
        1308L, 1317L, 1322L, 1346L, 1348L, 1356L, 1367L, 1372L, 1448L, 1547L,
        1554L, 1557L, 1573L, 1595L, 1598L
    )

    fun randomSeed(rng: Random = Random): Long = SEEDS[rng.nextInt(SEEDS.size)]
}

object KlondikeWinnableSeeds {
    val SEEDS: LongArray = longArrayOf(
        1L, 2L, 3L, 5L, 7L, 8L, 9L, 10L, 11L, 13L,
        14L, 15L, 17L, 18L, 19L, 21L, 22L, 23L, 24L, 25L,
        27L, 28L, 29L, 31L, 32L, 33L, 34L, 35L, 36L, 37L,
        39L, 40L, 42L, 43L, 44L, 45L, 47L, 48L, 49L, 50L,
        52L, 53L, 54L, 55L, 57L, 58L, 59L, 60L, 62L, 63L,
        64L, 65L, 67L, 68L, 69L, 70L, 72L, 73L, 74L, 75L,
        77L, 78L, 79L, 80L, 82L, 83L, 84L, 85L, 87L, 88L,
        89L, 90L, 92L, 93L, 94L, 95L, 97L, 98L, 99L, 100L
    )

    fun randomSeed(rng: Random = Random): Long = SEEDS[rng.nextInt(SEEDS.size)]
}
