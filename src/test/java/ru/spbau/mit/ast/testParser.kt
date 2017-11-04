package ru.spbau.mit.ast

import org.antlr.v4.runtime.BufferedTokenStream
import org.antlr.v4.runtime.CharStreams
import org.junit.Test
import ru.spbau.mit.parser.LangLexer
import ru.spbau.mit.parser.LangParser
import kotlin.test.assertEquals

class TestParser {
    private fun getParser(s: String): LangParser = LangParser(BufferedTokenStream(LangLexer(CharStreams.fromString(s))))

    private fun v(name: String) = IdentifierExpression(name)
    private fun i(value: Int) = LiteralExpression(value)

    private fun block(vararg statements: Statement) = Block(listOf(*statements))
    private fun blockStmt(vararg statements: Statement) = BlockStatement(block(*statements))
    private fun callE(name: String, vararg args: Expression) = FunctionCallExpression(name, args.toList())
    private fun callS(name: String, vararg args: Expression) = ExpressionStatement(FunctionCallExpression(name, args.toList()))

    private fun funDef(name: String, parameters: List<String>, vararg body: Statement) = FunctionStatement(name, parameters, block(*body))

    @Test
    fun testExample1() {
        val parser = getParser("""
            |var a = 10
            |var b = 20
            |if (a > b) {
            |    println(1)
            |} else {
            |    println(0)
            |}""".trimMargin())
        val expected = block(
                VariableStatement("a", i(10)),
                VariableStatement("b", i(20)),
                IfStatement(
                        BinaryOperationExpression(v("a"), BinaryOperation.GT, v("b")),
                        blockStmt(callS("println", i(1))),
                        blockStmt(callS("println", i(0)))
                )
        )
        assertEquals(expected, parser.file().value!!)
    }

    @Test
    fun testExample2() {
        val parser = getParser("""
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
            |    i = i + 1
            |}""".trimMargin())
        val expected = block(
                funDef("fib", listOf("n"),
                        IfStatement(
                                BinaryOperationExpression(v("n"), BinaryOperation.LE, i(1)),
                                blockStmt(ReturnStatement(i(1))),
                                blockStmt()
                        ),
                        ReturnStatement(BinaryOperationExpression(
                                callE("fib", BinaryOperationExpression(v("n"), BinaryOperation.SUB, i(1))),
                                BinaryOperation.ADD,
                                callE("fib", BinaryOperationExpression(v("n"), BinaryOperation.SUB, i(2))
                                ))
                        )
                ),
                VariableStatement("i", i(1)),
                WhileStatement(
                        BinaryOperationExpression(v("i"), BinaryOperation.LE, i(5)),
                        blockStmt(
                                callS("println", v("i"), callE("fib", v("i"))),
                                AssignmentStatement("i", BinaryOperationExpression(v("i"), BinaryOperation.ADD, i(1)))
                        )
                )
        )
        assertEquals(expected, parser.file().value!!)
    }

    @Test
    fun testExample3() {
        val parser = getParser("""
            |fun foo(n) {
            |    fun bar(m) {
            |        return m + n
            |    }
            |
            |    return bar(1)
            |}
            |
            |println(foo(41)) // prints 42""".trimMargin())
        val expected = block(
                funDef("foo", listOf("n"),
                        funDef("bar", listOf("m"),
                                ReturnStatement(BinaryOperationExpression(v("m"), BinaryOperation.ADD, v("n")))
                        ),
                        ReturnStatement(callE("bar", i(1)))
                ),
                callS("println", callE("foo", i(41)))
        )
        assertEquals(expected, parser.file().value!!)
    }
}