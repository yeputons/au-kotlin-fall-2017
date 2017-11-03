package ru.spbau.mit

import org.junit.Test
import java.io.ByteArrayOutputStream
import java.util.*
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

    @Test
    fun testMaxTest() {
        val height = 10
        val width = 100000
        val queries = 100000
        val rnd = Random()
        val flagLines = (0 until height).joinToString("\n") {
            (0 until width).joinToString(" ") { _ -> Integer.toString(rnd.nextInt(1000000) + 1) }
        }
        val queryLines = (0 until queries).joinToString("\n") {
            val a = rnd.nextInt(width) + 1
            val b = rnd.nextInt(width) + 1
            val l = minOf(a, b)
            val r = maxOf(a, b)
            "$l $r"
        }
        val data = "$height $width $queries\n$flagLines\n$queryLines\n"
        solve(data.byteInputStream(), ByteArrayOutputStream())
    }
}