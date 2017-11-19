package ru.spbau.mit.tex

import org.junit.Test
import kotlin.test.assertEquals

class ArgumentsTest {
    @Test
    fun testCurlyArguments() {
        assertEquals("{hello,world}", curlyArguments("hello", "world").toString())
        assertEquals("{hello,,world}", curlyArguments("hello", "", "world").toString())
        assertEquals("{hello}", curlyArguments("hello").toString())
        assertEquals("{}", curlyArguments().toString())
    }

    @Test
    fun testCurlyArgumentsPairs() {
        assertEquals("{foo=bar,hello=world}", curlyArguments("foo" to "bar", "hello" to "world").toString())
        assertEquals("{foo=bar,hello}", curlyArguments("foo" to "bar", plainArgument("hello")).toString())
        assertEquals("{width=\\foo}", curlyArguments("width" to """\foo""").toString())
    }

    @Test
    fun testSquareArguments() {
        assertEquals("[hello,world]", squareArguments("hello", "world").toString())
        assertEquals("[hello,,world]", squareArguments("hello", "", "world").toString())
        assertEquals("[hello]", squareArguments("hello").toString())
        assertEquals("[]", squareArguments().toString())
    }

    @Test
    fun testSquareArgumentsPairs() {
        assertEquals("[foo=bar,hello=world]", squareArguments("foo" to "bar", "hello" to "world").toString())
        assertEquals("[foo=bar,hello]", squareArguments("foo" to "bar", plainArgument("hello")).toString())
        assertEquals("[width=\\foo]", squareArguments("width" to """\foo""").toString())
    }
}

class TestEscaping {
    @Test
    fun testTexTextEscaping() {
        assertEquals("a\\&b", "a&b".texTextEscape())
        assertEquals("a\\%b", "a%b".texTextEscape())
        assertEquals("a\\\$b", "a\$b".texTextEscape())
        assertEquals("a\\#b", "a#b".texTextEscape())
        assertEquals("a\\_b", "a_b".texTextEscape())
        assertEquals("a\\{b", "a{b".texTextEscape())
        assertEquals("a\\}b", "a}b".texTextEscape())
    }

    @Test
    fun testTexTextEscapingCommands() {
        assertEquals("a\\textasciitilde{}b", "a~b".texTextEscape())
        assertEquals("a\\textasciicircum{}b", "a^b".texTextEscape())
        assertEquals("a\\textbackslash{}b", "a\\b".texTextEscape())
    }
}