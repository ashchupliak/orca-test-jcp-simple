package com.jetbrains.jcp.simple

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    println("🚀 JCP Simple Service Starting...")
    println("Environment: ${System.getenv("ENV") ?: "development"}")

    // Simulate some initialization
    delay(500)

    println("✅ Service ready on port 8080")
    println("Health check: http://localhost:8080/health")

    // Keep running
    while (true) {
        delay(10000)
        println("⏰ Service heartbeat - ${System.currentTimeMillis()}")
    }
}

fun greet(name: String): String {
    return "Hello, $name! Welcome to JCP Simple Service."
}
