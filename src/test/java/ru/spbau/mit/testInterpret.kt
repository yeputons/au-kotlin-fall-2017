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
        assertEquals(10, BinaryOperation.ADD(3, { 7 }))
    }

    @Test
    fun testSub() {
        assertEquals(-4, BinaryOperation.SUB(3, { 7 }))
    }

    @Test
    fun testAnd() {
        assertEquals(4, BinaryOperation.AND(3, { 4 }))
        assertEquals(0, BinaryOperation.AND(3, { 0 }))
        assertEquals(0, BinaryOperation.AND(0, { 4 }))
        assertEquals(0, BinaryOperation.AND(0, { 0 }))
    }

    @Test
    fun testOr() {
        assertEquals(3, BinaryOperation.OR(3, { 4 }))
        assertEquals(3, BinaryOperation.OR(3, { 0 }))
        assertEquals(4, BinaryOperation.OR(0, { 4 }))
        assertEquals(0, BinaryOperation.OR(0, { 0 }))
    }

    @Test
    fun testLazyAnd() {
        assertEquals(10, BinaryOperation.AND(3, { 10 }))
        assertEquals(0, BinaryOperation.AND(0, { fail(); 10 }))
    }

    @Test
    fun testLazyOr() {
        assertEquals(3, BinaryOperation.OR(3, { fail(); 10 }))
        assertEquals(10, BinaryOperation.OR(0, { 10 }))
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
        assertNull(context.run(ExpressionStatement(FunctionCallExpression("println", listOf(ConstExpression(20))))))
        assertEquals(listOf(listOf(20)), printed)
    }

    @Test
    fun testBlockStatement() {
        assertNull(context.run(BlockStatement(Block(listOf(
                ExpressionStatement(FunctionCallExpression("println", listOf(ConstExpression(20)))),
                ExpressionStatement(FunctionCallExpression("println", listOf(ConstExpression(30))))
        )))))
        assertEquals(listOf(listOf(20), listOf(30)), printed)
    }

    @Test
    fun testFunctionDefinitionAndCall() {
        assertNull(context.run(FunctionDefinitionStatement("func", listOf(), Block(listOf(
                ExpressionStatement(FunctionCallExpression("println", listOf(ConstExpression(20)))),
                ExpressionStatement(FunctionCallExpression("println", listOf(ConstExpression(30))))
        )))))
        assertEquals(emptyList<List<InterpreterValue>>(), printed)
        context.run(FunctionCallExpression("func", emptyList()))
        assertEquals(listOf(listOf(20), listOf(30)), printed)
    }

    @Test
    fun testFunctionDefinitionWithArguments() {
        assertNull(context.run(FunctionDefinitionStatement("func", listOf("x", "y"), Block(listOf(
                ExpressionStatement(FunctionCallExpression("println", listOf(VariableExpression("x"), VariableExpression("y")))),
                ExpressionStatement(FunctionCallExpression("println", listOf(VariableExpression("y"), VariableExpression("x"))))
        )))))
        assertEquals(emptyList<List<InterpreterValue>>(), printed)
        context.run(FunctionCallExpression("func", listOf(ConstExpression(10), ConstExpression(20))))
        assertEquals(listOf(listOf(10, 20), listOf(20, 10)), printed)
    }

    @Test
    fun testFunctionDefinitionWrongNumberOfArguments() {
        assertNull(context.run(FunctionDefinitionStatement("func", listOf("x", "y"), Block(listOf(
                ExpressionStatement(FunctionCallExpression("println", listOf(VariableExpression("x"), VariableExpression("y")))),
                ExpressionStatement(FunctionCallExpression("println", listOf(VariableExpression("y"), VariableExpression("x"))))
        )))))
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
    fun testWhileStatement() {
        assertNull(context.run(VariableDeclarationStatement("i", ConstExpression(10))))
        assertNull(context.run(WhileStatement(
                VariableExpression("i"),
                VariableAssignmentStatement("i", BinaryOperationExpression(
                        VariableExpression("i"),
                        BinaryOperation.SUB,
                        ConstExpression(1)
                ))
        )))
        assertEquals(0, context.run(VariableExpression("i")))
    }

    @Test
    fun testWhileStatementNoIteration() {
        assertNull(context.run(VariableDeclarationStatement("i", ConstExpression(0))))
        assertNull(context.run(WhileStatement(
                VariableExpression("i"),
                BlockStatement(Block(emptyList())))
        ))
        assertEquals(0, context.run(VariableExpression("i")))
    }

    @Test
    fun testReturn() {
        assertEquals(10, context.run(ReturnStatement(ConstExpression(10))))
    }

    @Test
    fun testReturnFromBlock() {
        assertEquals(20, context.run(BlockStatement(Block(listOf(
                ExpressionStatement(FunctionCallExpression("println", listOf(ConstExpression(10)))),
                ReturnStatement(ConstExpression(20)),
                ExpressionStatement(FunctionCallExpression("println", listOf(ConstExpression(30))))
        )))))
        assertEquals(listOf(listOf(10)), printed)
    }

    @Test
    fun testReturnFromIf() {
        val return10 = ReturnStatement(ConstExpression(10))
        val print20 = ExpressionStatement(FunctionCallExpression("println", listOf(ConstExpression(20))))

        assertEquals(10, context.run(IfStatement(ConstExpression(1), trueBody = return10, falseBody = print20)))
        assertEquals(emptyList<List<InterpreterValue>>(), printed)

        assertEquals(null, context.run(IfStatement(ConstExpression(0), trueBody = return10, falseBody = print20)))
        assertEquals(listOf(listOf(20)), printed)

        assertEquals(null, context.run(IfStatement(ConstExpression(1), trueBody = print20, falseBody = return10)))
        assertEquals(listOf(listOf(20), listOf(20)), printed)

        assertEquals(10, context.run(IfStatement(ConstExpression(0), trueBody = print20, falseBody = return10)))
        assertEquals(listOf(listOf(20), listOf(20)), printed)
    }

    @Test
    fun testReturnFromWhile() {
        assertEquals(10, context.run(WhileStatement(
                ConstExpression(1),
                ReturnStatement(ConstExpression(10))
        )))
    }

    @Test
    fun testReturnFromFunction() {
        assertEquals(null, context.run(FunctionDefinitionStatement("add1", listOf("x"), Block(listOf(
                ExpressionStatement(FunctionCallExpression("println", listOf(
                        BinaryOperationExpression(VariableExpression("x"), BinaryOperation.ADD, ConstExpression(1))
                ))),
                ReturnStatement(
                        BinaryOperationExpression(VariableExpression("x"), BinaryOperation.ADD, ConstExpression(2))
                ),
                ExpressionStatement(FunctionCallExpression("println", listOf(
                        BinaryOperationExpression(VariableExpression("x"), BinaryOperation.ADD, ConstExpression(3))
                )))
        )))))
        assertEquals(emptyList<List<InterpreterValue>>(), printed)
        assertEquals(12, context.run(FunctionCallExpression("add1", listOf(ConstExpression(10)))))
        assertEquals(listOf(listOf(11)), printed)
    }

    @Test
    fun testLocalFunctionVariable() {
        assertEquals(null, context.run(VariableDeclarationStatement("x", ConstExpression(1))))
        assertEquals(null, context.run(FunctionDefinitionStatement("foo", listOf("x"), Block(listOf(
                VariableDeclarationStatement("y", BinaryOperationExpression(
                        VariableExpression("x"),
                        BinaryOperation.ADD,
                        ConstExpression(5)
                )),
                VariableAssignmentStatement("y", BinaryOperationExpression(
                        VariableExpression("y"),
                        BinaryOperation.ADD,
                        ConstExpression(5)
                )),
                ReturnStatement(BinaryOperationExpression(
                        VariableExpression("y"),
                        BinaryOperation.ADD,
                        ConstExpression(100)
                ))
        )))))
        assertEquals(1, context.run(VariableExpression("x")))
        assertEquals(114, context.run(FunctionCallExpression("foo", listOf(
                BinaryOperationExpression(
                        VariableExpression("x"),
                        BinaryOperation.MUL,
                        ConstExpression(4)
                )
        ))))
        assertEquals(1, context.run(VariableExpression("x")))
        assertFailsWith (InterpreterException::class) {
            context.run(VariableExpression("y"))
        }
    }
}