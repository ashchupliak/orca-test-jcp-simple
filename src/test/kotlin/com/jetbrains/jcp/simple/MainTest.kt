package com.jetbrains.jcp.simple

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MainTest {

    @Test
    fun `greet returns proper message`() {
        val result = greet("Developer")
        assertTrue(result.contains("Hello, Developer"))
        assertTrue(result.contains("JCP Simple Service"))
    }

    @Test
    fun `greet handles empty string`() {
        val result = greet("")
        assertEquals("Hello, ! Welcome to JCP Simple Service.", result)
    }
}
