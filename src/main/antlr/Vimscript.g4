grammar Vimscript;

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//
//  Multiline blocks related rules
//
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
script:
    STATEMENT_SEPARATOR* (executable STATEMENT_SEPARATOR)* executable?;

executable:
    // containers
    forLoop | whileLoop | functionDefinition | ifStatement | tryStatement
    // one-liners
|   (ws_cols command)
|   (ws_cols comment);

forLoop:
    ws_cols FOR WS+ variableName WS+ IN WS+ expr WS* STATEMENT_SEPARATOR
        (ws_cols blockMember STATEMENT_SEPARATOR)*
    ws_cols ENDFOR WS*
;
whileLoop:
    ws_cols WHILE WS+ expr WS* STATEMENT_SEPARATOR
        (ws_cols blockMember STATEMENT_SEPARATOR)*
    ws_cols ENDWHILE WS*
;
blockMember:
    command | continueStatement | breakStatement | forLoop | whileLoop | ifStatement
|   returnStatement | throwStatement | functionDefinition | comment | tryStatement;
continueStatement:      CONTINUE WS*;
breakStatement:         BREAK WS*;
returnStatement:        range? ws_cols RETURN WS+ expr WS*;
throwStatement:         THROW WS+ expr WS*;

ifStatement:            ifBlock
                        elifBlock*
                        elseBlock?
                        ws_cols ENDIF WS*
;
ifBlock:                ws_cols IF WS+ expr WS* STATEMENT_SEPARATOR
                            (ws_cols blockMember STATEMENT_SEPARATOR)*
;
elifBlock:              ws_cols ELSEIF WS+ expr WS* STATEMENT_SEPARATOR
                            (ws_cols blockMember STATEMENT_SEPARATOR)*
;
elseBlock:              ws_cols ELSE WS* STATEMENT_SEPARATOR
                            (ws_cols blockMember STATEMENT_SEPARATOR)*
;

tryStatement:           tryBlock
                        catchBlock*
                        finallyBlock?
                        ws_cols ENDTRY WS*
;
tryBlock:               ws_cols TRY WS* STATEMENT_SEPARATOR
                            (ws_cols blockMember STATEMENT_SEPARATOR)*
;
catchBlock:             ws_cols CATCH WS+ pattern WS* STATEMENT_SEPARATOR
                            (ws_cols blockMember STATEMENT_SEPARATOR)*
;
pattern:                DIV patternBody DIV;
patternBody:            .*?;
finallyBlock:           ws_cols FINALLY WS* STATEMENT_SEPARATOR
                            (ws_cols blockMember STATEMENT_SEPARATOR)*
;

functionDefinition:     ws_cols FUNCTION (replace = EXCLAMATION)? WS+ (functionScope COLON)? functionName WS* L_PAREN argumentsDeclaration R_PAREN WS* STATEMENT_SEPARATOR
                            (ws_cols blockMember STATEMENT_SEPARATOR)*
                        ws_cols ENDFUNCTION WS*
;
argumentsDeclaration:   (variableName WS* (WS* COMMA WS* variableName)*)?;



////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//
//  Command related rules:
//
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
command:
    range ws_cols
    #GoToLineCommand|

    range? ws_cols ECHO (WS+ expr)* WS* comment?
    #EchoCommand|

    range? ws_cols LET WS+ expr WS*
        assignmentOperator =  (ASSIGN | PLUS_ASSIGN | MINUS_ASSIGN | STAR_ASSIGN | DIV_ASSIGN | MOD_ASSIGN | DOT_ASSIGN)
        WS* expr WS* comment?
    #LetCommand|

    range? ws_cols DELF (replace = EXCLAMATION)? WS+ (functionScope COLON)? functionName comment?
    #DelfunctionCommand|

    // add new rules above this one
    range? ws_cols commandName (WS* commandArgument)?
    #OtherCommand
;

commandArgument:
    (. | ESCAPED_BAR)*?;

range:
    rangeExpression | (rangeExpression? (rangeSeparator rangeExpression)+ rangeSeparator?);

rangeExpression:
    rangeMember | rangeOffset | (rangeMember rangeOffset);

rangeSeparator:
    COMMA | SEMI;

rangeMember:
    unsignedInt | DOT | MOD | DOLLAR | ESCAPED_QUESTION | ESCAPED_AMPERSAND | ESCAPED_SLASH  | mark | search+;

search:
    (QUESTION (~QUESTION)*? QUESTION)
|   (DIV (~DIV)*? DIV)
;
rangeOffset:
    (numberInOffset | plusOneOffset | minusOneOffset )+;
numberInOffset:
    (PLUS | MINUS)? unsignedInt;
plusOneOffset:
    PLUS;
minusOneOffset:
    MINUS;

commandName:
    alphabeticChar
|   IDENTIFIER_LOWERCASE
|   IDENTIFIER_ANY_CASE
|   IDENTIFIER_LOWERCASE_WITH_DIGITS
|   IDENTIFIER_ANY_CASE_WITH_DIGITS
|   IDENTIFIER_ANY_CASE_WITH_DIGITS_AND_UNDERSCORES
    // some odd command names (containing non-alphabetic chars)
|   PLUG
|   LSHIFT
|   RSHIFT
|   LESS
|   GREATER
|   AMPERSAND
|   TILDE
;



////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//
//  Expressions related rules
//
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
expr:                   WS* EXCLAMATION WS* expr                                                                        #UnaryExpression
                    |   expr WS* binaryOperator1 WS* expr                                                               #BinExpression1
                    |   expr WS* binaryOperator2 WS* expr                                                               #BinExpression2
                    |   expr WS* binaryOperator3 WS* expr                                                               #BinExpression3
                    |   expr WS* binaryOperator4 WS* expr                                                               #BinExpression4
                    |   expr WS* binaryOperator5 WS* expr                                                               #BinExpression5
                    |   WS* unaryOperator = (PLUS | MINUS) WS* expr                                                     #UnaryExpression
                    |	unsignedInt                                                                                     #IntExpression
                    |   unsignedFloat                                                                                   #FloatExpression
                    |   string                                                                                          #StringExpression
                    |   blob                                                                                            #BlobExpression
                    |   variable                                                                                        #VariableExpression
                    |   option                                                                                          #OptionExpression
                    |   envVariable                                                                                     #EnvVariableExpression
                    |   register                                                                                        #RegisterExpression
                    |   list                                                                                            #ListExpression
                    |   dictionary                                                                                      #DictionaryExpression
                    |   literalDictionary                                                                               #LiteralDictionaryExpression
                    |   L_PAREN WS* expr WS* R_PAREN                                                                    #WrappedExpression
                    |   expr WS* QUESTION WS* expr WS* COLON WS* expr                                                   #TernaryExpression
                    |   expr L_BRACKET WS* from = expr? WS* COLON WS* to = expr? WS* R_BRACKET                          #SublistExpression
                    |   expr L_BRACKET expr R_BRACKET                                                                   #OneElementSublistExpression
                    |   functionCall                                                                                    #FunctionCallExpression
;

binaryOperator1:        STAR | DIV | MOD;
binaryOperator2:        PLUS | MINUS | DOT;
binaryOperator3:        LESS | LESS_IC | LESS_CS
                    |   GREATER | GREATER_IC | GREATER_CS
                    |   LESS_OR_EQUALS | LESS_OR_EQUALS_IC | LESS_OR_EQUALS_CS
                    |   GREATER_OR_EQUALS | GREATER_OR_EQUALS_IC | GREATER_OR_EQUALS_CS
                    |   MATCHES | MATCHES_IC | MATCHES_CS
                    |   NOT_MATCHES | NOT_MATCHES_IC | NOT_MATCHES_CS
                    |   EQUALS | EQUALS_IC | EQUALS_CS
                    |   NOT_EQUALS | NOT_EQUALS_IC | NOT_EQUALS_CS
                    |   IS | IS_IC | IS_CS
                    |   IS_NOT | IS_NOT_IC | IS_NOT_CS
;
binaryOperator4:        AMPERSAND AMPERSAND;
binaryOperator5:        LOGICAL_OR;

register:               AT (DIGIT | alphabeticChar | MINUS | COLON | DOT | MOD | NUM | ASSIGN | STAR | PLUS | TILDE | UNDERSCORE | DIV);

variable:               (variableScope COLON)? variableName;
variableName:           anyCaseNameWithDigitsAndUnderscores;
variableScope:          anyScope;

option:                 AMPERSAND (optionScope COLON)? optionName;
optionName:             anyCaseNameWithDigitsAndUnderscores;
optionScope:            anyScope;

envVariable:            DOLLAR envVariableName;
envVariableName:        anyCaseNameWithDigitsAndUnderscores;

functionCall:           (functionScope COLON)? functionName WS* L_PAREN WS* functionArguments WS* R_PAREN;
functionName:           anyCaseNameWithDigitsAndUnderscores;
functionScope:          anyScope;
functionArguments:      (expr WS* (COMMA WS* expr WS*)*)?;

list:                   L_BRACKET WS* (expr WS* (COMMA WS* expr WS*)*)? R_BRACKET;

dictionary:             L_CURLY (WS* dictionaryEntry WS* (COMMA WS* dictionaryEntry WS*)*)? R_CURLY;
dictionaryEntry:        expr WS* COLON WS* expr;

literalDictionary:      NUM L_CURLY (WS* literalDictionaryEntry WS* (COMMA WS* literalDictionaryEntry WS*)*)? R_CURLY;
literalDictionaryEntry: literalDictionaryKey WS* COLON WS* expr;
literalDictionaryKey:   anyCaseNameWithDigitsAndUnderscores
                    |   unsignedInt
                    |   literalDictionaryKey MINUS+ literalDictionaryKey
                    |   MINUS+ literalDictionaryKey
                    |   literalDictionaryKey MINUS+
;

anyScope:               BUFFER_VARIABLE
                    |   WINDOW_VARIABLE
                    |   TABPAGE_VARIABLE
                    |   GLOBAL_VARIABLE
                    |   LOCAL_VARIABLE
                    |   SCRIPT_VARIABLE
                    |   FUNCTION_VARIABLE
                    |   VIM_VARIABLE
;



////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//
//  Names and vim datatypes
//
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
string:
                        EMPTY_DOUBLE_QUOTED_STRING
                    |   EMPTY_SINGLE_QUOTED_STRING
                    |   STRING_DOUBLE_QUOTED
                    |   (SINGLE_QUOTE | MARK_WITH_SINGLE_QUOTE)  (BACKSLASH | ~(SINGLE_QUOTE | MARK_WITH_SINGLE_QUOTE))*? SINGLE_QUOTE
;
unsignedFloat:          FLOAT;
unsignedInt:            DIGIT | INT;
blob:                   BLOB;

mark:                   MARK_WITH_SINGLE_QUOTE
                    |   MARK_WITH_BACKTICK
                    |   EMPTY_SINGLE_QUOTED_STRING  // '' is also a mark in vim
                    // we can't put below cases to MARK_WITH_SINGLE_QUOTE lexer rule, because part of "'10'<5" or "list['0']" etc will be recognized as marks ('< and '])
                    |   SINGLE_QUOTE (
                            LESS
                        |   GREATER
                        |   L_PAREN
                        |   R_PAREN
                        |   L_CURLY
                        |   R_CURLY
                        |   L_BRACKET
                        |   R_BRACKET
                        |   QUOTE
                        |   CARET
                        |   DOT
                    )
;
comment:                WS* (QUOTE | EMPTY_DOUBLE_QUOTED_STRING) ~STATEMENT_SEPARATOR *?;

anyCaseNameWithDigitsAndUnderscores:
                        anyCaseNameWithDigits
                    |   IDENTIFIER_ANY_CASE_WITH_DIGITS_AND_UNDERSCORES
;
anyCaseNameWithDigits:
                        anyCaseName
                    |   IDENTIFIER_LOWERCASE_WITH_DIGITS
                    |   IDENTIFIER_ANY_CASE_WITH_DIGITS
;
anyCaseName:            lowercaseName
                    |   uppercaseAlphabeticChar
                    |   IDENTIFIER_ANY_CASE
;
lowercaseName:          lowercaseAlphabeticChar
                    |   IDENTIFIER_LOWERCASE
                    |   keyword
;

alphabeticChar:         lowercaseAlphabeticChar
                    |   uppercaseAlphabeticChar
;
uppercaseAlphabeticChar:
                        UPPERCASE_CHAR;
lowercaseAlphabeticChar:
                        LOWERCASE_CHAR
                    |   BUFFER_VARIABLE
                    |   WINDOW_VARIABLE
                    |   TABPAGE_VARIABLE
                    |   GLOBAL_VARIABLE
                    |   LOCAL_VARIABLE
                    |   SCRIPT_VARIABLE
                    |   FUNCTION_VARIABLE
                    |   VIM_VARIABLE
;

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
                    |   DELF
;

ws_cols:                 (WS | COLON)*;


////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//
//  Lexer rules
//
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
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
// Odd command names
LSHIFT:                 '<<';
RSHIFT:                 '>>';
PLUG:                   '<Plug>';

// Types
DIGIT:                  [0-9];
INT:                    [0-9]+
                    |   '0'[xX][0-9a-fA-F]+;
FLOAT:                  [0-9]+'.'[0-9]+
                    |   [0-9]+'.'[0-9]+'e'[+-]?[0-9]+
;
BLOB:                   '0'[zZ][0-9a-fA-F]+;
EMPTY_DOUBLE_QUOTED_STRING:
                        '""';
EMPTY_SINGLE_QUOTED_STRING:
                        '\'\'';
QUOTE:                  '"';
SINGLE_QUOTE:           '\'';
ESCAPED_DOUBLE_QUOTE:   '\\"';
STRING_DOUBLE_QUOTED:   (QUOTE (ESCAPED_DOUBLE_QUOTE | ~[\n])*? QUOTE);
MARK_WITH_SINGLE_QUOTE:
                        SINGLE_QUOTE
                            (
                                    UPPERCASE_CHAR
                                |   DIGIT
                            )
;
MARK_WITH_BACKTICK:
                        BACKTICK
                            (
                                    UPPERCASE_CHAR
                                |   DIGIT
                                |   LESS
                                |   GREATER
                                |   L_PAREN
                                |   R_PAREN
                                |   L_CURLY
                                |   R_CURLY
                                |   L_BRACKET
                                |   R_BRACKET
                                |   QUOTE
                                |   CARET
                                |   DOT
                                |   BACKTICK
                            )
;

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

// Unary operators
EXCLAMATION:            '!';

// Punctuation
L_PAREN:                '(';
R_PAREN:                ')';
L_CURLY:                '{';
R_CURLY:                '}';
L_BRACKET:              '[';
R_BRACKET:              ']';
COMMA:                  ',';
SEMI:                   ';';
COLON:                  ':';
QUESTION:               '?';
DOLLAR:                 '$';
AMPERSAND:              '&';
UNDERSCORE:             '_';
TILDE:                  '~';
NUM:                    '#';
AT:                     '@';
CARET:                  '^';
BACKTICK:               '`';

// Mixed operators
PLUS:                   '+';
MINUS:                  '-';

// Binary operators
STAR:                   '*';
DIV:                    '/';
MOD:                    '%';
DOT:                    '.';

// Logical operators
LOGICAL_OR:             '||';

// Relation operators
LESS:                   '<';
LESS_IC:                '<?';
LESS_CS:                '<#';
GREATER:                '>';
GREATER_IC:             '>?';
GREATER_CS:             '>#';
LESS_OR_EQUALS:         '<=';
LESS_OR_EQUALS_IC:      '<=?';
LESS_OR_EQUALS_CS:      '<=#';
GREATER_OR_EQUALS:      '>=';
GREATER_OR_EQUALS_IC:   '>=?';
GREATER_OR_EQUALS_CS:   '>=#';
EQUALS:                 '==';
EQUALS_IC:              '==?';
EQUALS_CS:              '==#';
NOT_EQUALS:             '!=';
NOT_EQUALS_IC:          '!=?';
NOT_EQUALS_CS:          '!=#';
MATCHES:                '=~';
MATCHES_IC:             '=~?';
MATCHES_CS:             '=~#';
NOT_MATCHES:            '!~';
NOT_MATCHES_IC:         '!~?';
NOT_MATCHES_CS:         '!~#';
IS:                     'is';
IS_IC:                  'is?';
IS_CS:                  'is#';
IS_NOT:                 'isnot';
IS_NOT_IC:              'isnot?';
IS_NOT_CS:              'isnot#';


// Assignment operators
ASSIGN:                 '=';
PLUS_ASSIGN:            '+=';
MINUS_ASSIGN:           '-=';
STAR_ASSIGN:            '*=';
DIV_ASSIGN:             '/=';
MOD_ASSIGN:             '%=';
DOT_ASSIGN:             '.=';

// Escaped chars
ESCAPED_QUESTION:       '\\?';
ESCAPED_SLASH:          '\\/';
ESCAPED_AMPERSAND:      '\\&';

// Special characters
CANCEL:                 '\u0018';
BACKSPACE:              '\u0008';
ESCAPED_BAR:            '\\|' | '\u0016|';
BACKSLASH:              '\\';

// Separators
STATEMENT_SEPARATOR:    [|\r\n]+;
WS:                     [ \t]+;
