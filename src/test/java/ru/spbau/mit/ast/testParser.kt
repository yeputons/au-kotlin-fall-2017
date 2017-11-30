package ru.spbau.mit.ast

import org.antlr.v4.runtime.BufferedTokenStream
import org.antlr.v4.runtime.CharStreams
import org.junit.Test
import ru.spbau.mit.parser.LangLexer
import ru.spbau.mit.parser.LangParser
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class TestParser {
    private fun getParser(s: String): LangParser = LangParser(BufferedTokenStream(LangLexer(CharStreams.fromString(s))))

    private fun v(name: String) = VariableExpression(name)
    private fun i(value: Int) = ConstExpression(value)
    private fun BinaryOperation.e(lhs: Expression, rhs: Expression) = BinaryOperationExpression(lhs, this, rhs)

    private fun block(vararg statements: Statement) = Block(listOf(*statements))
    private fun blockStmt(vararg statements: Statement) = BlockStatement(block(*statements))
    private fun callE(name: String, vararg args: Expression) = FunctionCallExpression(name, args.toList())
    private fun callS(name: String, vararg args: Expression) = ExpressionStatement(FunctionCallExpression(name, args.toList()))

    private fun funDef(name: String, parameters: List<String>, vararg body: Statement) = FunctionDefinitionStatement(name, parameters, block(*body))

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
                VariableDeclarationStatement("a", i(10)),
                VariableDeclarationStatement("b", i(20)),
                IfStatement(
                        BinaryOperationExpression(v("a"), BinaryOperation.GT, v("b")),
                        blockStmt(callS("println", i(1))),
                        blockStmt(callS("println", i(0)))
                )
        )
        assertEquals(expected, parser.file().ast())
        assertEquals(0, parser.numberOfSyntaxErrors)
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
                VariableDeclarationStatement("i", i(1)),
                WhileStatement(
                        BinaryOperationExpression(v("i"), BinaryOperation.LE, i(5)),
                        blockStmt(
                                callS("println", v("i"), callE("fib", v("i"))),
                                VariableAssignmentStatement("i", BinaryOperationExpression(v("i"), BinaryOperation.ADD, i(1)))
                        )
                )
        )
        assertEquals(expected, parser.file().ast())
        assertEquals(0, parser.numberOfSyntaxErrors)
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
        assertEquals(expected, parser.file().ast())
        assertEquals(0, parser.numberOfSyntaxErrors)
    }

    @Test
    fun testVariableDeclarationWithDefault() {
        val parser = getParser("var x = 5")
        val expected = block(VariableDeclarationStatement("x", i(5)))
        assertEquals(expected, parser.file().ast())
        assertEquals(0, parser.numberOfSyntaxErrors)
    }

    @Test
    fun testVariableDeclarationNoDefault() {
        val parser = getParser("var x")
        val expected = block(VariableDeclarationStatement("x", null))
        assertEquals(expected, parser.file().ast())
        assertEquals(0, parser.numberOfSyntaxErrors)
    }

    @Test
    fun testAssociativity() {
        val parser = getParser("1 + 2 - 3 + 4")
        val expected =
                BinaryOperationExpression(
                        BinaryOperationExpression(
                                BinaryOperationExpression(
                                        i(1),
                                        BinaryOperation.ADD,
                                        i(2)
                                ),
                                BinaryOperation.SUB,
                                i(3)
                        ),
                        BinaryOperation.ADD,
                        i(4)
                )
        assertEquals(expected, parser.expression().ast())
        assertEquals(0, parser.numberOfSyntaxErrors)
    }

    @Test
    fun testPrecedenceOrAndEqLe() {
        val parser = getParser("1 < 2 == 3 > 4 && 5 <= 6 != 7 >= 8 || 10 > 20 != 30 >= 40 && 50 < 60 == 70 <= 80")
        val expected =
                BinaryOperation.OR.e(
                        BinaryOperation.AND.e(
                                BinaryOperation.EQ.e(BinaryOperation.LT.e(i(1), i(2)), BinaryOperation.GT.e(i(3), i(4))),
                                BinaryOperation.NEQ.e(BinaryOperation.LE.e(i(5), i(6)), BinaryOperation.GE.e(i(7), i(8)))
                        ),
                        BinaryOperation.AND.e(
                                BinaryOperation.NEQ.e(BinaryOperation.GT.e(i(10), i(20)), BinaryOperation.GE.e(i(30), i(40))),
                                BinaryOperation.EQ.e(BinaryOperation.LT.e(i(50), i(60)), BinaryOperation.LE.e(i(70), i(80)))
                        )
                )
        assertEquals(expected, parser.expression().ast())
        assertEquals(0, parser.numberOfSyntaxErrors)
    }

    @Test
    fun testPrecedenceLeMulSum() {
        val parser = getParser("1 * 2 - 3 / 4 + 5 % 6 <= 7 % 8 + 9 * 10 - 11 / 12")
        val expected =
                BinaryOperation.LE.e(
                        BinaryOperation.ADD.e(
                                BinaryOperation.SUB.e(
                                        BinaryOperation.MUL.e(i(1), i(2)),
                                        BinaryOperation.DIV.e(i(3), i(4))
                                ),
                                BinaryOperation.MOD.e(i(5), i(6))
                        ),
                        BinaryOperation.SUB.e(
                                BinaryOperation.ADD.e(
                                        BinaryOperation.MOD.e(i(7), i(8)),
                                        BinaryOperation.MUL.e(i(9), i(10))
                                ),
                                BinaryOperation.DIV.e(i(11), i(12))
                        )
                )
        assertEquals(expected, parser.expression().ast())
        assertEquals(0, parser.numberOfSyntaxErrors)
    }

    @Test
    fun testPrecedenceMixed() {
        val parser = getParser("1 + 2 * 3 == 4 || 5 == 6 && 7 > 8")
        val expected =
                BinaryOperation.OR.e(
                        BinaryOperation.EQ.e(
                                BinaryOperation.ADD.e(
                                        i(1), BinaryOperation.MUL.e(i(2), i(3))
                                ),
                                i(4)
                        ),
                        BinaryOperation.AND.e(
                                BinaryOperation.EQ.e(i(5), i(6)),
                                BinaryOperation.GT.e(i(7), i(8))
                        )
                )
        assertEquals(expected, parser.expression().ast())
        assertEquals(0, parser.numberOfSyntaxErrors)
    }

    @Test
    fun testExprBrackets() {
        val parser = getParser("1 * (2 + 3) - (4 + 5) * 6")
        val expected =
                BinaryOperation.SUB.e(
                        BinaryOperation.MUL.e(i(1), BinaryOperation.ADD.e(i(2), i(3))),
                        BinaryOperation.MUL.e(BinaryOperation.ADD.e(i(4), i(5)), i(6))
                )
        assertEquals(expected, parser.expression().ast())
        assertEquals(0, parser.numberOfSyntaxErrors)
    }

    @Test
    fun testSyntaxError() {
        val parser = getParser("(1 + )")
        assertEquals(0, parser.numberOfSyntaxErrors)
        parser.expression()
        assertNotEquals(0, parser.numberOfSyntaxErrors)
    }
}