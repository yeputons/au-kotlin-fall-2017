package ru.spbau.mit.ast

import ru.spbau.mit.parser.LangParser

fun LangParser.FileContext.ast(): Block =
        block().ast()

fun LangParser.BlockContext.ast(): Block =
        Block(stmts.map { it.ast() })

fun LangParser.BlockWithBracesContext.ast(): Block =
        block().ast()

fun LangParser.StatementContext.ast(): Statement {
    if (functionStatement() != null) return functionStatement().ast()
    if (blockWithBraces() != null) return BlockStatement(blockWithBraces().ast())
    if (variableStatement() != null) return variableStatement().ast()
    if (expression() != null) return ExpressionStatement(expression().ast())
    if (whileStatement() != null) return whileStatement().ast()
    if (ifStatement() != null) return ifStatement().ast()
    if (assignmentStatement() != null) return assignmentStatement().ast()
    if (returnStatement() != null) return returnStatement().ast()
    throw RuntimeException("Received unexpected Statement from ANTLR")
}

fun LangParser.FunctionStatementContext.ast(): FunctionDefinitionStatement =
        FunctionDefinitionStatement(name.text, parameterNames().ast(), body.ast())

fun LangParser.VariableStatementContext.ast(): VariableDeclarationStatement =
        VariableDeclarationStatement(name.text, expression()?.ast())

fun LangParser.ParameterNamesContext.ast(): List<String> =
        names.map { it.text }

fun LangParser.WhileStatementContext.ast(): WhileStatement =
        WhileStatement(condition.ast(), body.ast())

fun LangParser.IfStatementContext.ast(): IfStatement =
        IfStatement(condition.ast(), true_body.ast(), false_body?.ast() ?: BlockStatement(Block(emptyList())))

fun LangParser.AssignmentStatementContext.ast(): VariableAssignmentStatement =
        VariableAssignmentStatement(name.text, expression().ast())

fun LangParser.ReturnStatementContext.ast(): ReturnStatement =
        ReturnStatement(expression().ast())

fun LangParser.ExpressionContext.ast(): Expression {
    if (functionCall() != null) return functionCall().ast()
    if (literal() != null) return ConstExpression(literal().ast())
    if (IDENTIFIER() != null) return VariableExpression(IDENTIFIER().text)
    if (embedded != null) return embedded.ast()
    return BinaryOperationExpression(
            lhs.ast(),
            BinaryOperation.byToken[op.text]!!,
            rhs.ast()
    )
}

fun LangParser.FunctionCallContext.ast(): FunctionCallExpression =
        FunctionCallExpression(name.text, arguments().ast())

fun LangParser.ArgumentsContext.ast(): List<Expression> =
        args.map { it.ast() }

fun LangParser.LiteralContext.ast(): Int =
        text.toInt()