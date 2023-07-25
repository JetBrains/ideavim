lexer grammar RegexLexer;

// ------------------------------------------------------------------------------------------------ //
//                                                                                                  //
//                                                                                                  //
// DEFAULT_MODE - This is default lexer mode, and can be set after seeing a \m token                //
//                                                                                                  //
//                                                                                                  //
// ------------------------------------------------------------------------------------------------ //
ALTERNATION_MAGIC: '\\|';
AND_MAGIC: '\\&';
LEFT_PAREN_MAGIC: '\\(';
RIGHT_PAREN_MAGIC: '\\)';
LITERAL_CHAR_MAGIC: [ -#%-)+--0-9:-@A-Z\]_`a-}] | '\\\\' | '\\$'| '\\.' | '\\/' | '\\[' | '\\^' | '\\~';
DOT_MAGIC: '.';
STAR_MAGIC: '*';
PLUS_MAGIC: '\\+';
OPTIONAL_MAGIC: '\\=' | '\\?';
RANGE_START_MAGIC: '\\{' -> pushMode(INSIDE_RANGE);
COLLECTION_START_MAGIC: '[' -> pushMode(INSIDE_COLLECTION);

// character classes
CLASS_IDENTIFIER_MAGIC: '\\i';
CLASS_IDENTIFIER_D_MAGIC: '\\I';
CLASS_KEYWORD_MAGIC: '\\k';
CLASS_KEYWORD_D_MAGIC: '\\K';
CLASS_FILENAME_MAGIC: '\\f';
CLASS_FILENAME_D_MAGIC: '\\F';
CLASS_PRINTABLE_MAGIC: '\\p';
CLASS_PRINTABLE_D_MAGIC: '\\P';
CLASS_WS_MAGIC: '\\s';
CLASS_NOT_WS_MAGIC: '\\S';
CLASS_DIGIT_MAGIC: '\\d';
CLASS_NOT_DIGIT_MAGIC: '\\D';
CLASS_HEX_MAGIC: '\\x';
CLASS_NOT_HEX_MAGIC: '\\X';
CLASS_OCTAL_MAGIC: '\\o';
CLASS_NOT_OCTAL_MAGIC: '\\O';
CLASS_WORD_MAGIC: '\\w';
CLASS_NOT_WORD_MAGIC: '\\W';
CLASS_HEADWORD_MAGIC: '\\h';
CLASS_NOT_HEADWORD_MAGIC: '\\H';
CLASS_ALPHA_MAGIC: '\\a';
CLASS_NOT_ALPHA_MAGIC: '\\A';
CLASS_LCASE_MAGIC: '\\l';
CLASS_NOT_LCASE_MAGIC: '\\L';
CLASS_UCASE_MAGIC: '\\u';
CLASS_NOT_UCASE_MAGIC: '\\U';
CLASS_ESC_MAGIC: '\\e';
CLASS_TAB_MAGIC: '\\t';
CLASS_CR_MAGIC: '\\r';
CLASS_BS_MAGIC: '\\b';
CLASS_NL_MAGIC: '\\n';

// tokens related to changing lexer mode. These are only used by the lexer, and not sent to the parser
SETMAGIC_MAGIC: '\\m' -> skip; // already in magic mode
SETNOMAGIC_MAGIC: '\\M' -> mode(NO_MAGIC), skip;
SETVMAGIC_MAGIC: '\\v' -> skip; // TODO: add very magic mode
SETVNOMAGIC_MAGIC: '\\V' -> skip; // TODO: add very nomagic mode


// ------------------------------------------------------------------------------------------------ //
//                                                                                                  //
//                                                                                                  //
// NO_MAGIC - This mode is set when the lexer comes across a \M token                               //
//                                                                                                  //
//                                                                                                  //
// ------------------------------------------------------------------------------------------------ //
mode NO_MAGIC;
ALTERNATION_NOMAGIC: '\\|';
AND_NOMAGIC: '\\&';
LEFT_PAREN_NOMAGIC: '\\(';
RIGHT_PAREN_NOMAGIC: '\\)';
LITERAL_CHAR_NOMAGIC: [ -#%-.0-9:-@A-Z[\]_`a-~] | '\\\\' | '\\$'| '\\/' | '\\^';
DOT_NOMAGIC: '\\.';
STAR_NOMAGIC: '\\*';
PLUS_NOMAGIC: '\\+';
OPTIONAL_NOMAGIC: '\\=' | '\\?';
RANGE_START_NOMAGIC: '\\{' -> pushMode(INSIDE_RANGE);
COLLECTION_START_NOMAGIC: '\\[' -> pushMode(INSIDE_COLLECTION);

// character classes
CLASS_IDENTIFIER_NOMAGIC: '\\i';
CLASS_IDENTIFIER_D_NOMAGIC: '\\I';
CLASS_KEYWORD_NOMAGIC: '\\k';
CLASS_KEYWORD_D_NOMAGIC: '\\K';
CLASS_FILENAME_NOMAGIC: '\\f';
CLASS_FILENAME_D_NOMAGIC: '\\F';
CLASS_PRINTABLE_NOMAGIC: '\\p';
CLASS_PRINTABLE_D_NOMAGIC: '\\P';
CLASS_WS_NOMAGIC: '\\s';
CLASS_NOT_WS_NOMAGIC: '\\S';
CLASS_DIGIT_NOMAGIC: '\\d';
CLASS_NOT_DIGIT_NOMAGIC: '\\D';
CLASS_HEX_NOMAGIC: '\\x';
CLASS_NOT_HEX_NOMAGIC: '\\X';
CLASS_OCTAL_NOMAGIC: '\\o';
CLASS_NOT_OCTAL_NOMAGIC: '\\O';
CLASS_WORD_NOMAGIC: '\\w';
CLASS_NOT_WORD_NOMAGIC: '\\W';
CLASS_HEADWORD_NOMAGIC: '\\h';
CLASS_NOT_HEADWORD_NOMAGIC: '\\H';
CLASS_ALPHA_NOMAGIC: '\\a';
CLASS_NOT_ALPHA_NOMAGIC: '\\A';
CLASS_LCASE_NOMAGIC: '\\l';
CLASS_NOT_LCASE_NOMAGIC: '\\L';
CLASS_UCASE_NOMAGIC: '\\u';
CLASS_NOT_UCASE_NOMAGIC: '\\U';
CLASS_ESC_NOMAGIC: '\\e';
CLASS_TAB_NOMAGIC: '\\t';
CLASS_CR_NOMAGIC: '\\r';
CLASS_BS_NOMAGIC: '\\b';
CLASS_NL_NOMAGIC: '\\n';

// tokens related to changing lexer mode. These are only used by the lexer, and not sent to the parser
SETMAGIC_NOMAGIC: '\\m' -> mode(DEFAULT_MODE), skip;
SETNOMAGIC_NOMAGIC: '\\M' -> skip; // already in nomagic mode
SETVMAGIC_NOMAGIC: '\\v' -> skip; // TODO: add very magic mode
SETVNOMAGIC_NOMAGIC: '\\V' -> skip; // TODO: add very nomagic mode


// ------------------------------------------------------------------------------------------------ //
//                                                                                                  //
//                                                                                                  //
// The following lexer modes may operate on top of any of the other modes, and are used to handle   //
// tokens inside ranges and collections                                                             //
//                                                                                                  //
//                                                                                                  //
// ------------------------------------------------------------------------------------------------ //
// Mode for when inside range quantifier
mode INSIDE_RANGE;
RANGE_END: '}'-> popMode;
INT: [0-9]+;
COMMA: ',';

// Mode for when inside collection
mode INSIDE_COLLECTION;
COLLECTION_END: ']' -> popMode;
CARET: '^';
DASH: '-';
ESCAPED_CHAR: '\\\\' | '\\-' | '\\^' | '\\]';
COLLECTION_CHAR: [a-zA-Z0-9];