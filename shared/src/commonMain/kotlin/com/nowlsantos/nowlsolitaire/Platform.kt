package com.nowlsantos.nowlsolitaire

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform