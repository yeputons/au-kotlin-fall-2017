package ru.spbau.mit

import org.antlr.v4.runtime.BufferedTokenStream
import org.antlr.v4.runtime.CharStreams
import org.junit.Test
import ru.spbau.mit.ast.BlockStatement
import ru.spbau.mit.ast.ast
import ru.spbau.mit.parser.LangLexer
import ru.spbau.mit.parser.LangParser
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ComplexCasesTest {
    private fun parse(s: String): BlockStatement = BlockStatement(LangParser(BufferedTokenStream(LangLexer(CharStreams.fromString(s)))).file().ast())
    private val printed = mutableListOf<List<InterpreterValue>>()
    private val context = BaseInterpretationContext(createStdlibScope(println = { printed.add(it); }))

    @Test
    fun testExample1() {
        val code = parse("""
            |var a = 10
            |var b = 20
            |if (a > b) {
            |    println(1)
            |} else {
            |    println(0)
            |}""".trimMargin())
        context.run(code)
        assertEquals(listOf(listOf(0)), printed)
    }

    @Test
    fun testExample2() {
        val code = parse("""
            |fun fib(n) {
            |    if (n <= 1) {
            |        return 1
            |    }
            |    return fib(n - 1) + fib(n - 2)
            |}
            |
            |var i = 1
            |while (i <= 5) {
            |    println(i, fib(i))
            |    i = i + 1}""".trimMargin())
        context.run(code)
        assertEquals(listOf(
                listOf(1, 1),
                listOf(2, 2),
                listOf(3, 3),
                listOf(4, 5),
                listOf(5, 8)
        ), printed)
    }

    @Test
    fun testExample3() {
        val code = parse("""
            |fun foo(n) {
            |    fun bar(m) {
            |        return m + n
            |    }
            |
            |    return bar(1)
            |}
            |
            |println(foo(41)) // prints 42""".trimMargin())
        context.run(code)
        assertEquals(listOf(listOf(42)), printed)
    }

    @Test
    fun testShadowing() {
        // See https://github.com/java-course-au/kotlin-course/issues/37
        val code = parse("""
            |var x = 1
            |if (1) {
            |  var x = 2
            |}
            |println(x)""".trimMargin())
        context.run(code)
        assertEquals(listOf(listOf(1)), printed)
    }

    @Test
    fun testNoDoubleDeclaration() {
        val code = parse("""
            |var x = 10
            |var x = 10""".trimMargin())
        assertFailsWith(InterpreterException::class) {
            context.run(code)
        }
    }

    @Test
    fun testRedeclarePrintln() {
        val code = parse("""
            |fun foo(x) { println(x) }
            |fun println(x) { foo(x) foo(5) foo(x) }
            |println(10)""".trimMargin())
        context.run(code)
        assertEquals(listOf(listOf(10), listOf(5), listOf(10)), printed)
    }

    @Test
    fun testClosureCapturesVariable() {
        // See https://github.com/java-course-au/kotlin-course/issues/38
        val code = parse("""
            |fun foo(n) {
            |    fun bar(m) {
            |        return m + n
            |    }
            |    n = n + 1
            |    return bar(1)
            |}
            |println(foo(41))  // prints 43""".trimMargin())
        context.run(code)
        assertEquals(listOf(listOf(43)), printed)
    }

    @Test
    fun testNoCallerScopeLookup() {
        // See https://github.com/java-course-au/kotlin-course/issues/38
        val code = parse("""
            |fun foo() {
            |    println(n)
            |}
            |var n = 10
            |println(foo())""".trimMargin())
        assertFailsWith(InterpreterException::class) {
            context.run(code)
        }
    }

    @Test
    fun testIfCreatesNewScope() {
        val code = parse("""
            |if (1) var x = 10
            |println(x)""".trimMargin())
        assertFailsWith(InterpreterException::class) {
            context.run(code)
        }
    }

    @Test
    fun testIfElseCreatesNewScope() {
        val code = parse("""
            |if (0) {} else var x = 10
            |println(x)""".trimMargin())
        assertFailsWith(InterpreterException::class) {
            context.run(code)
        }
    }

    @Test
    fun testWhileCreatesNewScope() {
        val code = parse("""
            |var x = 0
            |fun foo() { x = x + 1 return x <= 1 }
            |while (foo()) var y = 0
            |println(y)""".trimMargin())
        assertFailsWith(InterpreterException::class) {
            context.run(code)
        }
    }

    @Test
    fun testLazyAnd() {
        val code = parse("""
            |fun printAndRet(print, ret) { println(print) return ret }
            |println(printAndRet(10, 11) && printAndRet(12, 13))
            |println(printAndRet(20, 21) && printAndRet(22, 0))
            |println(printAndRet(30, 0) && printAndRet(32, 33))
            |println(printAndRet(40, 0) && printAndRet(42, 0))
            """.trimMargin())
        context.run(code)
        assertEquals(listOf(
                listOf(10), listOf(12), listOf(13),
                listOf(20), listOf(22), listOf(0),
                listOf(30), listOf(0),
                listOf(40), listOf(0)
        ), printed)
    }

    @Test
    fun testLazyOr() {
        val code = parse("""
            |fun printAndRet(print, ret) { println(print) return ret }
            |println(printAndRet(10, 11) || printAndRet(12, 13))
            |println(printAndRet(20, 21) || printAndRet(22, 0))
            |println(printAndRet(30, 0) || printAndRet(32, 33))
            |println(printAndRet(40, 0) || printAndRet(42, 0))
            """.trimMargin())
        context.run(code)
        assertEquals(listOf(
                listOf(10), listOf(11),
                listOf(20), listOf(21),
                listOf(30), listOf(32), listOf(33),
                listOf(40), listOf(42), listOf(0)
        ), printed)
    }
}