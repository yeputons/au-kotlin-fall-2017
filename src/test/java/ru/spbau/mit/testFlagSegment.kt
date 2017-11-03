package ru.spbau.mit

import org.junit.Test
import kotlin.test.assertEquals

fun assertArrayEquals(expected: IntArray, actual: IntArray) {
    assertEquals(expected.toList(), actual.toList())
}

class FlagSegmentTest {
    @Test
    fun testRepaintComponents() {
        val input = intArrayOf(3, 2, 2, 1, 2, 10)
        val expected = intArrayOf(0, 1, 1, 2, 1, 3)
        assertArrayEquals(expected, canonizeComponentsList(input))
    }

    @Test
    fun testConstructor() {
        val inputLeft = intArrayOf(3, 2, 2)
        val inputRight = intArrayOf(1, 2, 10)
        val colorsLeft = intArrayOf(10, 20, 30)
        val colorsRight = intArrayOf(40, 50, 60)
        val expectedLeft = intArrayOf(0, 1, 1)
        val expectedRight = intArrayOf(2, 1, 3)
        val stripe = FlagSegment(inputLeft, inputRight, colorsLeft, colorsRight, 4)
        assertEquals(3, stripe.height)
        assertArrayEquals(colorsLeft, stripe.leftColors)
        assertArrayEquals(colorsRight, stripe.rightColors)
        assertEquals(4, stripe.totalComponents)
        assertArrayEquals(expectedLeft, stripe.leftComponents)
        assertArrayEquals(expectedRight, stripe.rightComponents)
    }

    @Test
    fun testStripFromFlagStripe() {
        val flagStripe = intArrayOf(1, 1, 2, 1, 1, 4, 4, 1)
        val components = intArrayOf(0, 0, 1, 2, 2, 3, 3, 4)
        val stripe = FlagSegment.fromSingleColumn(flagStripe)
        assertEquals(8, stripe.height)
        assertArrayEquals(flagStripe, stripe.leftColors)
        assertArrayEquals(flagStripe, stripe.rightColors)
        assertEquals(5, stripe.totalComponents)
        assertArrayEquals(components, stripe.leftComponents)
        assertArrayEquals(components, stripe.rightComponents)
    }

    @Test
    fun testConcatRowStriped() {
        /*
         * 1     1  +  4     4   10     10
         * 2 ... 2  +  5 ... 5 = 11 ... 11
         * 3     3  +  6     6   12 ... 12
         * comps=3     comps=3    comps=3
         */
        val left = FlagSegment(intArrayOf(1, 2, 3), intArrayOf(1, 2, 3), intArrayOf(10, 20, 30), intArrayOf(40, 50, 60), 3)
        val right = FlagSegment(intArrayOf(4, 5, 6), intArrayOf(4, 5, 6), intArrayOf(40, 50, 60), intArrayOf(140, 150, 160), 3)
        val expected = FlagSegment(intArrayOf(10, 11, 12), intArrayOf(10, 11, 12), intArrayOf(10, 20, 30), intArrayOf(140, 150, 160), 3)
        assertEquals(expected, FlagSegmentConcatenator().reduce(left, right))
    }

    @Test
    fun testConcatRowComplex() {
        /*
         * 1     2  +  5     5   10     11
         * 2 ... 4  +  5 ... 7 = 11 ... 13
         * 3     4     6     6   12 ... 14
         * comps=5     comps=7    comps=10
         *
         * Joined components: 2, 4, 5
         */
        val left = FlagSegment(intArrayOf(1, 2, 3), intArrayOf(2, 4, 4), intArrayOf(10, 20, 30), intArrayOf(40, 50, 60), 5)
        val right = FlagSegment(intArrayOf(5, 5, 6), intArrayOf(5, 7, 6), intArrayOf(40, 50, 130), intArrayOf(140, 150, 160), 7)
        val expected = FlagSegment(intArrayOf(10, 11, 12), intArrayOf(11, 13, 14), intArrayOf(10, 20, 30), intArrayOf(140, 150, 160), 10)
        assertEquals(expected, FlagSegmentConcatenator().reduce(left, right))
    }

    @Test
    fun testFullFlag() {
        val columns = arrayOf(
                intArrayOf(1, 1, 2), // left column
                intArrayOf(2, 1, 4),
                intArrayOf(2, 1, 4),
                intArrayOf(1, 1, 3)  // right column
        ).map({ FlagSegment.fromSingleColumn(it) })
        val stripe = columns.reduce(FlagSegmentConcatenator()::reduce)
        assertEquals(3, stripe.height)
        assertArrayEquals(intArrayOf(1, 1, 2), stripe.leftColors)
        assertArrayEquals(intArrayOf(1, 1, 3), stripe.rightColors)
        assertEquals(5, stripe.totalComponents)
        assertArrayEquals(intArrayOf(0, 0, 1), stripe.leftComponents)
        assertArrayEquals(intArrayOf(0, 0, 2), stripe.rightComponents)
    }
}