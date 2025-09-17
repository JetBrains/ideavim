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

    (WS | COLON)* range? (WS | COLON)* DELF (replace = BANG)? WS+ (functionScope COLON)? functionName ((inline_comment NEW_LINE+) | (NEW_LINE | BAR)+)
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
        Y_LOWERCASE | YANK_LINES | X_LOWERCASE | WRITE_QUIT | WRITE_PREVIOUS | WRITE_NEXT | W_LOWERCASE | WRITE
        | WRITE_ALL | U_LOWERCASE | UNDO | TAB_ONLY | TAB_CLOSE | SOURCE | V_SPLIT | SPLIT | SHELL | SET_HANDLER | SET | SETGLOBAL | SETLOCAL
        | SELECT_LAST_FILE | SELECT_FIRST_FILE | SELECT_FILE | AT | REDO | Q_LOWERCASE | QUIT | PUT_LINES | PROMPT_FIND
        | PROMPT_REPLACE | P_LOWERCASE | P_UPPERCASE | PRINT | PREVIOUS_TAB | N_UPPERCASE | PREVIOUS_FILE | PLUG
        | ONLY | NO_HL_SEARCH | NEXT_TAB | N_LOWERCASE | NEXT_FILE | M_LOWERCASE | MOVE_TEXT | MARKS | K_LOWERCASE
        | MARK_COMMAND | JUMPS | J_LOWERCASE | JOIN_LINES | HISTORY | GO_TO_CHAR | SYMBOL | FIND | CLASS | F_LOWERCASE
        | CLEARJUMPS
        | FILE | EXIT | E_LOWERCASE | EDIT_FILE | DUMP_LINE | DIGRAPH | DEL_MARKS | D_LOWERCASE | DEL_LINES | DELCMD
        | T_LOWERCASE | COPY | CMD_CLEAR | BUFFER_LIST | BUFFER_CLOSE | B_LOWERCASE | BUFFER | ASCII
        | ACTIONLIST | ACTION | LOCKVAR | UNLOCKVAR | PACKADD | TABMOVE
        | ASSIGN    // `:=` print last line number
      )
      bangModifier = BANG?
    WS* ((commandArgumentWithoutBars? inline_comment NEW_LINE) | (commandArgumentWithoutBars? NEW_LINE) | (commandArgumentWithoutBars? BAR)) (NEW_LINE | BAR)*
    #CommandWithComment|

    (WS | COLON)* range? (WS | COLON)*
      name = (
        MAP | MAP_CLEAR | UNMAP | SORT | REGISTERS | CMD | H_LOWERCASE | HELP | NORMAL
      )
      bangModifier = BANG?
    WS* commandArgumentWithoutBars? (NEW_LINE | BAR)+
    #CommandWithoutComments|

    (WS | COLON)* range? (WS | COLON)*
      name = (
        G_LOWERCASE | GLOBAL | V_LOWERCASE | V_GLOBAL | S_LOWERCASE | SUBSTITUTE | TILDE | AMPERSAND | BANG
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
    (WS | COLON)* range? (WS | COLON)* LET WS+ expr WS*
        assignmentOperator
        WS* expr WS* ((inline_comment NEW_LINE) | (NEW_LINE | BAR)+)
    #Let1Command|

    (WS | COLON)* range? (WS | COLON)* LET WS+ commandArgument = ~(NEW_LINE)* NEW_LINE+
    #Let2Command
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
expr:
                        expr L_BRACKET expr R_BRACKET                                                                   #OneElementSublistExpression
                    |   WS* BANG WS* expr                                                                        #UnaryExpression
                    |   expr L_BRACKET WS* from = expr? WS* COLON WS* to = expr? WS* R_BRACKET                          #SublistExpression
                    |   expr WS* binaryOperator1 WS* expr                                                               #BinExpression1
                    |   expr WS* binaryOperator2 WS* expr                                                               #BinExpression2
                    |   expr WS* binaryOperator3 WS* expr                                                               #BinExpression3
                    |   expr WS* binaryOperator4 WS* expr                                                               #BinExpression4
                    |   expr WS* binaryOperator5 WS* expr                                                               #BinExpression5
                    |   WS* unaryOperator = (PLUS | MINUS) WS* expr                                                     #UnaryExpression
                    |   expr WS* ARROW WS* functionCall                                                                 #FunctionAsMethodCall1
                    |   expr WS* ARROW WS* lambda L_PAREN WS* functionArguments WS* R_PAREN                             #FunctionAsMethodCall2
                    |   functionCall                                                                                    #FunctionCallExpression
                    |   lambda L_PAREN WS* functionArguments WS* R_PAREN                                                #LambdaFunctionCallExpression
                    |   lambda                                                                                          #LambdaExpression
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
                    |   expr WS* QUESTION QUESTION WS* expr                                                             #FalsyExpression
                    |   expr WS* QUESTION WS* expr WS* COLON WS* expr                                                   #TernaryExpression
;

binaryOperator1:        STAR | DIV | MOD;
binaryOperator2:        PLUS | MINUS | DOT | (DOT DOT);
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
                    |   RANGE
                    |   ABORT
                    |   DICT
                    |   CLOSURE
                    |   AUTOCMD
                    |   AUGROUP
                    |   existingCommands
;
existingCommands:       RETURN
                    |   LET
                    |   ECHO
                    |   DELF
                    |   ACTION
                    |   ACTIONLIST
                    |   ASCII
                    |   BUFFER
                    |   BUFFER_CLOSE
                    |   BUFFER_LIST
                    |   CMD
                    |   CMD_CLEAR
                    |   COPY
                    |   DELCMD
                    |   DEL_LINES
                    |   DEL_MARKS
                    |   DIGRAPH
                    |   DUMP_LINE
                    |   EDIT_FILE
                    |   EXIT
                    |   FILE
                    |   CLASS
                    |   FIND
                    |   SYMBOL
                    |   GLOBAL
                    |   V_GLOBAL
                    |   GO_TO_CHAR
                    |   HELP
                    |   HISTORY
                    |   JOIN_LINES
                    |   JUMPS
                    |   MARK_COMMAND
                    |   MARKS
                    |   MOVE_TEXT
                    |   NEXT_FILE
                    |   NEXT_TAB
                    |   NO_HL_SEARCH
                    |   ONLY
                    |   PACKADD
                    |   PLUG
                    |   PREVIOUS_FILE
                    |   PREVIOUS_TAB
                    |   PRINT
                    |   PROMPT_FIND
                    |   PROMPT_REPLACE
                    |   PUT_LINES
                    |   QUIT
                    |   REDO
                    |   REGISTERS
                    |   SELECT_FILE
                    |   SELECT_FIRST_FILE
                    |   SELECT_LAST_FILE
                    |   SET
                    |   SETGLOBAL
                    |   SETLOCAL
                    |   SET_HANDLER
                    |   SHELL
                    |   SORT
                    |   SPLIT
                    |   V_SPLIT
                    |   SOURCE
                    |   SUBSTITUTE
                    |   TAB_CLOSE
                    |   TAB_ONLY
                    |   UNDO
                    |   WRITE_ALL
                    |   WRITE
                    |   WRITE_NEXT
                    |   WRITE_PREVIOUS
                    |   WRITE_QUIT
                    |   YANK_LINES
                    |   MAP
                    |   MAP_CLEAR
                    |   UNMAP
                    |   EXECUTE
                    |   CALL
                    |   NORMAL
                    |   TABMOVE
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
FUNCTION:               'fu' | 'fun' | 'func' | 'funct' | 'functi' | 'functio' | 'function';
ENDFUNCTION:            'endf' | 'endfu' | 'endfun' | 'endfunc' | 'endfunct' | 'endfuncti' | 'endfunctio' | 'endfunction';
RANGE:                  'range';
ABORT:                  'abort';
DICT:                   'dict';
CLOSURE:                'closure';
FOR:                    'for';
ENDFOR:                 'endfo' | 'endfor';
IN:                     'in';
BREAK:                  'brea' | 'break';
CONTINUE:               'con' | 'cont' | 'conti' | 'contin' | 'continu' | 'continue';
FINISH:                 'fini' | 'finis' | 'finish';
RETURN:                 'return';
WHILE:                  'wh' | 'whi' | 'whil' | 'while';
ENDWHILE:               'endw' | 'endwh' | 'endwhi' | 'endwhil' |'endwhile';
IF:                     'if';
ELSE:                   'el' | 'els' | 'else';
ELSEIF:                 'elsei' | 'elseif';
ENDIF:                  'en' | 'end' | 'endi' | 'endif';
TRY:                    'try';
CATCH:                  'cat' | 'catc'| 'catch';
FINALLY:                'fina' | 'final' | 'finall' | 'finally';
ENDTRY:                 'endt' | 'endtr' | 'endtry';
THROW:                  'throw';
SID:                    '<SID>';
SNR:                    '<SNR>';
AUTOCMD:                'au' | 'aut' | 'auto' | 'autoc' | 'autocm' | 'autocmd';
AUGROUP:                'aug' | 'augr' | 'augro' | 'augrou' | 'augroup';
END:                    'END';

// Commands
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
DELCMD:                 'delc' | 'delco' | 'delcom' | 'delcomm' | 'delcomma' | 'delcomman' | 'delcommand';
DELF:                   'delf' | 'delfu' | 'delfun' | 'delfunc'| 'delfunct' | 'delfuncti' | 'delfunctio' | 'delfunction';
DEL_LINES:              'de' | 'del' | 'dele' | 'delet' | 'delete';
DEL_MARKS:              'delm' | 'delma' | 'delmar' | 'delmark' | 'delmarks';
DIGRAPH:                'dig' | 'digr' | 'digra' | 'digrap' | 'digraph' | 'digraphs';
DUMP_LINE:              'dump' | 'dumpl' | 'dumpli' | 'dumplin' | 'dumpline';
ECHO:                   'ec' | 'ech' | 'echo';
EDIT_FILE:              'bro' | 'brow' | 'brows' | 'browse' | 'ed' | 'edi' | 'edit';
EXIT:                   'wqa' | 'wqal' | 'wqall' | 'qa' | 'qal' | 'qall' | 'xa' | 'xal' | 'xall' | 'quita' | 'quital' | 'quitall';
FILE:                   'fi' | 'fil' | 'file';
FIND:                   'fin' | 'find';
GLOBAL:                 'gl' | 'glo' | 'glob' | 'globa' | 'global';
GO_TO_CHAR:             'go' | 'got' | 'goto';
HELP:                   'he' | 'hel' | 'help';
HISTORY:                'his' | 'hist' | 'histo' | 'histor' | 'history';
JOIN_LINES:             'jo' | 'joi' | 'join';
JUMPS:                  'ju' | 'jum' | 'jump' | 'jumps';
LET:                    'let';
MARKS:                  'marks';
MARK_COMMAND:           'ma' | 'mar' | 'mark';
MOVE_TEXT:              'mo' | 'mov' | 'move';
NEXT_FILE:              'ne' | 'nex' | 'next' | 'bn' | 'bne' | 'bnex' | 'bnext';
NEXT_TAB:               'tabn' | 'tabne' | 'tabnex' | 'tabnext';
NO_HL_SEARCH:           'noh' | 'nohl' | 'nohls' | 'nohlse' | 'nohlsea' | 'nohlsear' | 'nohlsearc' | 'nohlsearch';
ONLY:                   'on' | 'onl' | 'only';
PACKADD:                'pa' | 'pac' | 'pack' | 'packa' | 'packad' | 'packadd';
PLUG:                   'Plug' | 'Plugi' | 'Plugin';
PREVIOUS_FILE:          'Ne' | 'Nex' | 'Next' | 'prev'| 'previ' | 'previo' | 'previou' | 'previous' | 'bp' | 'bpr' | 'bpre' |'bprev'| 'bprevi' | 'bprevio' | 'bpreviou' | 'bprevious';
PREVIOUS_TAB:           'tabp' | 'tabpr' | 'tabpre' | 'tabprev' | 'tabprevi' | 'tabprevio' | 'tabpreviou' | 'tabprevious' | 'tabN' | 'tabNe' | 'tabNex' | 'tabNext';
PRINT:                  'pr' | 'pri' | 'prin' | 'print' | 'Pr' | 'Pri' | 'Prin' | 'Print';
PROMPT_FIND:            'pro' | 'prom' | 'promp' | 'prompt' | 'promptf' | 'promptfi' | 'promptfin' | 'promptfind';
PROMPT_REPLACE:         'promptr' | 'promptre' | 'promptrep' | 'promptrepl';
PUT_LINES:              'pu' | 'put';
QUIT:                   'qu' | 'qui' | 'quit' | 'clo' | 'clos' | 'close' | 'hid' | 'hide';
REDO:                   'red' | 'redo';
REGISTERS:              'di' | 'dis' | 'disp' | 'displ' | 'displa' | 'display' | 'reg' | 'regi' | 'regis' | 'regist' | 'registe' | 'register' | 'registers';
SYMBOL:                 'sym' | 'symb' | 'symbo' | 'symbol';
V_GLOBAL:               'vg' | 'vgl' | 'vglo' | 'vglob' | 'vgloba' | 'vglobal';
SELECT_FILE:            'argu' | 'argum' | 'argume' | 'argumen' | 'argument';
SELECT_FIRST_FILE:      'fir' | 'firs' | 'first';
SELECT_LAST_FILE:       'la' | 'las' | 'last';
SET:                    'se' | 'set';
SETGLOBAL:              'setg' | 'setgl' | 'setglo' | 'setglob' | 'setgloba' | 'setglobal';
SETLOCAL:               'setl' | 'setlo' | 'setloc' | 'setloca' | 'setlocal';
SET_HANDLER:            'sethandler';
SHELL:                  'sh' | 'she' | 'shel' | 'shell';
SORT:                   'sor' | 'sort';
SPLIT:                  'sp' | 'spl' | 'spli' | 'split';
V_SPLIT:                'vs' | 'vsp' | 'vspl' | 'vspli' | 'vsplit';
SOURCE:                 'so' | 'sou' | 'sour' | 'sourc' | 'source';
SUBSTITUTE:             'su' | 'sub' | 'subs' | 'subst' | 'substi' | 'substit' | 'substitu' | 'substitut' | 'substitute';
TAB_CLOSE:              'tabc' | 'tabcl' | 'tabclo' | 'tabclos' | 'tabclose';
TAB_ONLY:               'tabo' | 'tabon' | 'tabonl' | 'tabonly';
UNDO:                   'un' | 'und' | 'undo';
WRITE_ALL:              'wa' | 'wal' | 'wall';
WRITE:                  'wr' | 'wri' | 'writ' | 'write';
WRITE_NEXT:             'wn' | 'wne' | 'wnex' | 'wnext';
WRITE_PREVIOUS:         'wN' | 'wNe' | 'wNex' | 'wNext' | 'wp' | 'wpr' | 'wpre' | 'wprev' | 'wprevi' | 'wprevio' | 'wpreviou' | 'wprevious';
WRITE_QUIT:             'wq' | 'exi' | 'exit' | 'xi' | 'xit';
YANK_LINES:             'ya' | 'yan' | 'yank';
MAP_CLEAR:              ('mapc' | 'nmapc' | 'vmapc' | 'xmapc' | 'smapc' | 'omapc' | 'imapc' | 'cmapc')
                    |   (('mapc' | 'nmapc' | 'vmapc' | 'xmapc' | 'smapc' | 'omapc' | 'imapc' | 'cmapc') 'l')
                    |   (('mapc' | 'nmapc' | 'vmapc' | 'xmapc' | 'smapc' | 'omapc' | 'imapc' | 'cmapc') 'le')
                    |   (('mapc' | 'nmapc' | 'vmapc' | 'xmapc' | 'smapc' | 'omapc' | 'imapc' | 'cmapc') 'lea')
                    |   (('mapc' | 'nmapc' | 'vmapc' | 'xmapc' | 'smapc' | 'omapc' | 'imapc' | 'cmapc') 'lear');
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
UNMAP:                  'unm' | 'nun' | 'vu' | 'xu' | 'sunm' | 'ou' | 'iu' | 'cu'
                    |   'vun' | 'xun' | 'oun' | 'iun' | 'cun'
                    |   'nunm' | 'vunm' | 'xunm' | 'ounm' | 'iunm' | 'cunm'
                    |   'unma' | 'nunma' | 'vunma' | 'xunma' | 'sunma' | 'ounma' | 'iunma' | 'cunma'
                    |   'unmap' | 'nunmap' | 'vunmap' | 'xunmap' | 'sunmap' | 'ounmap' | 'iunmap' | 'cunmap';
EXECUTE:                'exe' | 'exec' | 'execu' | 'execut' | 'execute';
LOCKVAR:                'lockv' | 'lockva' | 'lockvar';
UNLOCKVAR:              'unlo' | 'unloc' | 'unlock' | 'unlockv' | 'unlockva' | 'unlockvar';
NORMAL:                 'norm' | 'norma' | 'normal';
TABMOVE:                'tabm' | 'tabmo' | 'tabmov' | 'tabmove';

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
