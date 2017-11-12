package ru.spbau.mit

import org.junit.Test
import ru.spbau.mit.ast.ConstExpression
import ru.spbau.mit.ast.FunctionCallExpression
import kotlin.test.assertEquals

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
}