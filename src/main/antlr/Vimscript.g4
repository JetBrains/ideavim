grammar Vimscript;

script:                 (executable STATEMENT_SEPARATOR)* executable?;

executable:             forLoop | whileLoop | functionDefinition | ifStatement | command | comment | tryStatement;
forLoop:                WS* FOR WS+ variableName WS+ IN WS+ expr WS* STATEMENT_SEPARATOR
                            (WS* blockMember STATEMENT_SEPARATOR)*
                        WS* ENDFOR WS*
;
whileLoop:              WS* WHILE WS+ expr WS* STATEMENT_SEPARATOR
                            (WS* blockMember STATEMENT_SEPARATOR)*
                        WS* ENDWHILE WS*
;
blockMember:            command | continueStatement | breakStatement | forLoop | whileLoop | ifStatement
                    |   returnStatement | throwStatement | functionDefinition | comment | tryStatement;
continueStatement:      CONTINUE WS*;
breakStatement:         BREAK WS*;
returnStatement:        ':'* WS* range? WS* ':'* WS* RETURN WS+ expr WS*;
throwStatement:         THROW WS+ expr WS*;

ifStatement:            ifBlock
                        elifBlock*
                        elseBlock?
                        WS* ENDIF WS*
;
ifBlock:                WS* IF WS+ expr WS* STATEMENT_SEPARATOR
                            (WS* blockMember STATEMENT_SEPARATOR)*
;
elifBlock:              WS* ELSEIF WS+ expr WS* STATEMENT_SEPARATOR
                            (WS* blockMember STATEMENT_SEPARATOR)*
;
elseBlock:              WS* ELSE WS* STATEMENT_SEPARATOR
                            (WS* blockMember STATEMENT_SEPARATOR)*
;
tryStatement:           tryBlock
                        catchBlock*
                        finallyBlock?
                        WS* ENDTRY WS*
;
tryBlock:               WS* TRY WS* STATEMENT_SEPARATOR
                            (WS* blockMember STATEMENT_SEPARATOR)*
;
catchBlock:             WS* CATCH WS+ pattern WS* STATEMENT_SEPARATOR
                            (WS* blockMember STATEMENT_SEPARATOR)*
;
pattern:                '/' patternBody '/'; // todo is '^' something special or what?..
patternBody:            (. | '^')*?;
finallyBlock:           WS* FINALLY WS* STATEMENT_SEPARATOR
                            (WS* blockMember STATEMENT_SEPARATOR)*
;
functionDefinition:     WS* FUNCTION replace WS+ functionRef WS* '(' argumentsDeclaration ')' WS* STATEMENT_SEPARATOR
                            (WS* blockMember STATEMENT_SEPARATOR)*
                        WS* ENDFUNCTION WS*
;
functionRef:            (anyScope ':')? functionName;
argumentsDeclaration:   (variableName WS* (WS*  ',' WS* variableName)*)?;
command:
                        ':'* WS* range WS*                                                                              #GoToLineCommand
                    |   ':'* WS* range? WS* ':'* WS* ECHO (WS+ expr)* WS* comment?                                      #EchoCommand
                    |   ':'* WS* range? WS* ':'* WS* LET WS* expr WS* assignmentOperator WS* expr WS* comment?          #LetCommand
                    |   ':'* WS* range? DELF (replace)? WS+ functionRef comment?                                        #DelfunctionCommand
                    |   ':'* WS* range? WS* ':'* WS* commandName (WS* commandArgument)?                                 #OtherCommand
;
assignmentOperator:     '='|'+='|'-='|'*='|'/='|'%='|'.=';
commandArgument:        (.|'\\|')*?;
commandName:            anyScope
                    |   LOWERCASE_CHAR
                    |   UPPERCASE_CHAR
                    |   IDENTIFIER_LOWERCASE
                    |   IDENTIFIER_ANY_CASE
                    |   IDENTIFIER_LOWERCASE_WITH_DIGITS
                    |   IDENTIFIER_ANY_CASE_WITH_DIGITS
                    |   '<Plug>'
                    |   '>>' | '<<'
                    |   '>' | '<'
                    |   '&' | '~'
;

replace:            |   '!'?;

range:                  WS* (rangeExpression | (rangeExpression? (rangeSeparator rangeExpression)+ rangeSeparator?));
rangeExpression:        rangeMember | rangeOffset | (rangeMember rangeOffset);
rangeSeparator:         ',' | ';';
// maybe mark rule should be '\'' .    ?
rangeMember:            DIGIT | INT | '.' | '%' | '$' | '\\?' | '\\&' | '\\/' | MARK
                    |   search+
                    |   '\'<' | '\'>'
;
search:                 ('?' ('\\?' | ~('?'))*? '?') | ('/' ('\\/' | ~('/'))*? '/');
rangeOffset:            (numberInOffset | plusOneOffset | minusOneOffset)+;
numberInOffset:         ('+'|'-')?(DIGIT|INT);
plusOneOffset:          '+';
minusOneOffset:         '-';
expr:                   WS* '!' WS* expr                                                                                #UnaryExpression
                    |   expr WS* ('*'|'/'|'%') WS* expr                                                                 #BinExpression
                    |   expr WS* ('+'|'-'|'.') WS* expr                                                                 #BinExpression
                    |   expr WS* (
                            '<'|'<?'|'<#'
                            |'<='|'<=?'|'<=#'
                            |'>'|'>?'|'>#'
                            |'>='|'>=?'|'>=#'
                            |'=~'|'=~?'|'=~#'
                            |'!~'|'!~?'|'!~#'
                            |'=='|'==?'|'==#'
                            |'!='|'!=?'|'!=#'
                            |'is'|'is?'|'is#'
                            |'isnot'|'isnot?'|'isnot#'
                        ) WS* expr                                                                                      #BinExpression
                    |   expr WS* '&''&' WS* expr                                                                        #BinExpression
                    |   expr WS* '||' WS* expr                                                                          #BinExpression
                    |   WS* ('+'|'-') WS* expr                                                                          #UnaryExpression
                    |	(DIGIT | INT)                                                                                   #IntExpression
                    |   FLOAT                                                                                           #FloatExpression
                    |   EMPTY_STRING                                                                                    #StringExpression
                    |   STRING_DOUBLE_QUOTED                                                                            #StringExpression
                    |   singleQuotedString                                                                              #StringExpression
                    |   ESCAPED_SINGLE_QUOTE                                                                            #StringExpression
                    |   BLOB                                                                                            #BlobExpression
                    |   variable                                                                                        #VariableExpression
                    |   option                                                                                          #OptionExpression
                    |   envVariable                                                                                     #EnvVariableExpression
                    |   register                                                                                        #RegisterExpression
                    |   list                                                                                            #ListExpression
                    |   dictionary                                                                                      #DictionaryExpression
                    |   literalDictionary                                                                               #LiteralDictionaryExpression
                    |   '(' WS* expr WS* ')'                                                                            #WrappedExpression
                    |   expr WS* '?' WS* expr WS* ':' WS* expr                                                          #TernaryExpression
                    |   expr '[' WS* from? WS* ':' WS* to? WS* ']'                                                      #SublistExpression
                    |   expr '[' expr ']'                                                                               #OneElementSublistExpression
                    |   functionCall                                                                                    #FunctionCallExpression
;
singleQuotedString:     (SINGLE_QUOTE | MARK)  ('\\' | ~(SINGLE_QUOTE | MARK))*? SINGLE_QUOTE;
from:                   expr;
to:                     expr;

anyVariable:            variable
                    |   option
                    |   envVariable
                    |   register
;
variable:               (variableScope':')? variableName;
variableScope:          anyScope;
anyScope:               BUFFER_VARIABLE
                    |   WINDOW_VARIABLE
                    |   TABPAGE_VARIABLE
                    |   GLOBAL_VARIABLE
                    |   LOCAL_VARIABLE
                    |   SCRIPT_VARIABLE
                    |   FUNCTION_VARIABLE
                    |   VIM_VARIABLE
;
variableName:           IDENTIFIER_LOWERCASE
                    |   IDENTIFIER_ANY_CASE
                    |   IDENTIFIER_LOWERCASE_WITH_DIGITS
                    |   IDENTIFIER_ANY_CASE_WITH_DIGITS
                    |   IDENTIFIER_ANY_CASE_WITH_DIGITS_AND_UNDERSCORES
                    |   LOWERCASE_CHAR
                    |   UPPERCASE_CHAR
                    |   keyword
                    |   existingCommands
                    |   anyScope
;
option:                 '&' (variableScope':')? variableName;
envVariable:            '$' variableName;
register:               '@' (DIGIT | LOWERCASE_CHAR | UPPERCASE_CHAR | anyScope | '-' | ':' | '.' | '%' | '#'
                    |   '=' | '*' | '+' | '~' | '_' | '/')
;
functionCall:           functionRef WS* '(' WS* functionArguments WS* ')';
functionName:           IDENTIFIER_LOWERCASE
                    |   IDENTIFIER_ANY_CASE
                    |   IDENTIFIER_LOWERCASE_WITH_DIGITS
                    |   IDENTIFIER_ANY_CASE_WITH_DIGITS
                    |   LOWERCASE_CHAR
                    |   UPPERCASE_CHAR
                    |   keyword
                    |   anyScope
;
functionArguments:      (expr WS* (',' WS* expr WS*)*)?;
list:                   '[' WS* (expr WS* (',' WS* expr WS*)*)? ']';
dictionary:             '{' (WS* dictionaryEntry WS* (',' WS* dictionaryEntry WS*)*)? '}';
dictionaryEntry:        expr WS* ':' WS* expr;
literalDictionary:      '#{' (WS* literalDictionaryEntry WS* (',' WS* literalDictionaryEntry WS*)*)? '}';
literalDictionaryEntry: literalDictionaryKey WS* ':' WS* expr;
literalDictionaryKey:   IDENTIFIER_LOWERCASE
                    |   IDENTIFIER_ANY_CASE
                    |   IDENTIFIER_LOWERCASE_WITH_DIGITS
                    |   IDENTIFIER_ANY_CASE_WITH_DIGITS
                    |   IDENTIFIER_ANY_CASE_WITH_DIGITS_AND_UNDERSCORES
                    |   LOWERCASE_CHAR
                    |   UPPERCASE_CHAR
                    |   DIGIT
                    |   INT
                    |   keyword
                    |   anyScope
                    |   literalDictionaryKey '-'+ literalDictionaryKey
                    |   '-'+ literalDictionaryKey
                    |   literalDictionaryKey '-'+
;
comment:                WS* ('"' | '""') ~STATEMENT_SEPARATOR *?;
keyword:                FUNCTION
                    |   ENDFUNCTION
                    |   FOR
                    |   ENDFOR
                    |   IN
                    |   BREAK
                    |   CONTINUE
                    |   WHILE
                    |   ENDWHILE
                    |   IF
                    |   ELSE
                    |   ELSEIF
                    |   ENDIF
                    |   THROW
                    |   TRY
                    |   CATCH
                    |   FINALLY
                    |   ENDTRY
                    |   existingCommands
;
existingCommands:       RETURN
                    |   LET
                    |   ECHO
;

// Keywords
FUNCTION:               'function';
ENDFUNCTION:            'endfunction';
FOR:                    'for';
ENDFOR:                 'endfo' | 'endfor';
IN:                     'in';
BREAK:                  'break' | 'brea';
CONTINUE:               'continue' | 'con';
WHILE:                  'while' | 'wh';
ENDWHILE:               'endwhile' | 'endw';
IF:                     'if';
ELSE:                   'else' | 'el';
ELSEIF:                 'elseif' | 'elsei';
ENDIF:                  'endif' | 'en';
TRY:                    'try';
CATCH:                  'catch';
FINALLY:                'finally';
ENDTRY:                 'endtry';
THROW:                  'throw';

// Commands
RETURN:                 'return';
LET:                    'let';
ECHO:                   'ec' | 'echo';
DELF:                   'delf' | 'delfunction';

// Types
DIGIT:                  [0-9];
INT:                    [0-9]+
                    |   '0'[xX][0-9a-fA-F]+;
FLOAT:                  [0-9]+'.'[0-9]+
                    |   [0-9]+'.'[0-9]+'e'[+-]?[0-9]+
;
EMPTY_STRING:           '""';
STRING_DOUBLE_QUOTED:   ('"' ('\\"' | ~[\n])*? '"');
//                    |   '\'' ('\'\'' | ~['])* '\''
//;
ESCAPED_SINGLE_QUOTE:   '\'\'';
MARK:                   '\'' LOWERCASE_CHAR | '\'' UPPERCASE_CHAR;
SINGLE_QUOTE:           '\'';

BLOB:                   '0'[zZ][0-9a-fA-F]+;

// SCOPES
BUFFER_VARIABLE:        'b';
WINDOW_VARIABLE:        'w';
TABPAGE_VARIABLE:       't';
GLOBAL_VARIABLE:        'g';
LOCAL_VARIABLE:         'l';
SCRIPT_VARIABLE:        's';
FUNCTION_VARIABLE:      'a';
VIM_VARIABLE:           'v';

// Identifiers
LOWERCASE_CHAR:         [a-z];
UPPERCASE_CHAR:         [A-Z];
IDENTIFIER_LOWERCASE:   [a-z]+;
IDENTIFIER_ANY_CASE:    [a-zA-Z]+;
IDENTIFIER_LOWERCASE_WITH_DIGITS:   [a-z][a-z0-9]+;
IDENTIFIER_ANY_CASE_WITH_DIGITS:    [a-zA-Z][a-zA-Z0-9]+;
IDENTIFIER_ANY_CASE_WITH_DIGITS_AND_UNDERSCORES: [a-zA-Z_][a-zA-Z0-9_]+;

// Special characters
CANCEL:                 '\u0018';
BACKSPACE:              '\u0008';
ESCAPED_BAR:            '\\|' | '\u0016|';

// Separators
STATEMENT_SEPARATOR:    [|\r\n]+;
WS:                     [ \t]+;
