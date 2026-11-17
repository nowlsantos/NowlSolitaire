package com.nowlsantos.nowlsolitaire.ui.components

import com.nowlsantos.nowlsolitaire.domain.model.Card
import com.nowlsantos.nowlsolitaire.domain.model.CardBackTheme
import com.nowlsantos.nowlsolitaire.domain.model.Rank
import com.nowlsantos.nowlsolitaire.domain.model.Suit
import nowlsolitaire.shared.generated.resources.Res
import nowlsolitaire.shared.generated.resources.backface_01
import nowlsolitaire.shared.generated.resources.backface_02
import nowlsolitaire.shared.generated.resources.backface_03
import nowlsolitaire.shared.generated.resources.backface_04
import nowlsolitaire.shared.generated.resources.card_2c
import nowlsolitaire.shared.generated.resources.card_2d
import nowlsolitaire.shared.generated.resources.card_2h
import nowlsolitaire.shared.generated.resources.card_2s
import nowlsolitaire.shared.generated.resources.card_3c
import nowlsolitaire.shared.generated.resources.card_3d
import nowlsolitaire.shared.generated.resources.card_3h
import nowlsolitaire.shared.generated.resources.card_3s
import nowlsolitaire.shared.generated.resources.card_4c
import nowlsolitaire.shared.generated.resources.card_4d
import nowlsolitaire.shared.generated.resources.card_4h
import nowlsolitaire.shared.generated.resources.card_4s
import nowlsolitaire.shared.generated.resources.card_5c
import nowlsolitaire.shared.generated.resources.card_5d
import nowlsolitaire.shared.generated.resources.card_5h
import nowlsolitaire.shared.generated.resources.card_5s
import nowlsolitaire.shared.generated.resources.card_6c
import nowlsolitaire.shared.generated.resources.card_6d
import nowlsolitaire.shared.generated.resources.card_6h
import nowlsolitaire.shared.generated.resources.card_6s
import nowlsolitaire.shared.generated.resources.card_7c
import nowlsolitaire.shared.generated.resources.card_7d
import nowlsolitaire.shared.generated.resources.card_7h
import nowlsolitaire.shared.generated.resources.card_7s
import nowlsolitaire.shared.generated.resources.card_8c
import nowlsolitaire.shared.generated.resources.card_8d
import nowlsolitaire.shared.generated.resources.card_8h
import nowlsolitaire.shared.generated.resources.card_8s
import nowlsolitaire.shared.generated.resources.card_9c
import nowlsolitaire.shared.generated.resources.card_9d
import nowlsolitaire.shared.generated.resources.card_9h
import nowlsolitaire.shared.generated.resources.card_9s
import nowlsolitaire.shared.generated.resources.card_ac
import nowlsolitaire.shared.generated.resources.card_ad
import nowlsolitaire.shared.generated.resources.card_ah
import nowlsolitaire.shared.generated.resources.card_as
import nowlsolitaire.shared.generated.resources.card_back
import nowlsolitaire.shared.generated.resources.card_jc
import nowlsolitaire.shared.generated.resources.card_jd
import nowlsolitaire.shared.generated.resources.card_jh
import nowlsolitaire.shared.generated.resources.card_js
import nowlsolitaire.shared.generated.resources.card_kc
import nowlsolitaire.shared.generated.resources.card_kd
import nowlsolitaire.shared.generated.resources.card_kh
import nowlsolitaire.shared.generated.resources.card_ks
import nowlsolitaire.shared.generated.resources.card_qc
import nowlsolitaire.shared.generated.resources.card_qd
import nowlsolitaire.shared.generated.resources.card_qh
import nowlsolitaire.shared.generated.resources.card_qs
import nowlsolitaire.shared.generated.resources.card_tc
import nowlsolitaire.shared.generated.resources.card_td
import nowlsolitaire.shared.generated.resources.card_th
import nowlsolitaire.shared.generated.resources.card_ts
import org.jetbrains.compose.resources.DrawableResource

fun getCardDrawableRes(suit: Suit, rank: Rank): DrawableResource {
    return when (suit) {
        Suit.CLUBS -> when (rank) {
            Rank.ACE -> Res.drawable.card_ac
            Rank.TWO -> Res.drawable.card_2c
            Rank.THREE -> Res.drawable.card_3c
            Rank.FOUR -> Res.drawable.card_4c
            Rank.FIVE -> Res.drawable.card_5c
            Rank.SIX -> Res.drawable.card_6c
            Rank.SEVEN -> Res.drawable.card_7c
            Rank.EIGHT -> Res.drawable.card_8c
            Rank.NINE -> Res.drawable.card_9c
            Rank.TEN -> Res.drawable.card_tc
            Rank.JACK -> Res.drawable.card_jc
            Rank.QUEEN -> Res.drawable.card_qc
            Rank.KING -> Res.drawable.card_kc
        }
        Suit.DIAMONDS -> when (rank) {
            Rank.ACE -> Res.drawable.card_ad
            Rank.TWO -> Res.drawable.card_2d
            Rank.THREE -> Res.drawable.card_3d
            Rank.FOUR -> Res.drawable.card_4d
            Rank.FIVE -> Res.drawable.card_5d
            Rank.SIX -> Res.drawable.card_6d
            Rank.SEVEN -> Res.drawable.card_7d
            Rank.EIGHT -> Res.drawable.card_8d
            Rank.NINE -> Res.drawable.card_9d
            Rank.TEN -> Res.drawable.card_td
            Rank.JACK -> Res.drawable.card_jd
            Rank.QUEEN -> Res.drawable.card_qd
            Rank.KING -> Res.drawable.card_kd
        }
        Suit.HEARTS -> when (rank) {
            Rank.ACE -> Res.drawable.card_ah
            Rank.TWO -> Res.drawable.card_2h
            Rank.THREE -> Res.drawable.card_3h
            Rank.FOUR -> Res.drawable.card_4h
            Rank.FIVE -> Res.drawable.card_5h
            Rank.SIX -> Res.drawable.card_6h
            Rank.SEVEN -> Res.drawable.card_7h
            Rank.EIGHT -> Res.drawable.card_8h
            Rank.NINE -> Res.drawable.card_9h
            Rank.TEN -> Res.drawable.card_th
            Rank.JACK -> Res.drawable.card_jh
            Rank.QUEEN -> Res.drawable.card_qh
            Rank.KING -> Res.drawable.card_kh
        }
        Suit.SPADES -> when (rank) {
            Rank.ACE -> Res.drawable.card_as
            Rank.TWO -> Res.drawable.card_2s
            Rank.THREE -> Res.drawable.card_3s
            Rank.FOUR -> Res.drawable.card_4s
            Rank.FIVE -> Res.drawable.card_5s
            Rank.SIX -> Res.drawable.card_6s
            Rank.SEVEN -> Res.drawable.card_7s
            Rank.EIGHT -> Res.drawable.card_8s
            Rank.NINE -> Res.drawable.card_9s
            Rank.TEN -> Res.drawable.card_ts
            Rank.JACK -> Res.drawable.card_js
            Rank.QUEEN -> Res.drawable.card_qs
            Rank.KING -> Res.drawable.card_ks
        }
    }
}

fun getCardDrawableRes(card: Card): DrawableResource = getCardDrawableRes(card.suit, card.rank)

fun getCardBackDrawableRes(theme: CardBackTheme = CardBackTheme.CLASSIC): DrawableResource {
    return when (theme) {
        CardBackTheme.CLASSIC -> Res.drawable.card_back
        CardBackTheme.BACK_1 -> Res.drawable.backface_01
        CardBackTheme.BACK_2 -> Res.drawable.backface_02
        CardBackTheme.BACK_3 -> Res.drawable.backface_03
        CardBackTheme.BACK_4 -> Res.drawable.backface_04
    }
}

val CARD_BACK_DRAWABLE_RES: DrawableResource = Res.drawable.card_back
