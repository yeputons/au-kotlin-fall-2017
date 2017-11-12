package ru.spbau.mit

import ru.spbau.mit.ast.*

typealias InterpreterValue = Int
typealias InterpreterFunction = (List<InterpreterValue>) -> InterpreterValue?

data class MutableInterpreterValue(var value: InterpreterValue)

interface InterpretationContext {
    fun run(stmt: Statement): InterpreterValue?
    fun run(expr: Expression): InterpreterValue
}

class InterpreterException(message: String) : Exception(message)

operator fun BinaryOperation.invoke(lhs: Lazy<InterpreterValue>, rhs: Lazy<InterpreterValue>): Lazy<InterpreterValue> =
        when (this) {
            BinaryOperation.OR -> if (lhs.value != 0) lhs else rhs
            BinaryOperation.AND -> if (lhs.value == 0) lhs else rhs
            else -> {
                val lhs = lhs.value
                val rhs = rhs.value
                lazyOf(when (this) {
                    BinaryOperation.OR -> throw RuntimeException("BinaryOperation.OR should've been already processed")
                    BinaryOperation.AND -> throw RuntimeException("BinaryOperation.AND should've been already processed")
                    BinaryOperation.EQ -> if (lhs == rhs) 1 else 0
                    BinaryOperation.NEQ -> if (lhs != rhs) 1 else 0
                    BinaryOperation.LT -> if (lhs < rhs) 1 else 0
                    BinaryOperation.LE -> if (lhs <= rhs) 1 else 0
                    BinaryOperation.GT -> if (lhs > rhs) 1 else 0
                    BinaryOperation.GE -> if (lhs >= rhs) 1 else 0
                    BinaryOperation.MUL -> lhs * rhs
                    BinaryOperation.DIV -> lhs / rhs
                    BinaryOperation.MOD -> lhs % rhs
                    BinaryOperation.ADD -> lhs + rhs
                    BinaryOperation.SUB -> lhs - rhs
                })
            }
        }

class ScopedMap<T>(private val dict: MutableMap<String, T>) {
    constructor(parent: ScopedMap<T>) : this(HashMap(parent.dict))

    operator fun get(name: String): T =
            dict[name] ?: throw InterpreterException("'$name' is not found in current scope")

    fun addOrShadow(name: String, value: T) {
        dict[name] = value
    }
}

class Scope(val functions: ScopedMap<InterpreterFunction>, val variables: ScopedMap<MutableInterpreterValue>) {
    constructor(parent: Scope) : this(ScopedMap(parent.functions), ScopedMap(parent.variables))
}

class BaseInterpretationContext(private val scope: Scope) : InterpretationContext {
    override fun run(expr: Expression): InterpreterValue =
            when (expr) {
                is FunctionCallExpression -> scope.functions[expr.name](expr.arguments.map { run(it) }) ?: 0
                is ConstExpression -> expr.value
                is VariableExpression -> scope.variables[expr.name].value
                is BinaryOperationExpression -> expr.op(lazy { run(expr.lhs) }, lazy { run(expr.rhs) }).value
            }

    private fun run(block: Block): InterpreterValue? =
            block.statements.map { run(it) }.last()

    override fun run(stmt: Statement): InterpreterValue? {
        when (stmt) {
            is FunctionDefinitionStatement -> {
                val closure = Scope(scope)
                val funImpl: InterpreterFunction = fun(args): InterpreterValue? {
                    if (args.size != stmt.parameterNames.size) {
                        throw InterpreterException("Expected ${stmt.parameterNames.size} arguments, found ${args.size} when calling function ${stmt.name}")
                    }
                    val callScope = Scope(closure)
                    for ((name, value) in stmt.parameterNames.zip(args)) {
                        callScope.variables.addOrShadow(name, MutableInterpreterValue(value))
                    }
                    return BaseInterpretationContext(callScope).run(stmt.body)
                }
                scope.functions.addOrShadow(stmt.name, funImpl)
                closure.functions.addOrShadow(stmt.name, funImpl)  // Allow non-mutual recursion
                return null
            }
            is BlockStatement ->
                return BaseInterpretationContext(Scope(scope)).run(stmt.block)
            is VariableDeclarationStatement -> {
                val initValue = if (stmt.initValue != null) run(stmt.initValue) else 0
                scope.variables.addOrShadow(stmt.name, MutableInterpreterValue(initValue))
                return null
            }
            is ExpressionStatement -> {
                run(stmt.expression)
                return null
            }
            is WhileStatement -> {
                while (run(stmt.condition) != 0) {
                    // TODO: always create a new Scope
                    val result = run(stmt.body)
                    if (result != null) {
                        return result
                    }
                }
                return null
            }
            is IfStatement ->
                // TODO: always create a new Scope
                return if (run(stmt.condition) != 0) {
                    run(stmt.trueBody)
                } else {
                    run(stmt.falseBody)
                }
            is VariableAssignmentStatement -> {
                scope.variables[stmt.name].value = run(stmt.value)
                return null
            }
            is ReturnStatement -> return run(stmt.value)
        }
    }
}

fun createStdlibScope(println : (List<InterpreterValue>) -> Unit): Scope = Scope(
        functions = ScopedMap(mutableMapOf(
                "println" to fun (args): InterpreterValue? { println(args); return null }
        )),
        variables = ScopedMap(mutableMapOf())
)