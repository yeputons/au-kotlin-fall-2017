grammar Lang;

file : block EOF ;
block : statement* ;
blockWithBraces : '{' block '}' ;
statement
    : functionStatement
    | blockWithBraces
    | variableStatement
    | expression
    | whileStatement
    | ifStatement
    | assignmentStatement
    | returnStatement
    ;
functionStatement : 'fun' name=IDENTIFIER '(' parameterNames ')' body=blockWithBraces ;
variableStatement : 'var' name=IDENTIFIER ('=' initValue=expression)? ;
parameterNames : (names+=IDENTIFIER (',' names+=IDENTIFIER)*)? ;
whileStatement : 'while' '(' condition=expression ')' body=statement ;
ifStatement : 'if' '(' condition=expression ')' true_body=statement ('else' false_body=statement)? ;
assignmentStatement : name=IDENTIFIER '=' value=expression ;
returnStatement : 'return' value=expression ;
expression
    : functionCall  # functionCallExpression
    | literal  # numberExpression
    | IDENTIFIER  # variableExpression
    | '(' expression ')'  # nestedExpression
    | expression op='||' expression  # binaryOperation
    | expression op='&&' expression  # binaryOperation
    | expression op=('==' | '!=') expression  # binaryOperation
    | expression op=('<' | '<=' | '>' | '>=') expression  # binaryOperation
    | expression op=('*' | '/' | '%') expression  # binaryOperation
    | expression op=('+' | '-') expression  # binaryOperation
    ;
functionCall : name=IDENTIFIER '(' arguments ')' ;
arguments : (args+=expression (',' args+=expression)*)? ;

literal returns [int value] : LITERAL { $value = Integer.parseInt($LITERAL.text); } ;

LITERAL : [1-9] [0-9]* | '0' ;
IDENTIFIER : [a-zA-Z_] [a-zA-Z0-9_]* ;
COMMENT : '//' ~[\r\n]* -> skip ;
UNICODE_WS : [\p{White_Space}] -> skip ;
