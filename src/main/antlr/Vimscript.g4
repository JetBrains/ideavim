grammar Vimscript;

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//
//  Multiline blocks related rules
//
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
script:
    executable+ EOF;

executable:
    comment | forLoop | whileLoop | functionDefinition | ifStatement | tryStatement | command;

forLoop:
    ws_cols FOR WS+ variableName WS+ IN WS+ expr WS* STATEMENT_SEPARATOR
        blockMember*
    ws_cols ENDFOR WS* STATEMENT_SEPARATOR
;
whileLoop:
    ws_cols WHILE WS+ expr WS* STATEMENT_SEPARATOR
        blockMember*
    ws_cols ENDWHILE WS* STATEMENT_SEPARATOR
;
blockMember:
    command | continueStatement | breakStatement | forLoop | whileLoop | ifStatement
|   returnStatement | throwStatement | functionDefinition | comment | tryStatement;
continueStatement:      ws_cols CONTINUE WS* STATEMENT_SEPARATOR;
breakStatement:         ws_cols BREAK WS* STATEMENT_SEPARATOR;
returnStatement:        ws_cols range? ws_cols RETURN WS+ expr WS* STATEMENT_SEPARATOR;
throwStatement:         ws_cols THROW WS+ expr WS* STATEMENT_SEPARATOR;

ifStatement:            ifBlock
                        elifBlock*
                        elseBlock?
                        ws_cols ENDIF WS* STATEMENT_SEPARATOR
;
ifBlock:                ws_cols IF WS+ expr WS* STATEMENT_SEPARATOR
                           blockMember*
;
elifBlock:              ws_cols ELSEIF WS+ expr WS* STATEMENT_SEPARATOR
                           blockMember*
;
elseBlock:              ws_cols ELSE WS* STATEMENT_SEPARATOR
                            blockMember*
;

tryStatement:           tryBlock
                        catchBlock*
                        finallyBlock?
                        ws_cols ENDTRY WS* STATEMENT_SEPARATOR
;
tryBlock:               ws_cols TRY WS* STATEMENT_SEPARATOR
                            blockMember*
;
catchBlock:             ws_cols CATCH WS+ pattern WS* STATEMENT_SEPARATOR
                            blockMember*
;
pattern:                DIV patternBody DIV;
patternBody:            .*?;
finallyBlock:           ws_cols FINALLY WS* STATEMENT_SEPARATOR
                            blockMember*
;

functionDefinition:     ws_cols FUNCTION (replace = EXCLAMATION)? WS+ (functionScope COLON)? functionName WS* L_PAREN argumentsDeclaration R_PAREN WS* STATEMENT_SEPARATOR
                            blockMember*
                        ws_cols ENDFUNCTION WS* STATEMENT_SEPARATOR
;
argumentsDeclaration:   (variableName WS* (WS* COMMA WS* variableName)*)?;



////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//
//  Command related rules:
//
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
command:
    ws_cols range ws_cols STATEMENT_SEPARATOR
    #GoToLineCommand|

    ws_cols range? ws_cols ECHO (WS+ expr)* WS* STATEMENT_SEPARATOR
    #EchoCommand|

    ws_cols range? ws_cols LET WS+ expr WS*
        assignmentOperator =  (ASSIGN | PLUS_ASSIGN | MINUS_ASSIGN | STAR_ASSIGN | DIV_ASSIGN | MOD_ASSIGN | DOT_ASSIGN)
        WS* expr WS* STATEMENT_SEPARATOR
    #LetCommand|

    ws_cols range? ws_cols DELF (replace = EXCLAMATION)? WS+ (functionScope COLON)? functionName inline_comment? STATEMENT_SEPARATOR
    #DelfunctionCommand|

    ws_cols range? ws_cols ACTION (WS* commandArgument) inline_comment? STATEMENT_SEPARATOR
    #ActionCommand|

    ws_cols range? ws_cols ACTIONLIST (WS* commandArgument) inline_comment? STATEMENT_SEPARATOR
    #ActionListCommand|

    ws_cols range? ws_cols ASCII (WS* commandArgument)? inline_comment? STATEMENT_SEPARATOR
    #AsciiCommand|

    ws_cols range? ws_cols (B_LOWERCASE | BUFFER) (WS* commandArgument)? inline_comment? STATEMENT_SEPARATOR
    #BufferCommand|

    ws_cols range? ws_cols BUFFER_CLOSE (WS* commandArgument)? inline_comment? STATEMENT_SEPARATOR
    #BufferCloseCommand|

    ws_cols range? ws_cols BUFFER_LIST (WS* commandArgument)? inline_comment? STATEMENT_SEPARATOR
    #BufferListCommand|

    ws_cols range? ws_cols CMD (WS* commandArgument)? STATEMENT_SEPARATOR
    #CmdCommand|

    ws_cols range? ws_cols EXCLAMATION (WS* commandArgument)? inline_comment? STATEMENT_SEPARATOR
    #CmdFilterCommand|

    ws_cols range? ws_cols CMD_CLEAR (WS* commandArgument)? inline_comment? STATEMENT_SEPARATOR
    #CmdClearCommand|

    ws_cols range? ws_cols (T_LOWERCASE | COPY) (WS* commandArgument)? inline_comment? STATEMENT_SEPARATOR
    #CopyTextCommand|

    ws_cols range? ws_cols DELCMD (WS* commandArgument)? inline_comment? STATEMENT_SEPARATOR
    #DelCmdCommand|

    ws_cols range? ws_cols (D_LOWERCASE | DEL_LINES) (WS* commandArgument)? inline_comment? STATEMENT_SEPARATOR
    #DeleteLinesCommand|

    ws_cols range? ws_cols DEL_MARKS (WS* commandArgument)? inline_comment? STATEMENT_SEPARATOR
    #DeleteMarksCommand|

    ws_cols range? ws_cols DIGRAPH (WS* commandArgument)? inline_comment? STATEMENT_SEPARATOR
    #DigraphCommand|

    ws_cols range? ws_cols DUMP_LINE (WS* commandArgument)? inline_comment? STATEMENT_SEPARATOR
    #DumpLineCommand|

    ws_cols range? ws_cols (E_LOWERCASE | EDIT_FILE) (WS* commandArgument)? inline_comment? STATEMENT_SEPARATOR
    #EditFileCommand|

    ws_cols range? ws_cols EXIT (WS* commandArgument)? inline_comment? STATEMENT_SEPARATOR
    #ExitCommand|

    ws_cols range? ws_cols (F_LOWERCASE | FILE) (WS* commandArgument)? inline_comment? STATEMENT_SEPARATOR
    #FileCommand|

    ws_cols range? ws_cols CLASS (WS* commandArgument)? inline_comment? STATEMENT_SEPARATOR
    #FindClassCommand|

    ws_cols range? ws_cols FIND (WS* commandArgument)? inline_comment? STATEMENT_SEPARATOR
    #FindFileCommand|

    ws_cols range? ws_cols SYMBOL (WS* commandArgument)? inline_comment? STATEMENT_SEPARATOR
    #FindSymbolCommand|

    ws_cols range? ws_cols (G_LOWERCASE | GLOBAL) (invert = EXCLAMATION)? (WS* commandArgument)? inline_comment? STATEMENT_SEPARATOR
    #GlobalCommand|

    ws_cols range? ws_cols (V_LOWERCASE | V_GLOBAL) (WS* commandArgument)? inline_comment? STATEMENT_SEPARATOR
    #VglobalCommand|

    ws_cols range? ws_cols GO_TO_CHAR (WS* commandArgument)? inline_comment? STATEMENT_SEPARATOR
    #GoToCharacterCommand|

    ws_cols range? ws_cols (H_LOWERCASE | HELP) (WS* commandArgument)? STATEMENT_SEPARATOR
    #HelpCommand|

    ws_cols range? ws_cols HISTORY (WS* commandArgument)? inline_comment? STATEMENT_SEPARATOR
    #HistoryCommand|

    ws_cols range? ws_cols (J_LOWERCASE | JOIN_LINES) (WS* commandArgument)? inline_comment? STATEMENT_SEPARATOR
    #JoinLinesCommand|

    ws_cols range? ws_cols JUMPS (WS* commandArgument)? inline_comment? STATEMENT_SEPARATOR
    #JumpsCommand|

    ws_cols range? ws_cols (K_LOWERCASE | MARK_COMMAND) (WS* commandArgument)? inline_comment? STATEMENT_SEPARATOR
    #MarkCommand|

    ws_cols range? ws_cols MARKS (WS* commandArgument)? inline_comment? STATEMENT_SEPARATOR
    #MarksCommand|

    ws_cols range? ws_cols (M_LOWERCASE | MOVE_TEXT) (WS* commandArgument)? inline_comment? STATEMENT_SEPARATOR
    #MoveTextCommand|

    ws_cols range? ws_cols (N_LOWERCASE | NEXT_FILE) (WS* commandArgument)? inline_comment? STATEMENT_SEPARATOR
    #NextFileCommand|

    ws_cols range? ws_cols NEXT_TAB (WS* commandArgument)? inline_comment? STATEMENT_SEPARATOR
    #NextTabCommand|

    ws_cols range? ws_cols NO_HL_SEARCH (WS* commandArgument)? inline_comment? STATEMENT_SEPARATOR
    #NoHlSearchCommand|

    ws_cols range? ws_cols ONLY (WS* commandArgument)? inline_comment? STATEMENT_SEPARATOR
    #OnlyCommand|

    ws_cols range? ws_cols PLUG (WS* commandArgument)? inline_comment? STATEMENT_SEPARATOR
    #PlugCommand|

    ws_cols range? ws_cols (N_UPPERCASE | PREVIOUS_FILE) (WS* commandArgument)? inline_comment? STATEMENT_SEPARATOR
    #PreviousFileCommand|

    ws_cols range? ws_cols PREVIOUS_TAB (WS* commandArgument)? inline_comment? STATEMENT_SEPARATOR
    #PreviousTabCommand|

    ws_cols range? ws_cols (P_LOWERCASE | P_UPPERCASE | PRINT) (WS* commandArgument)? inline_comment? STATEMENT_SEPARATOR
    #PrintCommand|

    ws_cols range? ws_cols PROMPT_FIND (WS* commandArgument)? inline_comment? STATEMENT_SEPARATOR
    #PromptFindCommand|

    ws_cols range? ws_cols PROMPT_REPLACE (WS* commandArgument)? inline_comment? STATEMENT_SEPARATOR
    #PromptReplaceCommand|

    ws_cols range? ws_cols PUT_LINES (WS* commandArgument)? inline_comment? STATEMENT_SEPARATOR
    #PutLinesCommand|

    ws_cols range? ws_cols (Q_LOWERCASE | QUIT) (WS* commandArgument)? inline_comment? STATEMENT_SEPARATOR
    #QuitCommand|

    ws_cols range? ws_cols REDO (WS* commandArgument)? inline_comment? STATEMENT_SEPARATOR
    #RedoCommand|

    ws_cols range? ws_cols REGISTERS (WS* commandArgument)? STATEMENT_SEPARATOR
    #RegistersCommand|

    ws_cols range? ws_cols AT (WS* commandArgument)? inline_comment? STATEMENT_SEPARATOR
    #RepeatCommand|

    ws_cols range? ws_cols SELECT_FILE (WS* commandArgument)? inline_comment? STATEMENT_SEPARATOR
    #SelectFileCommand|

    ws_cols range? ws_cols SELECT_FIRST_FILE (WS* commandArgument)? inline_comment? STATEMENT_SEPARATOR
    #SelectFirstFileCommand|

    ws_cols range? ws_cols SELECT_LAST_FILE (WS* commandArgument)? inline_comment? STATEMENT_SEPARATOR
    #SelectLastFileCommand|

    ws_cols range? ws_cols SET (WS* commandArgument)? inline_comment? STATEMENT_SEPARATOR
    #SetCommand|

    ws_cols range? ws_cols SET_HANDLER (WS* commandArgument)? inline_comment? STATEMENT_SEPARATOR
    #SetHandlerCommand|

    ws_cols range? ws_cols SHELL (WS* commandArgument)? inline_comment? STATEMENT_SEPARATOR
    #ShellCommand|

    ws_cols range? ws_cols lShift (WS* commandArgument)? inline_comment? STATEMENT_SEPARATOR
    #ShiftLeftCommand|

    ws_cols range? ws_cols rShift (WS* commandArgument)? inline_comment? STATEMENT_SEPARATOR
    #ShiftRightCommand|

    ws_cols range? ws_cols SORT (WS* commandArgument)? STATEMENT_SEPARATOR
    #SortCommand|

    ws_cols range? ws_cols SPLIT (WS* commandArgument)? inline_comment? STATEMENT_SEPARATOR
    #SplitCommand|

    ws_cols range? ws_cols V_SPLIT (WS* commandArgument)? inline_comment? STATEMENT_SEPARATOR
    #VSplitCommand|

    ws_cols range? ws_cols SOURCE (WS* commandArgument)? inline_comment? STATEMENT_SEPARATOR
    #SourceCommand|

    ws_cols range? ws_cols substituteCommandName = (S_LOWERCASE | SUBSTITUTE | TILDE | AMPERSAND) (WS* commandArgument)? inline_comment? STATEMENT_SEPARATOR
    #SubstituteCommand|

    ws_cols range? ws_cols TAB_CLOSE (WS* commandArgument)? inline_comment? STATEMENT_SEPARATOR
    #TabCloseCommand|

    ws_cols range? ws_cols TAB_ONLY (WS* commandArgument)? inline_comment? STATEMENT_SEPARATOR
    #TabOnlyCommand|

    ws_cols range? ws_cols (U_LOWERCASE | UNDO) (WS* commandArgument)? inline_comment? STATEMENT_SEPARATOR
    #UndoCommand|

    ws_cols range? ws_cols WRITE_ALL (WS* commandArgument)? inline_comment? STATEMENT_SEPARATOR
    #WriteAllCommand|

    ws_cols range? ws_cols (W_LOWERCASE | WRITE) (WS* commandArgument)? inline_comment? STATEMENT_SEPARATOR
    #WriteCommand|

    ws_cols range? ws_cols WRITE_NEXT (WS* commandArgument)? inline_comment? STATEMENT_SEPARATOR
    #WriteNextCommand|

    ws_cols range? ws_cols WRITE_PREVIOUS (WS* commandArgument)? inline_comment? STATEMENT_SEPARATOR
    #WritePreviousCommand|

    ws_cols range? ws_cols (X_LOWERCASE | WRITE_QUIT) (WS* commandArgument)? inline_comment? STATEMENT_SEPARATOR
    #WriteQuitCommand|

    ws_cols range? ws_cols (Y_LOWERCASE | YANK_LINES) (WS* commandArgument)? inline_comment? STATEMENT_SEPARATOR
    #YankLinesCommand|

    ws_cols range? ws_cols MAP (WS* commandArgument)? STATEMENT_SEPARATOR
    #MapCommand|

    ws_cols range? ws_cols MAP_CLEAR (WS* commandArgument)? STATEMENT_SEPARATOR
    #MapClearCommand|

    ws_cols range? ws_cols EXECUTE WS+ (expr WS*)* STATEMENT_SEPARATOR
    #ExecuteCommand|

    ws_cols range? ws_cols UNMAP (WS* commandArgument)? STATEMENT_SEPARATOR
    #UnmapCommand|

//  Command rule pattern:
//    ws_cols range? COMMAND_TOKEN ws_cols (WS* commandArgument)? inline_comment? STATEMENT_SEPARATOR
//    #ID|
//
    // add new rules above this one
    ws_cols range? ws_cols commandName (WS? commandArgument)? STATEMENT_SEPARATOR
    #OtherCommand
;
lShift:
    (LESS)+;
rShift:
    (GREATER)+;

commandArgument:
    ~STATEMENT_SEPARATOR*?;

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
                    |   functionCall                                                                                    #FunctionCallExpression
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
                        EMPTY_DOUBLE_QUOTED_STRING
                    |   STRING_DOUBLE_QUOTED
                    |   ESCAPED_SINGLE_QUOTE
                    |   ESCAPED_SINGLE_QUOTE ESCAPED_SINGLE_QUOTE
                    |   (SINGLE_QUOTE | (ESCAPED_SINGLE_QUOTE SINGLE_QUOTE))  ~(SINGLE_QUOTE)*? SINGLE_QUOTE
;
unsignedFloat:          FLOAT;
unsignedInt:            DIGIT | INT;
blob:                   BLOB;

mark:                   (SINGLE_QUOTE (lowercaseAlphabeticChar | uppercaseAlphabeticChar | DIGIT | LESS | GREATER | L_PAREN | R_PAREN | L_CURLY | R_CURLY | L_BRACKET | R_BRACKET | QUOTE | CARET | DOT | BACKTICK | SINGLE_QUOTE))
                    |   (BACKTICK (lowercaseAlphabeticChar | uppercaseAlphabeticChar | DIGIT | LESS | GREATER | L_PAREN | R_PAREN | L_CURLY | R_CURLY | L_BRACKET | R_BRACKET | QUOTE | CARET | DOT | BACKTICK | SINGLE_QUOTE))
;
comment:                inline_comment STATEMENT_SEPARATOR;
inline_comment:         (WS* (QUOTE | EMPTY_DOUBLE_QUOTED_STRING) ~STATEMENT_SEPARATOR*?)
                    |   (STRING_DOUBLE_QUOTED ~STATEMENT_SEPARATOR*?);
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
;

ws_cols:                 (WS | COLON)*;


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

// Keywords
FUNCTION:               'fu' | 'fun' | 'func' | 'funct' | 'functi' | 'functio' | 'function';
ENDFUNCTION:            'endf' | 'endfu' | 'endfun' | 'endfunc' | 'endfunct' | 'endfuncti' | 'endfunctio' | 'endfunction';
FOR:                    'for';
ENDFOR:                 'endfo' | 'endfor';
IN:                     'in';
BREAK:                  'brea' | 'break';
CONTINUE:               'con' | 'cont' | 'conti' | 'contin' | 'continu' | 'continue';
WHILE:                  'wh' | 'whi' | 'whil' | 'while';
ENDWHILE:               'endw' | 'endwh' | 'endwhi' | 'endwhil' |'endwhile';
IF:                     'if';
ELSE:                   'el' | 'els' | 'else';
ELSEIF:                 'elsei' | 'elseif';
ENDIF:                  'en' | 'end' | 'endi' | 'endif';
TRY:                    'try';
CATCH:                  'catch';
FINALLY:                'finally';
ENDTRY:                 'endtry';
THROW:                  'throw';

// Commands
ACTION:                 'action';
ACTIONLIST:             'actionlist';
ASCII:                  'as' | 'asc' | 'asci' | 'ascii';
BUFFER:                 'bu' | 'buf' | 'buff' | 'buffe' | 'buffer';
BUFFER_CLOSE:           'bd' | 'bde' | 'bdel' | 'bdele' | 'bdelet' | 'bdelete';
BUFFER_LIST:            'buffers' | 'ls' | 'files';
CLASS:                  'cla' | 'clas' | 'class';
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
RETURN:                 'return';
SYMBOL:                 'sym' | 'symb' | 'symbo' | 'symbol';
V_GLOBAL:               'vg' | 'vgl' | 'vglo' | 'vglob' | 'vgloba' | 'vglobal';
SELECT_FILE:            'argu' | 'argum' | 'argume' | 'argumen' | 'argument';
SELECT_FIRST_FILE:      'fir' | 'firs' | 'first';
SELECT_LAST_FILE:       'la' | 'las' | 'last';
SET:                    'set';
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
                    |   'nm' | 'vm' | 'xm' | 'om' | 'im' | 'cm'
                    |   (('nm' | 'vm' | 'xm' | 'om' | 'im' | 'cm') 'a')
                    |   (('nm' | 'vm' | 'xm' | 'om' | 'im' | 'cm') 'ap')
                    |   'no' | 'nn' | 'vn' | 'xn' | 'ono' | 'ino' | 'cno'
                    |   'nno' | 'vno' | 'xno'
                    |   (('no' | 'nno' | 'vno' | 'xno' | 'ono' | 'ino' | 'cno') 'r')
                    |   (('no' | 'nno' | 'vno' | 'xno' | 'ono' | 'ino' | 'cno') 're')
                    |   (('no' | 'nno' | 'vno' | 'xno' | 'ono' | 'ino' | 'cno') 'rem')
                    |   (('no' | 'nno' | 'vno' | 'xno' | 'ono' | 'ino' | 'cno') 'rema')
                    |   (('no' | 'nno' | 'vno' | 'xno' | 'ono' | 'ino' | 'cno') 'remap');
UNMAP:                  'unm' | 'nun' | 'vu' | 'xu' | 'sunm' | 'ou' | 'iu' | 'cu'
                    |   'vun' | 'xun' | 'oun' | 'iun' | 'cun'
                    |   'nunm' | 'vunm' | 'xunm' | 'ounm' | 'iunm' | 'cunm'
                    |   'unma' | 'nunma' | 'vunma' | 'xunma' | 'sunma' | 'ounma' | 'iunma' | 'cunma'
                    |   'unmap' | 'nunmap' | 'vunmap' | 'xunmap' | 'sunmap' | 'ounmap' | 'iunmap' | 'cunmap';
EXECUTE:                'exe' | 'exec' | 'execu' | 'execut' | 'execute';

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
QUOTE:                  '"';
SINGLE_QUOTE:           '\'';
ESCAPED_SINGLE_QUOTE:   '\'\'';
ESCAPED_DOUBLE_QUOTE:   '\\"';
STRING_DOUBLE_QUOTED:   (QUOTE (ESCAPED_DOUBLE_QUOTE | ~[\n])*? QUOTE);

// Identifiers
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
STATEMENT_SEPARATOR:    [|\n]+;
RETURN_CARET:           '\r';
WS:                     [ \t]+;
INLINE_SEPARATOR:       '\n' ' '* BACKSLASH -> skip;

// All the other symbols
UNICODE_CHAR:           '\u0000'..'\uFFFE';
