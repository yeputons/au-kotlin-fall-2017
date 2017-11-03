package ru.spbau.mit

import org.junit.Test
import java.io.ByteArrayOutputStream
import kotlin.test.assertEquals

class MainTest {
    @Test
    fun testExample1() {
        val input = """4 5 4
1 1 1 1 1
1 2 2 3 3
1 1 1 2 5
4 4 5 5 5
1 5
2 5
1 2
4 5"""
        val expected = """6
7
3
4
"""
        val outputStream = ByteArrayOutputStream()
        solve(input.byteInputStream(), outputStream)
        assertEquals(expected, outputStream.toString().replace("\r\n", "\n"))
    }
}