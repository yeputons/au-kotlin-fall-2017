grammar Lang;

@header {
import ru.spbau.mit.ast.*;
}

file                returns [Block value] : block EOF
    { $value = $block.value; } ;
block               returns [Block value] : (stmts+=statement)*
    { $value = new Block($stmts.stream().map(x -> x.value).collect(java.util.stream.Collectors.toList())); } ;
blockWithBraces     returns [Block value] : '{' block '}'
    { $value = $block.value; } ;
statement           returns [Statement value]
    : functionStatement   { $value = $functionStatement.value; }
    | blockWithBraces     { $value = new BlockStatement($blockWithBraces.value); }
    | variableStatement   { $value = $variableStatement.value; }
    | expression          { $value = new ExpressionStatement($expression.value); }
    | whileStatement      { $value = $whileStatement.value; }
    | ifStatement         { $value = $ifStatement.value; }
    | assignmentStatement { $value = $assignmentStatement.value; }
    | returnStatement     { $value = $returnStatement.value; }
    ;
functionStatement   returns [FunctionDefinitionStatement value]  : 'fun' IDENTIFIER '(' parameterNames ')' blockWithBraces
    { $value = new FunctionDefinitionStatement($IDENTIFIER.text, $parameterNames.value, $blockWithBraces.value); } ;
variableStatement   returns [VariableDeclarationStatement value] : 'var' IDENTIFIER ('=' expression)?
    { $value = new VariableDeclarationStatement($IDENTIFIER.text, $expression.value); } ;
parameterNames      returns [List<String> value]                 : (names+=IDENTIFIER (',' names+=IDENTIFIER)*)?
    { $value = $names.stream().map(x -> x.getText()).collect(java.util.stream.Collectors.toList()); } ;
whileStatement      returns [WhileStatement value]               : 'while' '(' expression ')' statement
    { $value = new WhileStatement($expression.value, $statement.value); } ;
ifStatement         returns [IfStatement value]
    locals [Statement false_body_value = new BlockStatement(new Block(java.util.Collections.emptyList()))]
    : 'if' '(' expression ')'
        true_body=statement
      ('else' false_body=statement { $false_body_value = $false_body.value; })?
    { $value = new IfStatement($expression.value, $true_body.value, $false_body_value ); } ;
assignmentStatement returns [VariableAssignmentStatement value]  : IDENTIFIER '=' expression
    { $value = new VariableAssignmentStatement($IDENTIFIER.text, $expression.value); } ;
returnStatement     returns [ReturnStatement value]              : 'return' expression
    { $value = new ReturnStatement($expression.value); } ;

// Left-recursive rule is a special case and are supported in ANTLR 4
// See https://github.com/antlr/antlr4/blob/master/doc/left-recursion.md
// Cases are listed from the highest priority to the lowest priority
expression returns [Expression value]
    : functionCall        { $value = $functionCall.value; }
    | literal             { $value = new ConstExpression($literal.value); }
    | IDENTIFIER          { $value = new VariableExpression($IDENTIFIER.text); }
    | '(' expression ')'  { $value = $expression.value; }
    // Unfortunately, Antlr4 calls '@after' action only after it processes multiple elements,
    // so we cannot extract common action from the following rules.
    | lhs=expression op=('*' | '/' | '%') rhs=expression
        { $value = new BinaryOperationExpression($lhs.value, BinaryOperation.Companion.getByToken().get($op.text), $rhs.value); }
    | lhs=expression op=('+' | '-') rhs=expression
        { $value = new BinaryOperationExpression($lhs.value, BinaryOperation.Companion.getByToken().get($op.text), $rhs.value); }
    | lhs=expression op=('<' | '<=' | '>' | '>=') rhs=expression
        { $value = new BinaryOperationExpression($lhs.value, BinaryOperation.Companion.getByToken().get($op.text), $rhs.value); }
    | lhs=expression op=('==' | '!=') rhs=expression
        { $value = new BinaryOperationExpression($lhs.value, BinaryOperation.Companion.getByToken().get($op.text), $rhs.value); }
    | lhs=expression op='&&' rhs=expression
        { $value = new BinaryOperationExpression($lhs.value, BinaryOperation.Companion.getByToken().get($op.text), $rhs.value); }
    | lhs=expression op='||' rhs=expression
        { $value = new BinaryOperationExpression($lhs.value, BinaryOperation.Companion.getByToken().get($op.text), $rhs.value); }
    ;
functionCall returns [FunctionCallExpression value] : IDENTIFIER '(' arguments ')'
    { $value = new FunctionCallExpression($IDENTIFIER.text, $arguments.value); } ;
arguments    returns [List<Expression> value]       : (args+=expression (',' args+=expression)*)?
    { $value = $args.stream().map(x -> x.value).collect(java.util.stream.Collectors.toList()); } ;

literal returns [int value] : LITERAL { $value = Integer.parseInt($LITERAL.text); } ;

LITERAL : [1-9] [0-9]* | '0' ;
IDENTIFIER : [a-zA-Z_] [a-zA-Z0-9_]* ;
COMMENT : '//' ~[\r\n]* -> skip ;
UNICODE_WS : [\p{White_Space}] -> skip ;
