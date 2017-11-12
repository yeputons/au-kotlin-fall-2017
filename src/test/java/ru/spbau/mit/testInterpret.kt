package ru.spbau.mit

import org.junit.Assert.*
import org.junit.Test
import ru.spbau.mit.ast.BinaryOperation
import ru.spbau.mit.ast.ConstExpression
import ru.spbau.mit.ast.FunctionCallExpression
import kotlin.test.assertEquals

class ScopeTest {
    @Test
    fun testScopedMap() {
        val level1 = ScopedMap<Int>(mutableMapOf("first" to 1, "second" to 2))
        assertEquals(1, level1["first"])
        assertEquals(2, level1["second"])
        level1.addOrShadow("second", 20)
        assertEquals(20, level1["second"])

        val level2 = ScopedMap<Int>(level1)
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
    val printed = mutableListOf<List<InterpreterValue>>()
    val context = BaseInterpretationContext(createStdlibScope(println = { printed.add(it); }))

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
}