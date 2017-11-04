package ru.spbau.mit.ast

data class Block(val statements: List<Statement>)

sealed class Statement
data class FunctionStatement(val name: String, val parameterNames: List<String>, val body: Block) : Statement()
data class BlockStatement(val block: Block) : Statement()
data class VariableStatement(val name: String, val initValue: Expression?) : Statement()
data class ExpressionStatement(val expression: Expression) : Statement()
data class WhileStatement(val condition: Expression, val body: Statement) : Statement()
data class IfStatement(val condition: Expression, val trueBody: Statement, val falseBody: Statement) : Statement()
data class AssignmentStatement(val name: String, val value: Expression) : Statement()
data class ReturnStatement(val value: Expression) : Statement()

sealed class Expression
data class FunctionCallExpression(val name: String, val arguments: List<Expression>) : Expression()
data class LiteralExpression(val value: Int) : Expression()
data class IdentifierExpression(val name: String) : Expression()
data class BinaryOperationExpression(val lhs: Expression, val op: BinaryOperation, val rhs: Expression) : Expression()

enum class BinaryOperation(val token: String) {
    OR("||"), AND("&&"),
    EQ("=="), NEQ("!="),
    LT("<"), LE("<="), GT(">"), GE(">="),
    MUL("*"), DIV("/"), MOD("%"),
    ADD("+"), SUB("-");

    companion object {
        val byToken = BinaryOperation.values().associateBy(BinaryOperation::token)
    }
}
