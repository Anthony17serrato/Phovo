package com.serratocreations.phovo

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform