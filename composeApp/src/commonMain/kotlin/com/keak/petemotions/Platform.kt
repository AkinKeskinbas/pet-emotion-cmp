package com.keak.petemotions

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform