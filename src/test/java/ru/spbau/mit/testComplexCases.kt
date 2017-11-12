package ru.spbau.mit

import org.antlr.v4.runtime.BufferedTokenStream
import org.antlr.v4.runtime.CharStreams
import org.junit.Test
import ru.spbau.mit.ast.BlockStatement
import ru.spbau.mit.parser.LangLexer
import ru.spbau.mit.parser.LangParser
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ComplexCasesTest {
    private fun parse(s: String): BlockStatement = BlockStatement(LangParser(BufferedTokenStream(LangLexer(CharStreams.fromString(s)))).file().value!!)
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
}