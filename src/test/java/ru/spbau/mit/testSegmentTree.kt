package ru.spbau.mit

import org.junit.Test
import kotlin.test.assertEquals

class SegmentTreeTest {
    private class ConcatPolicy : SemigroupPolicy<String> {
        override fun combine(left: String, right: String): String = left + right
    }

    @Test
    fun testRsqSingleton() {
        val rsq = RangeQuerySolver(listOf("foo"), ConcatPolicy())
        assertEquals(1, rsq.size)
        assertEquals("foo", rsq.getRangeValue(0, 0))
    }

    @Test
    fun testRsqTwo() {
        val rsq = RangeQuerySolver(listOf("foo", "bar"), ConcatPolicy())
        assertEquals(2, rsq.size)
        assertEquals("foo", rsq.getRangeValue(0, 0))
        assertEquals("bar", rsq.getRangeValue(1, 1))
        assertEquals("foobar", rsq.getRangeValue(0, 1))
    }

    @Test
    fun testRsqBig() {
        val rsq = RangeQuerySolver("012345".map(Char::toString), ConcatPolicy())
        assertEquals(6, rsq.size)
        assertEquals("0", rsq.getRangeValue(0, 0))
        assertEquals("4", rsq.getRangeValue(4, 4))
        assertEquals("5", rsq.getRangeValue(5, 5))
        assertEquals("0123", rsq.getRangeValue(0, 3))
        assertEquals("234", rsq.getRangeValue(2, 4))
        assertEquals("345", rsq.getRangeValue(3, 5))
        assertEquals("012345", rsq.getRangeValue(0, 5))
    }
}