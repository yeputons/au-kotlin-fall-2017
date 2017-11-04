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
}