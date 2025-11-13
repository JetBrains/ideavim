grammar Vimscript;

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//
//  Multiline blocks related rules
//
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
script:
    augroup? blockMember* EOF;

forLoop:
    (WS | COLON)* FOR WS+ ((variableScope COLON)? variableName | (L_BRACKET argumentsDeclaration R_BRACKET)) WS+ IN WS* expr WS* ((inline_comment NEW_LINE) | (NEW_LINE | BAR)+)
        blockMember*
    (WS | COLON)* ENDFOR WS* ((inline_comment NEW_LINE) | (NEW_LINE | BAR)+)
;

whileLoop:
    (WS | COLON)* WHILE WS* expr WS* ((inline_comment NEW_LINE) | (NEW_LINE | BAR))
        blockMember*
    (WS | COLON)* ENDWHILE WS* ((inline_comment NEW_LINE) | (NEW_LINE | BAR))
;
blockMember:
    command | finishStatement | continueStatement | breakStatement | forLoop| whileLoop | ifStatement
|   returnStatement | throwStatement | functionDefinition | tryStatement | ((WS | COLON)* (NEW_LINE | BAR)) | autoCmd | comment;

comment:                (WS | COLON)* QUOTE ~(NEW_LINE)* NEW_LINE;

finishStatement:        (WS | COLON)* FINISH WS* (NEW_LINE | BAR);
continueStatement:      (WS | COLON)* CONTINUE WS* (NEW_LINE | BAR);
breakStatement:         (WS | COLON)* BREAK WS* (NEW_LINE | BAR);
returnStatement:        (WS | COLON)* range? (WS | COLON)* RETURN (WS+ expr)? WS* (NEW_LINE | BAR);
throwStatement:         (WS | COLON)* THROW WS+ expr WS* (NEW_LINE | BAR);

ifStatement:            ifBlock
                        elifBlock*
                        elseBlock?
                        (WS | COLON)* ENDIF WS* ((inline_comment NEW_LINE) | (NEW_LINE | BAR))
;
ifBlock:                (WS | COLON)* IF WS* expr WS* ((inline_comment NEW_LINE) | (NEW_LINE | BAR))
                           blockMember*
;
elifBlock:              (WS | COLON)* ELSEIF WS* expr WS* ((inline_comment NEW_LINE) | (NEW_LINE | BAR))
                           blockMember*
;
elseBlock:              (WS | COLON)* ELSE WS* ((inline_comment NEW_LINE) | (NEW_LINE | BAR))
                            blockMember*
;

tryStatement:           tryBlock
                        catchBlock*
                        finallyBlock?
                        (WS | COLON)* ENDTRY WS* ((inline_comment NEW_LINE) | (NEW_LINE | BAR))
;
tryBlock:               (WS | COLON)* TRY WS* ((inline_comment NEW_LINE) | (NEW_LINE | BAR))
                            blockMember*
;
catchBlock:             (WS | COLON)* CATCH WS* pattern? WS* ((inline_comment NEW_LINE) | (NEW_LINE | BAR))
                            blockMember*
;
pattern:                DIV patternBody DIV;
patternBody:            ~(NEW_LINE | BAR)*?;
finallyBlock:           (WS | COLON)* FINALLY WS* ((inline_comment NEW_LINE) | (NEW_LINE | BAR))
                            blockMember*
;

functionDefinition:
                        (WS | COLON)* FUNCTION (replace = BANG)? WS+ (SID | SNR)? (anyCaseNameWithDigitsAndUnderscores NUM)* (functionScope COLON)? (functionName | (literalDictionaryKey (DOT literalDictionaryKey)+)) WS* L_PAREN WS* argumentsDeclaration WS* R_PAREN WS* (functionFlag WS*)* ((inline_comment NEW_LINE) | (NEW_LINE | BAR)+)
                            blockMember*
                        (WS | COLON)* ENDFUNCTION WS* ((inline_comment NEW_LINE) | (NEW_LINE | BAR))
;
functionFlag:           RANGE | ABORT | DICT | CLOSURE;
argumentsDeclaration:   (ETC | defaultValue (WS* COMMA WS* defaultValue)* (WS* COMMA WS* ETC WS*)? | (variableName (WS* COMMA WS* variableName)* (WS* COMMA WS* defaultValue)* (WS* COMMA WS* ETC WS*)?))?;
defaultValue:           variableName WS* ASSIGN WS* expr;

autoCmd:                (WS | COLON)* AUTOCMD commandArgument = ~(NEW_LINE)*? NEW_LINE;

augroup:                (WS | COLON)* AUGROUP .*? AUGROUP WS+ END WS* NEW_LINE;

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//
//  Command related rules:
//
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
command:
    (WS | COLON)* (range | shortRange) (WS | COLON)* (NEW_LINE | BAR)+
    #GoToLineCommand|

//    (WS | COLON)* range? (WS | COLON)* LET WS+ (~(NEW_LINE | BAR)+ | string)+ (NEW_LINE | BAR)+
    letCommands
    #LetCommand|

    (WS | COLON)* range? (WS | COLON)* ECHO (WS* expr)* WS* (NEW_LINE | BAR)+
    #EchoCommand|

    (WS | COLON)* range? (WS | COLON)* DELFUNCTION (replace = BANG)? WS+ (functionScope COLON)? functionName ((inline_comment NEW_LINE+) | (NEW_LINE | BAR)+)
    #DelfunctionCommand|

    (WS | COLON)* range? (WS | COLON)* CALL WS+ expr WS* ((inline_comment NEW_LINE+) | (NEW_LINE | BAR)+)
    #CallCommand|

    (WS | COLON)* range? (WS | COLON)* EXECUTE WS* (expr WS*)* (NEW_LINE | BAR)+
    #ExecuteCommand|

    (WS | COLON)* range? (WS | COLON)* lShift
    WS* ((commandArgumentWithoutBars? inline_comment NEW_LINE) | (commandArgumentWithoutBars? NEW_LINE) | (commandArgumentWithoutBars? BAR))
    (NEW_LINE | BAR)*
    #ShiftLeftCommand|

    (WS | COLON)* range? (WS | COLON)* rShift
    WS* ((commandArgumentWithoutBars? inline_comment NEW_LINE) | (commandArgumentWithoutBars? NEW_LINE) | (commandArgumentWithoutBars? BAR))
    (NEW_LINE | BAR)*
    #ShiftRightCommand|

    (WS | COLON)* range? (WS | COLON)*
      name = (
          ACTIONLIST | ACTION | ASCII | AT
        | ASSIGN    // `:=` print last line number
        | B_LOWERCASE | BUFFER| BUFFER_CLOSE | BUFFER_LIST
        | CLASS | CLEARJUMPS | CMD_CLEAR | COPY
        | D_LOWERCASE | DELETE | DELCOMMAND | DELMARKS | DIGRAPHS | DUMPLINE
        | E_LOWERCASE | EDIT_FILE | EXIT
        | F_LOWERCASE | FILE | FIND
        | GOTO
        | HISTORY
        | J_LOWERCASE | JOIN | JUMPS
        | K_LOWERCASE
        | LOCKVAR
        | M_LOWERCASE | MARK | MARKS | MOVE_TEXT
        | N_LOWERCASE | N_UPPERCASE | NEXT_FILE | NOHLSEARCH
        | ONLY
        | P_LOWERCASE | P_UPPERCASE | PACKADD | PLUG | PREVIOUS_FILE | PRINT | PROMPT_REPLACE | PROMPTFIND | PUT
        | Q_LOWERCASE | QUIT
        | REDO
        | SET | SELECT_LAST_FILE | SELECT_FIRST_FILE | SELECT_FILE | SETGLOBAL | SETHANDLER | SETLOCAL | SHELL | SOURCE | SPLIT | SYMBOL
        | T_LOWERCASE | TABCLOSE | TABMOVE | TABNEXT | TABONLY | TABPREVIOUS
        | U_LOWERCASE | UNDO | UNLOCKVAR
        | VSPLIT
        | W_LOWERCASE | WRITE | WRITE_ALL | WRITE_NEXT | WRITE_PREVIOUS | WRITE_QUIT
        | X_LOWERCASE
        | Y_LOWERCASE | YANK
      )
      bangModifier = BANG?
    WS* ((commandArgumentWithoutBars? inline_comment NEW_LINE) | (commandArgumentWithoutBars? NEW_LINE) | (commandArgumentWithoutBars? BAR)) (NEW_LINE | BAR)*
    #CommandWithComment|

    (WS | COLON)* range? (WS | COLON)*
      name = (
          CMD
        | H_LOWERCASE | HELP
        | MAP | MAP_CLEAR
        | NORMAL
        | REGISTERS
        | SORT
        | UNMAP
      )
      bangModifier = BANG?
    WS* commandArgumentWithoutBars? (NEW_LINE | BAR)+
    #CommandWithoutComments|

    (WS | COLON)* range? (WS | COLON)*
      name = (
          AMPERSAND
        | BANG
        | G_LOWERCASE | GLOBAL
        | V_LOWERCASE | VGLOBAL
        | S_LOWERCASE | SUBSTITUTE
        | TILDE
      )
      bangModifier = BANG?
    WS* commandArgumentWithBars? NEW_LINE+
    #CommandWithBars|

    (WS | COLON)* range? (WS | COLON)* commandName (bangModifier = BANG?) WS* commandArgumentWithBars? (NEW_LINE | BAR)+
    #OtherCommand
;
commandArgumentWithBars: ~(NEW_LINE)+;
commandArgumentWithoutBars: ~(NEW_LINE | BAR)+;
lShift: LESS+;
rShift: GREATER+;

letCommands:
    (WS | COLON)* range? (WS | COLON)* LET WS+ (unpack = unpackLValue | lvalue = expr) WS* assignmentOperator WS* rvalue = expr
        WS* ((inline_comment NEW_LINE) | (NEW_LINE | BAR)+)
    #Let1Command|

    (WS | COLON)* range? (WS | COLON)* LET WS+ commandArgument = ~(NEW_LINE)* NEW_LINE+
    #Let2Command
;

unpackLValue:
    L_BRACKET WS* (lvalues += expr WS* (COMMA WS* lvalues += expr WS*)*)+ (SEMI WS* rest = expr)? WS* R_BRACKET
;

assignmentOperator:
    ASSIGN | plusAssign | minusAssign | startAssign | divAssign | modAssign | dotAssign | dotDotAssign;
plusAssign:
    PLUS ASSIGN;
minusAssign:
    MINUS ASSIGN;
startAssign:
    STAR ASSIGN;
divAssign:
    DIV ASSIGN;
modAssign:
    MOD ASSIGN;
dotAssign:
    DOT ASSIGN;
dotDotAssign:
    DOT DOT ASSIGN;

shortRange:
    ((QUESTION (~QUESTION)* QUESTION?) | (DIV (~DIV)* DIV?));
range:
    rangeUnit+;

rangeUnit:
    (rangeExpression? WS* rangeSeparator WS*) | (rangeExpression WS* rangeSeparator? WS*);

rangeExpression:
    (rangeMember rangeOffset?) | (rangeMember? rangeOffset);

rangeMember:
    unsignedInt | DOT | MOD | DOLLAR | ESCAPED_QUESTION | ESCAPED_AMPERSAND | ESCAPED_SLASH  | mark | search+;

rangeSeparator:
    COMMA | SEMI;

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
    (LESS)+
|   (GREATER)+
|   anyCaseNameWithDigitsAndUnderscoresExceptKeywords
|   commandName (bang = BANG)
;



////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//
//  Expressions related rules
//
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//
// These are in precedence order, from most significant to least. It reflects the precedence list in the Vim docs
// (see `:help expression-syntax`) albeit without the nested grammar rules.
// Note that expr10 has higher precedence than expr11 because Vim defines expr10 rules in terms of expr11 or expr10 with
// trailing syntax. If we put expr11 higher, it will match the leading syntax and then fail to match the trailing syntax
expr:
                    // expr10
                        expr L_BRACKET WS* expr WS* R_BRACKET                                   #IndexedExpression
                    |   expr L_BRACKET WS* from = expr? WS* COLON WS* to = expr? WS* R_BRACKET  #SublistExpression
                    |   expr WS* L_PAREN WS* functionArguments WS* R_PAREN                      #FunctionCallExpression
                    |   expr WS* ARROW WS* functionCall                                         #FunctionAsMethodCall1
                    |   expr WS* ARROW WS* lambda L_PAREN WS* functionArguments WS* R_PAREN     #FunctionAsMethodCall2
                    // TODO: expr10.name
                    // expr11
                    // Vim applies unary PLUS or MINUS to a numeric constant with higher precedence than to any other
                    // expression/part expression
                    |   unaryOperator = (PLUS | MINUS)? WS* unsignedInt                         #IntExpression
                    |   unaryOperator = (PLUS | MINUS)? WS* unsignedFloat                       #FloatExpression
                    |   string                                                                  #StringExpression
                    |   blob                                                                    #BlobExpression
                    |   list                                                                    #ListExpression
                    |   dictionary                                                              #DictionaryExpression
                    |   literalDictionary                                                       #LiteralDictionaryExpression
                    |   option                                                                  #OptionExpression
                    |   L_PAREN WS* expr WS* R_PAREN                                            #WrappedExpression
                    // Make sure functionCall is before variable so foo() isn't treated as a funcref variable invocation
                    |   functionCall                                                            #FunctionCallExpression
                    |   variable                                                                #VariableExpression
                    |   envVariable                                                             #EnvVariableExpression
                    |   register                                                                #RegisterExpression
                    |   lambda L_PAREN WS* functionArguments WS* R_PAREN                        #LambdaFunctionCallExpression
                    |   lambda                                                                  #LambdaExpression
                    // expr9
                    |   unaryOperator = (BANG | PLUS | MINUS) WS* expr                          #UnaryExpression
                    // expr8 is Vim9 typecast
                    // expr7: * / %
                    |   expr WS* multiplicativeOperator WS* expr                                #MultiplicativeExpression
                    // expr6: + - . ..
                    |   expr WS* additiveOperator WS* expr                                      #AdditiveExpression
                    // expr5: left/right bitwise shift
                    |   expr WS* bitwiseShiftOperator WS* expr                                  #BitwiseShiftExpression
                    // expr4: equality/is/comparisons
                    |   expr WS* comparisonOperator WS* expr                                    #ComparisonExpression
                    // expr3: logical AND
                    |   expr WS* logicalAndOperator WS* expr                                    #LogicalAndExpression
                    // expr2: logical OR
                    |   expr WS* logicalOrOperator WS* expr                                     #LogicalOrExpression
                    // expr1: ternary/falsy
                    |   <assoc=right> expr WS* QUESTION QUESTION WS* expr                       #FalsyExpression
                    |   <assoc=right> expr WS* QUESTION WS* expr WS* COLON WS* expr             #TernaryExpression
;

multiplicativeOperator: STAR | DIV | MOD;
additiveOperator:       PLUS | MINUS | DOT | (DOT DOT);
comparisonOperator:     LESS | LESS_IC | LESS_CS
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
bitwiseShiftOperator:   LESS LESS | GREATER GREATER;
logicalAndOperator:     AMPERSAND AMPERSAND;
logicalOrOperator:      BAR BAR;

register:               AT (DIGIT | lowercaseAlphabeticChar | uppercaseAlphabeticChar | MINUS | COLON | DOT | MOD | NUM | ASSIGN | STAR | PLUS | TILDE | UNDERSCORE | DIV | AT | QUOTE);
// todo argumentDeclaration but without default values
lambda:                 L_CURLY WS* argumentsDeclaration WS* ARROW WS* expr WS* R_CURLY;

variable:               (variableScope COLON)? variableName;
variableName:           curlyBracesName;
variableScope:          anyScope;

curlyBracesName:        element+;
element:                anyCaseNameWithDigitsAndUnderscores | unsignedInt | L_CURLY WS* expr WS* R_CURLY;

option:                 AMPERSAND (optionScope COLON)? optionName;
optionName:             anyCaseNameWithDigitsAndUnderscores;
optionScope:            anyScope;

envVariable:            DOLLAR envVariableName;
envVariableName:        anyCaseNameWithDigitsAndUnderscores;

functionCall:           (functionScope COLON)? (anyCaseNameWithDigitsAndUnderscores NUM)* functionName WS* L_PAREN WS* functionArguments WS* R_PAREN;
functionName:           curlyBracesName;
functionScope:          anyScope;
functionArguments:      (functionArgument WS* (COMMA WS* functionArgument WS*)*)?;
functionArgument:       expr | (anyScope COLON);

list:                   L_BRACKET WS* (expr WS* (COMMA WS* expr WS*)*)? COMMA? WS* R_BRACKET;

dictionary:             L_CURLY WS* (dictionaryEntry WS* (COMMA WS* dictionaryEntry WS*)*)? COMMA? WS* R_CURLY;
dictionaryEntry:        expr WS* COLON WS* expr;

literalDictionary:      NUM L_CURLY WS* (literalDictionaryEntry WS* (COMMA WS* literalDictionaryEntry WS*)* COMMA? WS*)? R_CURLY;
literalDictionaryEntry: literalDictionaryKey WS* COLON WS* expr;
literalDictionaryKey:   anyCaseNameWithDigitsAndUnderscores
                    |   unsignedInt
                    |   literalDictionaryKey MINUS+ literalDictionaryKey
                    |   MINUS+ literalDictionaryKey
                    |   literalDictionaryKey MINUS+
;

anyScope:               B_LOWERCASE // buffer variable
                    |   W_LOWERCASE // window variable
                    |   T_LOWERCASE // tabpage variable
                    |   G_LOWERCASE // global variable
                    |   L_LOWERCASE // local variable
                    |   S_LOWERCASE // script variable
                    |   A_LOWERCASE // function variable
                    |   V_LOWERCASE // vim variable
;



////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//
//  Names and vim datatypes
//
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
string:
                        QUOTE ~(NEW_LINE | QUOTE)* QUOTE
                    |   ESCAPED_SINGLE_QUOTE
                    |   ESCAPED_SINGLE_QUOTE ESCAPED_SINGLE_QUOTE
                    |   (SINGLE_QUOTE | MARK_SINGLE_QUOTED | (ESCAPED_SINGLE_QUOTE SINGLE_QUOTE))  ~(SINGLE_QUOTE | NEW_LINE)*? SINGLE_QUOTE
;
unsignedFloat:          FLOAT;
unsignedInt:            DIGIT | INT;
blob:                   BLOB;

mark:                   (SINGLE_QUOTE (DIGIT | LESS | GREATER | L_PAREN | R_PAREN | L_CURLY | R_CURLY | L_BRACKET | R_BRACKET | QUOTE | CARET | DOT | BACKTICK | SINGLE_QUOTE))
                    |   (BACKTICK (DIGIT | LESS | GREATER | L_PAREN | R_PAREN | L_CURLY | R_CURLY | L_BRACKET | R_BRACKET | QUOTE | CARET | DOT | BACKTICK | SINGLE_QUOTE))
                    |   MARK_SINGLE_QUOTED
                    |   MARK_BACKTICK
;
inline_comment:         QUOTE ~(NEW_LINE)*?;
anyCaseNameWithDigitsAndUnderscores:
                        lowercaseAlphabeticChar
                    |   uppercaseAlphabeticChar
                    |   keyword
                    |   operator
                    |   IDENTIFIER_LOWERCASE
                    |   IDENTIFIER_ANY_CASE
                    |   anyCaseNameWithDigitsAndUnderscores (anyCaseNameWithDigitsAndUnderscores | DIGIT | INT | UNDERSCORE)+
;

anyCaseNameWithDigitsAndUnderscoresExceptKeywords:
                        lowercaseAlphabeticChar
                    |   uppercaseAlphabeticChar
                    |   IDENTIFIER_LOWERCASE
                    |   IDENTIFIER_ANY_CASE
                    |   anyCaseNameWithDigitsAndUnderscores (anyCaseNameWithDigitsAndUnderscores | DIGIT | INT | UNDERSCORE)+
;

uppercaseAlphabeticChar:
                        A_UPPERCASE
                    |   B_UPPERCASE
                    |   C_UPPERCASE
                    |   D_UPPERCASE
                    |   E_UPPERCASE
                    |   F_UPPERCASE
                    |   G_UPPERCASE
                    |   H_UPPERCASE
                    |   I_UPPERCASE
                    |   J_UPPERCASE
                    |   K_UPPERCASE
                    |   L_UPPERCASE
                    |   M_UPPERCASE
                    |   N_UPPERCASE
                    |   O_UPPERCASE
                    |   P_UPPERCASE
                    |   Q_UPPERCASE
                    |   R_UPPERCASE
                    |   S_UPPERCASE
                    |   T_UPPERCASE
                    |   U_UPPERCASE
                    |   V_UPPERCASE
                    |   W_UPPERCASE
                    |   X_UPPERCASE
                    |   Y_UPPERCASE
                    |   Z_UPPERCASE
;
lowercaseAlphabeticChar:
                        A_LOWERCASE
                    |   B_LOWERCASE
                    |   C_LOWERCASE
                    |   D_LOWERCASE
                    |   E_LOWERCASE
                    |   F_LOWERCASE
                    |   G_LOWERCASE
                    |   H_LOWERCASE
                    |   I_LOWERCASE
                    |   J_LOWERCASE
                    |   K_LOWERCASE
                    |   L_LOWERCASE
                    |   M_LOWERCASE
                    |   N_LOWERCASE
                    |   O_LOWERCASE
                    |   P_LOWERCASE
                    |   Q_LOWERCASE
                    |   R_LOWERCASE
                    |   S_LOWERCASE
                    |   T_LOWERCASE
                    |   U_LOWERCASE
                    |   V_LOWERCASE
                    |   W_LOWERCASE
                    |   X_LOWERCASE
                    |   Y_LOWERCASE
                    |   Z_LOWERCASE
;

keyword:                ABORT
                    |   AUGROUP
                    |   AUTOCMD
                    |   BREAK
                    |   CATCH
                    |   CLOSURE
                    |   CONTINUE
                    |   DICT
                    |   ELSE
                    |   ELSEIF
                    |   ENDIF
                    |   ENDFOR
                    |   ENDFUNCTION
                    |   ENDTRY
                    |   ENDWHILE
                    |   FINALLY
                    |   FOR
                    |   FUNCTION
                    |   IF
                    |   IN
                    |   RANGE
                    |   THROW
                    |   TRY
                    |   WHILE
                    |   existingCommands
;
operator:               IS
                    |   IS_NOT
;
existingCommands:       ACTION
                    |   ACTIONLIST
                    |   ASCII
                    |   BUFFER
                    |   BUFFER_CLOSE
                    |   BUFFER_LIST
                    |   CALL
                    |   CLASS
                    |   CMD
                    |   CMD_CLEAR
                    |   COPY
                    |   DELCOMMAND
                    |   DELETE
                    |   DELFUNCTION
                    |   DELMARKS
                    |   DIGRAPHS
                    |   DUMPLINE
                    |   ECHO
                    |   EDIT_FILE
                    |   EXECUTE
                    |   EXIT
                    |   FILE
                    |   FIND
                    |   GLOBAL
                    |   GOTO
                    |   HELP
                    |   HISTORY
                    |   JOIN
                    |   JUMPS
                    |   LET
                    |   MAP
                    |   MAP_CLEAR
                    |   MARK
                    |   MARKS
                    |   MOVE_TEXT
                    |   NEXT_FILE
                    |   NOHLSEARCH
                    |   NORMAL
                    |   ONLY
                    |   PACKADD
                    |   PLUG
                    |   PREVIOUS_FILE
                    |   PRINT
                    |   PROMPTFIND
                    |   PROMPT_REPLACE
                    |   PUT
                    |   QUIT
                    |   REDO
                    |   REGISTERS
                    |   RETURN
                    |   SELECT_FILE
                    |   SELECT_FIRST_FILE
                    |   SELECT_LAST_FILE
                    |   SET
                    |   SETGLOBAL
                    |   SETLOCAL
                    |   SETHANDLER
                    |   SHELL
                    |   SORT
                    |   SOURCE
                    |   SPLIT
                    |   SUBSTITUTE
                    |   SYMBOL
                    |   TABCLOSE
                    |   TABMOVE
                    |   TABNEXT
                    |   TABONLY
                    |   TABPREVIOUS
                    |   UNDO
                    |   UNMAP
                    |   VGLOBAL
                    |   VSPLIT
                    |   WRITE
                    |   WRITE_ALL
                    |   WRITE_NEXT
                    |   WRITE_PREVIOUS
                    |   WRITE_QUIT
                    |   YANK
;
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//
//  Lexer rules
//
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Lowercase chars
A_LOWERCASE:            'a';
B_LOWERCASE:            'b';
C_LOWERCASE:            'c';
D_LOWERCASE:            'd';
E_LOWERCASE:            'e';
F_LOWERCASE:            'f';
G_LOWERCASE:            'g';
H_LOWERCASE:            'h';
I_LOWERCASE:            'i';
J_LOWERCASE:            'j';
K_LOWERCASE:            'k';
L_LOWERCASE:            'l';
M_LOWERCASE:            'm';
N_LOWERCASE:            'n';
O_LOWERCASE:            'o';
P_LOWERCASE:            'p';
Q_LOWERCASE:            'q';
R_LOWERCASE:            'r';
S_LOWERCASE:            's';
T_LOWERCASE:            't';
U_LOWERCASE:            'u';
V_LOWERCASE:            'v';
W_LOWERCASE:            'w';
X_LOWERCASE:            'x';
Y_LOWERCASE:            'y';
Z_LOWERCASE:            'z';

// Uppercase chars
A_UPPERCASE:            'A';
B_UPPERCASE:            'B';
C_UPPERCASE:            'C';
D_UPPERCASE:            'D';
E_UPPERCASE:            'E';
F_UPPERCASE:            'F';
G_UPPERCASE:            'G';
H_UPPERCASE:            'H';
I_UPPERCASE:            'I';
J_UPPERCASE:            'J';
K_UPPERCASE:            'K';
L_UPPERCASE:            'L';
M_UPPERCASE:            'M';
N_UPPERCASE:            'N';
O_UPPERCASE:            'O';
P_UPPERCASE:            'P';
Q_UPPERCASE:            'Q';
R_UPPERCASE:            'R';
S_UPPERCASE:            'S';
T_UPPERCASE:            'T';
U_UPPERCASE:            'U';
V_UPPERCASE:            'V';
W_UPPERCASE:            'W';
X_UPPERCASE:            'X';
Y_UPPERCASE:            'Y';
Z_UPPERCASE:            'Z';

MARK_SINGLE_QUOTED:     '\''[a-zA-Z];
MARK_BACKTICK:          '`'[a-zA-Z];

// Keywords
ABORT:                  'abort';
AUTOCMD:                'au' | 'aut' | 'auto' | 'autoc' | 'autocm' | 'autocmd';
AUGROUP:                'aug' | 'augr' | 'augro' | 'augrou' | 'augroup';
BREAK:                  'brea' | 'break';
CATCH:                  'cat' | 'catc'| 'catch';
CLOSURE:                'closure';
CONTINUE:               'con' | 'cont' | 'conti' | 'contin' | 'continu' | 'continue';
DICT:                   'dict';
ELSE:                   'el' | 'els' | 'else';
ELSEIF:                 'elsei' | 'elseif';
END:                    'END';
ENDFOR:                 'endfo' | 'endfor';
ENDIF:                  'en' | 'end' | 'endi' | 'endif';
ENDFUNCTION:            'endf' | 'endfu' | 'endfun' | 'endfunc' | 'endfunct' | 'endfuncti' | 'endfunctio' | 'endfunction';
ENDTRY:                 'endt' | 'endtr' | 'endtry';
ENDWHILE:               'endw' | 'endwh' | 'endwhi' | 'endwhil' |'endwhile';
FINALLY:                'fina' | 'final' | 'finall' | 'finally';
FINISH:                 'fini' | 'finis' | 'finish';
FOR:                    'for';
FUNCTION:               'fu' | 'fun' | 'func' | 'funct' | 'functi' | 'functio' | 'function';
IF:                     'if';
IN:                     'in';
RANGE:                  'range';
RETURN:                 'return';
THROW:                  'throw';
SID:                    '<SID>';
SNR:                    '<SNR>';
TRY:                    'try';
WHILE:                  'wh' | 'whi' | 'whil' | 'while';

// Commands
// Keep in alphabetical order. Aim to use the name of the command as the name of the token, use underscores for clarity
// (E.g. WRITE_ALL instead of WALL)
ACTION:                 'action';
ACTIONLIST:             'actionlist';
ASCII:                  'as' | 'asc' | 'asci' | 'ascii';
BUFFER:                 'bu' | 'buf' | 'buff' | 'buffe' | 'buffer';
BUFFER_CLOSE:           'bd' | 'bde' | 'bdel' | 'bdele' | 'bdelet' | 'bdelete';
BUFFER_LIST:            'buffers' | 'ls' | 'files';
CALL:                   'cal' | 'call';
CLASS:                  'cla' | 'clas' | 'class';
CLEARJUMPS:             'cle' | 'clea' | 'clear' | 'clearj' | 'clearju' | 'clearjum' | 'clearjump' | 'clearjumps';
CMD:                    'com' | 'comm' | 'comma' | 'comman' | 'command';
CMD_CLEAR:              'comc' | 'comcl' | 'comcle' | 'comclea' | 'comclear';
COPY:                   'co' | 'cop' | 'copy';
DELCOMMAND:             'delc' | 'delco' | 'delcom' | 'delcomm' | 'delcomma' | 'delcomman' | 'delcommand';
DELETE:                 'de' | 'del' | 'dele' | 'delet' | 'delete';
DELFUNCTION:            'delf' | 'delfu' | 'delfun' | 'delfunc'| 'delfunct' | 'delfuncti' | 'delfunctio' | 'delfunction';
DELMARKS:               'delm' | 'delma' | 'delmar' | 'delmark' | 'delmarks';
DIGRAPHS:               'dig' | 'digr' | 'digra' | 'digrap' | 'digraph' | 'digraphs';
DUMPLINE:               'dump' | 'dumpl' | 'dumpli' | 'dumplin' | 'dumpline';
ECHO:                   'ec' | 'ech' | 'echo';
EDIT_FILE:              'bro' | 'brow' | 'brows' | 'browse' | 'ed' | 'edi' | 'edit';
EXECUTE:                'exe' | 'exec' | 'execu' | 'execut' | 'execute';
EXIT:                   'wqa' | 'wqal' | 'wqall' | 'qa' | 'qal' | 'qall' | 'xa' | 'xal' | 'xall' | 'quita' | 'quital' | 'quitall';
FILE:                   'fi' | 'fil' | 'file';
FIND:                   'fin' | 'find';
GLOBAL:                 'gl' | 'glo' | 'glob' | 'globa' | 'global';
GOTO:                   'go' | 'got' | 'goto';
HELP:                   'he' | 'hel' | 'help';
HISTORY:                'his' | 'hist' | 'histo' | 'histor' | 'history';
JOIN:                   'jo' | 'joi' | 'join';
JUMPS:                  'ju' | 'jum' | 'jump' | 'jumps';
LET:                    'let';
LOCKVAR:                'lockv' | 'lockva' | 'lockvar';
MARK:                   'ma' | 'mar' | 'mark';
MARKS:                  'marks';
MOVE_TEXT:              'mo' | 'mov' | 'move';
NEXT_FILE:              'ne' | 'nex' | 'next' | 'bn' | 'bne' | 'bnex' | 'bnext';
NOHLSEARCH:             'noh' | 'nohl' | 'nohls' | 'nohlse' | 'nohlsea' | 'nohlsear' | 'nohlsearc' | 'nohlsearch';
NORMAL:                 'norm' | 'norma' | 'normal';
ONLY:                   'on' | 'onl' | 'only';
PACKADD:                'pa' | 'pac' | 'pack' | 'packa' | 'packad' | 'packadd';
PLUG:                   'Plug' | 'Plugi' | 'Plugin';
PREVIOUS_FILE:          'Ne' | 'Nex' | 'Next' | 'prev'| 'previ' | 'previo' | 'previou' | 'previous' | 'bp' | 'bpr' | 'bpre' |'bprev'| 'bprevi' | 'bprevio' | 'bpreviou' | 'bprevious';
PRINT:                  'pr' | 'pri' | 'prin' | 'print' | 'Pr' | 'Pri' | 'Prin' | 'Print';
PROMPTFIND:             'pro' | 'prom' | 'promp' | 'prompt' | 'promptf' | 'promptfi' | 'promptfin' | 'promptfind';
PROMPT_REPLACE:         'promptr' | 'promptre' | 'promptrep' | 'promptrepl';
PUT:                    'pu' | 'put';
QUIT:                   'qu' | 'qui' | 'quit' | 'clo' | 'clos' | 'close' | 'hid' | 'hide';
REDO:                   'red' | 'redo';
REGISTERS:              'di' | 'dis' | 'disp' | 'displ' | 'displa' | 'display' | 'reg' | 'regi' | 'regis' | 'regist' | 'registe' | 'register' | 'registers';
SELECT_FILE:            'argu' | 'argum' | 'argume' | 'argumen' | 'argument';
SELECT_FIRST_FILE:      'fir' | 'firs' | 'first';
SELECT_LAST_FILE:       'la' | 'las' | 'last';
SET:                    'se' | 'set';
SETGLOBAL:              'setg' | 'setgl' | 'setglo' | 'setglob' | 'setgloba' | 'setglobal';
SETLOCAL:               'setl' | 'setlo' | 'setloc' | 'setloca' | 'setlocal';
SETHANDLER:             'sethandler';
SHELL:                  'sh' | 'she' | 'shel' | 'shell';
SORT:                   'sor' | 'sort';
SOURCE:                 'so' | 'sou' | 'sour' | 'sourc' | 'source';
SPLIT:                  'sp' | 'spl' | 'spli' | 'split';
SUBSTITUTE:             'su' | 'sub' | 'subs' | 'subst' | 'substi' | 'substit' | 'substitu' | 'substitut' | 'substitute';
SYMBOL:                 'sym' | 'symb' | 'symbo' | 'symbol';
TABCLOSE:               'tabc' | 'tabcl' | 'tabclo' | 'tabclos' | 'tabclose';
TABMOVE:                'tabm' | 'tabmo' | 'tabmov' | 'tabmove';
TABNEXT:                'tabn' | 'tabne' | 'tabnex' | 'tabnext';
TABONLY:                'tabo' | 'tabon' | 'tabonl' | 'tabonly';
TABPREVIOUS:            'tabp' | 'tabpr' | 'tabpre' | 'tabprev' | 'tabprevi' | 'tabprevio' | 'tabpreviou' | 'tabprevious' | 'tabN' | 'tabNe' | 'tabNex' | 'tabNext';
UNDO:                   'un' | 'und' | 'undo';
UNLOCKVAR:              'unlo' | 'unloc' | 'unlock' | 'unlockv' | 'unlockva' | 'unlockvar';
VGLOBAL:                'vg' | 'vgl' | 'vglo' | 'vglob' | 'vgloba' | 'vglobal';
VSPLIT:                 'vs' | 'vsp' | 'vspl' | 'vspli' | 'vsplit';
WRITE:                  'wr' | 'wri' | 'writ' | 'write';
WRITE_ALL:              'wa' | 'wal' | 'wall';
WRITE_NEXT:             'wn' | 'wne' | 'wnex' | 'wnext';
WRITE_PREVIOUS:         'wN' | 'wNe' | 'wNex' | 'wNext' | 'wp' | 'wpr' | 'wpre' | 'wprev' | 'wprevi' | 'wprevio' | 'wpreviou' | 'wprevious';
WRITE_QUIT:             'wq' | 'exi' | 'exit' | 'xi' | 'xit';
YANK:                   'ya' | 'yan' | 'yank';
MAP:                    'map'
                    |   'smap'
                    |   'nm' | 'vm' | 'xm' | 'om' | 'im' | 'cm'
                    |   (('nm' | 'vm' | 'xm' | 'om' | 'im' | 'cm') 'a')
                    |   (('nm' | 'vm' | 'xm' | 'om' | 'im' | 'cm') 'ap')
                    |   'no' | 'nn' | 'vn' | 'xn' | 'ono' | 'ino' | 'cno'
                    |   'nno' | 'vno' | 'xno'
                    |   (('no' | 'nno' | 'vno' | 'xno' | 'ono' | 'ino' | 'cno') 'r')
                    |   (('no' | 'nno' | 'vno' | 'xno' | 'ono' | 'ino' | 'cno') 're')
                    |   (('no' | 'nno' | 'vno' | 'xno' | 'ono' | 'ino' | 'cno') 'rem')
                    |   (('no' | 'nno' | 'vno' | 'xno' | 'ono' | 'ino' | 'cno') 'rema')
                    |   (('no' | 'nno' | 'vno' | 'xno' | 'ono' | 'ino' | 'cno') 'remap')
                    |   ('snor' | 'snore' | 'snorem' | 'snorema' | 'snoremap');
MAP_CLEAR:              ('mapc' | 'nmapc' | 'vmapc' | 'xmapc' | 'smapc' | 'omapc' | 'imapc' | 'cmapc')
                    |   (('mapc' | 'nmapc' | 'vmapc' | 'xmapc' | 'smapc' | 'omapc' | 'imapc' | 'cmapc') 'l')
                    |   (('mapc' | 'nmapc' | 'vmapc' | 'xmapc' | 'smapc' | 'omapc' | 'imapc' | 'cmapc') 'le')
                    |   (('mapc' | 'nmapc' | 'vmapc' | 'xmapc' | 'smapc' | 'omapc' | 'imapc' | 'cmapc') 'lea')
                    |   (('mapc' | 'nmapc' | 'vmapc' | 'xmapc' | 'smapc' | 'omapc' | 'imapc' | 'cmapc') 'lear');
UNMAP:                  'unm' | 'nun' | 'vu' | 'xu' | 'sunm' | 'ou' | 'iu' | 'cu'
                    |   'vun' | 'xun' | 'oun' | 'iun' | 'cun'
                    |   'nunm' | 'vunm' | 'xunm' | 'ounm' | 'iunm' | 'cunm'
                    |   'unma' | 'nunma' | 'vunma' | 'xunma' | 'sunma' | 'ounma' | 'iunma' | 'cunma'
                    |   'unmap' | 'nunmap' | 'vunmap' | 'xunmap' | 'sunmap' | 'ounmap' | 'iunmap' | 'cunmap';

// Types
DIGIT:                  [0-9];
INT:                    [0-9]+
                    |   '0'[bB][0-1]+
                    |   '0'[oO][0-7]+
                    |   '0'[xX][0-9a-fA-F]+;
FLOAT:                  [0-9]+'.'[0-9]+
                    |   [0-9]+'.'[0-9]+'e'[+-]?[0-9]+
;
BLOB:                   '0'[zZ][0-9a-fA-F]+;
QUOTE:                  '"';
SINGLE_QUOTE:           '\'';
ESCAPED_SINGLE_QUOTE:   '\'\'';
ESCAPED_DOUBLE_QUOTE:   '\\"';

// Alphabetic relational operators - defined before identifiers to avoid being tokenized as identifiers
IS:                     'is';
IS_IC:                  'is?';
IS_CS:                  'is#';
IS_NOT:                 'isnot';
IS_NOT_IC:              'isnot?';
IS_NOT_CS:              'isnot#';

// Identifiers
IDENTIFIER_LOWERCASE:   [a-z]+;
IDENTIFIER_ANY_CASE:    [a-zA-Z]+;

// Unary operators
BANG:                   '!';

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
BAR:                    '|';
ETC:                    '...';
ARROW:                  '->';

// Mixed operators
PLUS:                   '+';
MINUS:                  '-';

// Binary operators
STAR:                   '*';
DIV:                    '/';
MOD:                    '%';
DOT:                    '.';

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


// Assignment operators
ASSIGN:                 '=';
//PLUS_ASSIGN:            '+=';
//MINUS_ASSIGN:           '-=';
//STAR_ASSIGN:            '*=';
//DIV_ASSIGN:             '/=';
//MOD_ASSIGN:             '%=';
//DOT_ASSIGN:             '.=';

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
NEW_LINE:               '\n' | '\r\n';
WS:                     [ \t]+;
INLINE_SEPARATOR:       '\n' (' ' | '\t')* BACKSLASH -> skip;
LUA_CODE:               'lua' WS* '<<' WS* 'EOF' .*? 'EOF' -> skip;
LUA_CODE2:              'lua' WS* '<<' WS* 'END' .*? 'END' -> skip;
IDEAVIM_IGNORE:         ('ideavim' | 'ideaVim' | 'IdeaVim') WS 'ignore' .*? ('ideavim' | 'ideaVim' | 'IdeaVim') WS 'ignore end' NEW_LINE -> skip;
AUGROUP_SKIP:           NEW_LINE (WS|COLON)* AUGROUP .*? AUGROUP WS+ END -> skip;

// All the other symbols
UNICODE_CHAR:           '\u0000'..'\uFFFE';
EMOJI:                  [\p{Emoji}];
