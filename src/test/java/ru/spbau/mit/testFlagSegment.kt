package ru.spbau.mit

import org.junit.Test
import kotlin.test.assertEquals

class FlagSegmentTest {
    @Test
    fun testRepaintComponents() {
        val input = listOf(3, 2, 2, 1, 2, 10)
        val expected = listOf(0, 1, 1, 2, 1, 3)
        assertEquals(expected, canonizeComponentsList(input).toList())
    }

    @Test
    fun testConstructor() {
        val inputLeft = listOf(3, 2, 2)
        val inputRight = listOf(1, 2, 10)
        val colorsLeft = listOf(10, 20, 30)
        val colorsRight = listOf(40, 50, 60)
        val expectedLeft = listOf(0, 1, 1)
        val expectedRight = listOf(2, 1, 3)
        val stripe = FlagSegment(inputLeft, inputRight, colorsLeft, colorsRight, 4)
        assertEquals(3, stripe.height)
        assertEquals(colorsLeft, stripe.leftColors)
        assertEquals(colorsRight, stripe.rightColors)
        assertEquals(4, stripe.totalComponents)
        assertEquals(expectedLeft, stripe.leftComponents)
        assertEquals(expectedRight, stripe.rightComponents)
    }

    @Test
    fun testStripFromFlagStripe() {
        val flagStripe = listOf(1, 1, 2, 1, 1, 4, 4, 1)
        val components = listOf(0, 0, 1, 2, 2, 3, 3, 4)
        val stripe = FlagSegment.fromSingleColumn(flagStripe)
        assertEquals(8, stripe.height)
        assertEquals(flagStripe, stripe.leftColors)
        assertEquals(flagStripe, stripe.rightColors)
        assertEquals(5, stripe.totalComponents)
        assertEquals(components, stripe.leftComponents)
        assertEquals(components, stripe.rightComponents)
    }

    @Test
    fun testConcatRowStriped() {
        /*
         * 1     1  +  4     4   10     10
         * 2 ... 2  +  5 ... 5 = 11 ... 11
         * 3     3  +  6     6   12 ... 12
         * comps=3     comps=3    comps=3
         */
        val left = FlagSegment(listOf(1, 2, 3), listOf(1, 2, 3), listOf(10, 20, 30), listOf(40, 50, 60), 3)
        val right = FlagSegment(listOf(4, 5, 6), listOf(4, 5, 6), listOf(40, 50, 60), listOf(140, 150, 160), 3)
        val expected = FlagSegment(listOf(10, 11, 12), listOf(10, 11, 12), listOf(10, 20, 30), listOf(140, 150, 160), 3)
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
        val left = FlagSegment(listOf(1, 2, 3), listOf(2, 4, 4), listOf(10, 20, 30), listOf(40, 50, 60), 5)
        val right = FlagSegment(listOf(5, 5, 6), listOf(5, 7, 6), listOf(40, 50, 130), listOf(140, 150, 160), 7)
        val expected = FlagSegment(listOf(10, 11, 12), listOf(11, 13, 14), listOf(10, 20, 30), listOf(140, 150, 160), 10)
        assertEquals(expected, FlagSegmentConcatenator().reduce(left, right))
    }

    @Test
    fun testFullFlag() {
        val columns = listOf(
                listOf(1, 1, 2), // left column
                listOf(2, 1, 4),
                listOf(2, 1, 4),
                listOf(1, 1, 3)  // right column
        ).map({ FlagSegment.fromSingleColumn(it) })
        val stripe = columns.reduce(FlagSegmentConcatenator()::reduce)
        assertEquals(3, stripe.height)
        assertEquals(listOf(1, 1, 2), stripe.leftColors)
        assertEquals(listOf(1, 1, 3), stripe.rightColors)
        assertEquals(5, stripe.totalComponents)
        assertEquals(listOf(0, 0, 1), stripe.leftComponents)
        assertEquals(listOf(0, 0, 2), stripe.rightComponents)
    }
}