grammar Lang;

file : block EOF ;
block : (stmts+=statement)* ;
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
functionStatement   : 'fun' name=IDENTIFIER '(' parameterNames ')' body=blockWithBraces ;
variableStatement   : 'var' name=IDENTIFIER ('=' expression)? ;
parameterNames      : (names+=IDENTIFIER (',' names+=IDENTIFIER)*)? ;
whileStatement      : 'while' '(' condition=expression ')' body=statement ;
ifStatement
    : 'if' '(' condition=expression ')'
        true_body=statement
      ('else' false_body=statement)? ;
assignmentStatement : name=IDENTIFIER '=' expression ;
returnStatement     : 'return' expression ;

// Left-recursive rule is a special case and are supported in ANTLR 4
// See https://github.com/antlr/antlr4/blob/master/doc/left-recursion.md
// Cases are listed from the highest priority to the lowest priority
expression
    : functionCall
    | literal
    | IDENTIFIER
    | '(' embedded=expression ')'
    | lhs=expression op=('*' | '/' | '%') rhs=expression
    | lhs=expression op=('+' | '-') rhs=expression
    | lhs=expression op=('<' | '<=' | '>' | '>=') rhs=expression
    | lhs=expression op=('==' | '!=') rhs=expression
    | lhs=expression op='&&' rhs=expression
    | lhs=expression op='||' rhs=expression
    ;
functionCall : name=IDENTIFIER '(' arguments ')' ;
arguments    : (args+=expression (',' args+=expression)*)? ;

literal : LITERAL ;

LITERAL : [1-9] [0-9]* | '0' ;
IDENTIFIER : [a-zA-Z_] [a-zA-Z0-9_]* ;
COMMENT : '//' ~[\r\n]* -> skip ;
UNICODE_WS : [\p{White_Space}] -> skip ;
