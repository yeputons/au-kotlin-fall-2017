package ru.spbau.mit

import org.junit.Assert.*
import org.junit.Test
import ru.spbau.mit.ast.*
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ScopeTest {
    @Test
    fun testScopedMap() {
        val level1 = ScopedMap(mutableMapOf("first" to 1, "second" to 2))
        assertEquals(1, level1["first"])
        assertEquals(2, level1["second"])
        level1.addOrShadow("second", 20)
        assertEquals(20, level1["second"])

        val level2 = ScopedMap(level1)
        assertEquals(1, level2["first"])
        assertEquals(20, level2["second"])

        level2.addOrShadow("first", 100)
        assertEquals(100, level2["first"])
        assertEquals(1, level1["first"])

        level2.addOrShadow("second", 200)
        assertEquals(200, level2["second"])
        assertEquals(20, level1["second"])
    }

    @Test
    fun testScope() {
        val func1: InterpreterFunction = fun(args): InterpreterValue? { return 10 }
        val one = MutableInterpreterValue(10)
        val parent = Scope(
                functions = ScopedMap(mutableMapOf("func1" to func1)),
                variables = ScopedMap(mutableMapOf("one" to one))
        )
        assertSame(func1, parent.functions["func1"])
        assertSame(one, parent.variables["one"])

        val child = Scope(parent)
        assertNotSame(parent.functions, child.functions)
        assertNotSame(parent.variables, child.variables)
        assertSame(func1, child.functions["func1"])
        assertSame(one, child.variables["one"])
    }
}

class BinaryOperationTest {
    @Test
    fun testAdd() {
        assertEquals(10, BinaryOperation.ADD(lazyOf(3), lazyOf(7)).value)
    }

    @Test
    fun testSub() {
        assertEquals(-4, BinaryOperation.SUB(lazyOf(3), lazyOf(7)).value)
    }

    @Test
    fun testAnd() {
        assertEquals(4, BinaryOperation.AND(lazyOf(3), lazyOf(4)).value)
        assertEquals(0, BinaryOperation.AND(lazyOf(3), lazyOf(0)).value)
        assertEquals(0, BinaryOperation.AND(lazyOf(0), lazyOf(4)).value)
        assertEquals(0, BinaryOperation.AND(lazyOf(0), lazyOf(0)).value)
    }

    @Test
    fun testOr() {
        assertEquals(3, BinaryOperation.OR(lazyOf(3), lazyOf(4)).value)
        assertEquals(3, BinaryOperation.OR(lazyOf(3), lazyOf(0)).value)
        assertEquals(4, BinaryOperation.OR(lazyOf(0), lazyOf(4)).value)
        assertEquals(0, BinaryOperation.OR(lazyOf(0), lazyOf(0)).value)
    }

    @Test
    fun testLazyAnd() {
        val rhsFail = lazy { fail(); 10 }
        assertSame(rhsFail, BinaryOperation.AND(lazyOf(3), rhsFail))

        val lhs0 = lazyOf(0)
        assertSame(lhs0, BinaryOperation.AND(lhs0, rhsFail))
    }

    @Test
    fun testLazyOr() {
        val rhsFail = lazy { fail(); 0 }
        val lhs3 = lazyOf(3)
        assertSame(lhs3, BinaryOperation.OR(lhs3, rhsFail))

        assertSame(rhsFail, BinaryOperation.OR(lazyOf(0), rhsFail))
    }
}

class InterpretTest {
    private val printed = mutableListOf<List<InterpreterValue>>()
    private val context = BaseInterpretationContext(createStdlibScope(println = { printed.add(it); }))

    @Test
    fun testPrintlnStub() {
        assertEquals(emptyList<List<InterpreterValue>>(), printed)

        context.run(FunctionCallExpression("println", listOf(ConstExpression(10), ConstExpression(20))))
        assertEquals(listOf(listOf(10, 20)), printed)

        context.run(FunctionCallExpression("println", listOf()))
        assertEquals(listOf(listOf(10, 20), listOf()), printed)
    }

    @Test
    fun testConstExpressionEvaluation() {
        assertEquals(239, context.run(ConstExpression(239)))
    }

    @Test
    fun testBinaryOperationEvaluation() {
        assertEquals(239, context.run(
                BinaryOperationExpression(
                        BinaryOperationExpression(
                                ConstExpression(10),
                                BinaryOperation.MUL,
                                ConstExpression(20)
                        ),
                        BinaryOperation.ADD,
                        BinaryOperationExpression(
                                ConstExpression(40),
                                BinaryOperation.SUB,
                                ConstExpression(1)
                        )
                )
        ))
    }

    @Test
    fun testVariableDeclarationAndEvaluation() {
        assertNull(context.run(VariableDeclarationStatement("foo", ConstExpression(123))))
        assertEquals(123, context.run(VariableExpression("foo")))
    }

    @Test
    fun testVariableMissing() {
        assertFailsWith(InterpreterException::class) {
            context.run(VariableExpression("foo"))
        }
    }

    @Test
    fun testVariableDeclarationNoInit() {
        assertNull(context.run(VariableDeclarationStatement("foo", null)))
        assertEquals(0, context.run(VariableExpression("foo")))
    }

    @Test
    fun testVariableAssignment() {
        assertNull(context.run(VariableDeclarationStatement("foo", null)))
        assertNull(context.run(VariableAssignmentStatement("foo", ConstExpression(10))))
        assertEquals(10, context.run(VariableExpression("foo")))
    }

    @Test
    fun testVariableAssignmentNoVariable() {
        assertFailsWith(InterpreterException::class) {
            context.run(VariableAssignmentStatement("foo", ConstExpression(10)))
        }
    }

    @Test
    fun testFunctionCallEvaluatesArguments() {
        context.run(FunctionCallExpression("println", listOf(
                BinaryOperationExpression(ConstExpression(10), BinaryOperation.DIV, ConstExpression(3)),
                BinaryOperationExpression(ConstExpression(10), BinaryOperation.MOD, ConstExpression(3))
        )))
        assertEquals(listOf(listOf(3, 1)), printed)
    }

    @Test
    fun testExpressionStatement() {
        context.run(ExpressionStatement(FunctionCallExpression("println", listOf(ConstExpression(20)))))
        assertEquals(listOf(listOf(20)), printed)
    }

    @Test
    fun testBlockStatement() {
        context.run(BlockStatement(Block(listOf(
                ExpressionStatement(FunctionCallExpression("println", listOf(ConstExpression(20)))),
                ExpressionStatement(FunctionCallExpression("println", listOf(ConstExpression(30))))
        ))))
        assertEquals(listOf(listOf(20), listOf(30)), printed)
    }

    @Test
    fun testFunctionDefinitionAndCall() {
        context.run(FunctionDefinitionStatement("func", listOf(), Block(listOf(
                ExpressionStatement(FunctionCallExpression("println", listOf(ConstExpression(20)))),
                ExpressionStatement(FunctionCallExpression("println", listOf(ConstExpression(30))))
        ))))
        assertEquals(emptyList<List<InterpreterValue>>(), printed)
        context.run(FunctionCallExpression("func", emptyList()))
        assertEquals(listOf(listOf(20), listOf(30)), printed)
    }

    @Test
    fun testFunctionDefinitionWithArguments() {
        context.run(FunctionDefinitionStatement("func", listOf("x", "y"), Block(listOf(
                ExpressionStatement(FunctionCallExpression("println", listOf(VariableExpression("x"), VariableExpression("y")))),
                ExpressionStatement(FunctionCallExpression("println", listOf(VariableExpression("y"), VariableExpression("x"))))
        ))))
        assertEquals(emptyList<List<InterpreterValue>>(), printed)
        context.run(FunctionCallExpression("func", listOf(ConstExpression(10), ConstExpression(20))))
        assertEquals(listOf(listOf(10, 20), listOf(20, 10)), printed)
    }

    @Test
    fun testFunctionDefinitionWrongNumberOfArguments() {
        context.run(FunctionDefinitionStatement("func", listOf("x", "y"), Block(listOf(
                ExpressionStatement(FunctionCallExpression("println", listOf(VariableExpression("x"), VariableExpression("y")))),
                ExpressionStatement(FunctionCallExpression("println", listOf(VariableExpression("y"), VariableExpression("x"))))
        ))))
        assertEquals(emptyList<List<InterpreterValue>>(), printed)
        assertFailsWith(InterpreterException::class) {
            context.run(FunctionCallExpression("func", listOf(ConstExpression(10))))
        }
        assertFailsWith(InterpreterException::class) {
            context.run(FunctionCallExpression("func", listOf(ConstExpression(10), ConstExpression(20), ConstExpression(30))))
        }
    }

    @Test
    fun testIfStatement() {
        val trueBody = BlockStatement(Block(listOf(
                ExpressionStatement(FunctionCallExpression("println", listOf(ConstExpression(20))))
        )))
        val falseBody = BlockStatement(Block(listOf(
                ExpressionStatement(FunctionCallExpression("println", listOf(ConstExpression(30))))
        )))
        assertNull(context.run(IfStatement(ConstExpression(10), trueBody, falseBody)))
        assertEquals(listOf(listOf(20)), printed)

        assertNull(context.run(IfStatement(ConstExpression(0), trueBody, falseBody)))
        assertEquals(listOf(listOf(20), listOf(30)), printed)
    }

    @Test
    fun testReturn() {
        assertEquals(10, context.run(ReturnStatement(ConstExpression(10))))
    }
}