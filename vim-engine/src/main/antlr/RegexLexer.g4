lexer grammar RegexLexer;

tokens {
  ALTERNATION, AND, LEFT_PAREN, RIGHT_PAREN, LITERAL_CHAR, DOT, STAR, PLUS, OPTIONAL,
  RANGE_START, COLLECTION_START, CLASS_IDENTIFIER, CLASS_IDENTIFIER_D, CLASS_KEYWORD,
  CLASS_KEYWORD_D, CLASS_FILENAME, CLASS_FILENAME_D, CLASS_PRINTABLE, CLASS_PRINTABLE_D,
  CLASS_WS, CLASS_NOT_WS, CLASS_DIGIT, CLASS_NOT_DIGIT, CLASS_HEX, CLASS_NOT_HEX,
  CLASS_OCTAL, CLASS_NOT_OCTAL, CLASS_WORD, CLASS_NOT_WORD, CLASS_HEADWORD, CLASS_NOT_HEADWORD,
  CLASS_ALPHA, CLASS_NOT_ALPHA, CLASS_LCASE, CLASS_NOT_LCASE, CLASS_UCASE, CLASS_NOT_UCASE,
  CLASS_ESC, CLASS_TAB, CLASS_CR, CLASS_BS, CLASS_NL, COLLECTION_LITERAL_CHAR
}

// ------------------------------------------------------------------------------------------------ //
//                                                                                                  //
//                                                                                                  //
// DEFAULT_MODE - This is the default lexer mode, and can be set after seeing a \m token             //
//                                                                                                  //
//                                                                                                  //
// ------------------------------------------------------------------------------------------------ //
ALTERNATION_MAGIC: '\\|' -> type(ALTERNATION);
AND_MAGIC: '\\&' -> type(AND);
LEFT_PAREN_MAGIC: '\\(' -> type(LEFT_PAREN);
RIGHT_PAREN_MAGIC: '\\)' -> type(RIGHT_PAREN);
LITERAL_CHAR_MAGIC: ([ -#%-)+--0-9:-@A-Z\]_`a-}] | '\\\\' | '\\$'| '\\.' | '\\/' | '\\[' | '\\^' | '\\~') -> type(LITERAL_CHAR);
DOT_MAGIC: '.' -> type(DOT);
STAR_MAGIC: '*' -> type(STAR);
PLUS_MAGIC: '\\+' -> type(PLUS);
OPTIONAL_MAGIC: ('\\=' | '\\?') -> type(OPTIONAL);
RANGE_START_MAGIC: '\\{' -> pushMode(INSIDE_RANGE), type(RANGE_START);
COLLECTION_START_MAGIC: '[' -> pushMode(INSIDE_COLLECTION), type(COLLECTION_START);

// character classes
CLASS_IDENTIFIER_MAGIC: '\\i' -> type(CLASS_IDENTIFIER);
CLASS_IDENTIFIER_D_MAGIC: '\\I' -> type(CLASS_IDENTIFIER_D);
CLASS_KEYWORD_MAGIC: '\\k' -> type(CLASS_KEYWORD);
CLASS_KEYWORD_D_MAGIC: '\\K' -> type(CLASS_KEYWORD_D);
CLASS_FILENAME_MAGIC: '\\f' -> type(CLASS_FILENAME);
CLASS_FILENAME_D_MAGIC: '\\F' -> type(CLASS_FILENAME_D);
CLASS_PRINTABLE_MAGIC: '\\p' -> type(CLASS_PRINTABLE);
CLASS_PRINTABLE_D_MAGIC: '\\P' -> type(CLASS_PRINTABLE_D);
CLASS_WS_MAGIC: '\\s' -> type(CLASS_WS);
CLASS_NOT_WS_MAGIC: '\\S' -> type(CLASS_NOT_WS);
CLASS_DIGIT_MAGIC: '\\d' -> type(CLASS_DIGIT);
CLASS_NOT_DIGIT_MAGIC: '\\D' -> type(CLASS_NOT_DIGIT);
CLASS_HEX_MAGIC: '\\x' -> type(CLASS_HEX);
CLASS_NOT_HEX_MAGIC: '\\X' -> type(CLASS_NOT_HEX);
CLASS_OCTAL_MAGIC: '\\o' -> type(CLASS_OCTAL);
CLASS_NOT_OCTAL_MAGIC: '\\O' -> type(CLASS_NOT_OCTAL);
CLASS_WORD_MAGIC: '\\w' -> type(CLASS_WORD);
CLASS_NOT_WORD_MAGIC: '\\W' -> type(CLASS_NOT_WORD);
CLASS_HEADWORD_MAGIC: '\\h' -> type(CLASS_HEADWORD);
CLASS_NOT_HEADWORD_MAGIC: '\\H' -> type(CLASS_NOT_HEADWORD);
CLASS_ALPHA_MAGIC: '\\a' -> type(CLASS_ALPHA);
CLASS_NOT_ALPHA_MAGIC: '\\A' -> type(CLASS_NOT_ALPHA);
CLASS_LCASE_MAGIC: '\\l' -> type(CLASS_LCASE);
CLASS_NOT_LCASE_MAGIC: '\\L' -> type(CLASS_NOT_LCASE);
CLASS_UCASE_MAGIC: '\\u' -> type(CLASS_UCASE);
CLASS_NOT_UCASE_MAGIC: '\\U' -> type(CLASS_NOT_UCASE);
CLASS_ESC_MAGIC: '\\e' -> type(CLASS_ESC);
CLASS_TAB_MAGIC: '\\t' -> type(CLASS_TAB);
CLASS_CR_MAGIC: '\\r' -> type(CLASS_CR);
CLASS_BS_MAGIC: '\\b' -> type(CLASS_BS);
CLASS_NL_MAGIC: '\\n' -> type(CLASS_NL);

// tokens related to changing lexer mode. These are only used by the lexer and not sent to the parser
SETMAGIC_MAGIC: '\\m' -> skip;
SETNOMAGIC_MAGIC: '\\M' -> mode(NO_MAGIC), skip;
SETVMAGIC_MAGIC: '\\v' -> skip; // TODO: add very magic mode
SETVNOMAGIC_MAGIC: '\\V' -> skip; // TODO: add very nomagic mode


// ------------------------------------------------------------------------------------------------ //
//                                                                                                  //
//                                                                                                  //
// NO_MAGIC - This mode is set when the lexer comes across an \M token                               //
//                                                                                                  //
//                                                                                                  //
// ------------------------------------------------------------------------------------------------ //
mode NO_MAGIC;
ALTERNATION_NOMAGIC: '\\|' -> type(ALTERNATION);
AND_NOMAGIC: '\\&' -> type(AND);
LEFT_PAREN_NOMAGIC: '\\(' -> type(LEFT_PAREN);
RIGHT_PAREN_NOMAGIC: '\\)' -> type(RIGHT_PAREN);
LITERAL_CHAR_NOMAGIC: ([ -#%-.0-9:-@A-Z[\]_`a-~] | '\\\\' | '\\$'| '\\/' | '\\^') -> type(LITERAL_CHAR);
DOT_NOMAGIC: '\\.' -> type(DOT);
STAR_NOMAGIC: '\\*' -> type(STAR);
PLUS_NOMAGIC: '\\+' -> type(PLUS);
OPTIONAL_NOMAGIC: ('\\=' | '\\?') -> type(OPTIONAL);
RANGE_START_NOMAGIC: '\\{' -> pushMode(INSIDE_RANGE), type(RANGE_START);
COLLECTION_START_NOMAGIC: '\\[' -> pushMode(INSIDE_COLLECTION), type(COLLECTION_START);

// character classes
CLASS_IDENTIFIER_NOMAGIC: '\\i' -> type(CLASS_IDENTIFIER);
CLASS_IDENTIFIER_D_NOMAGIC: '\\I' -> type(CLASS_IDENTIFIER_D);
CLASS_KEYWORD_NOMAGIC: '\\k' -> type(CLASS_KEYWORD);
CLASS_KEYWORD_D_NOMAGIC: '\\K' -> type(CLASS_KEYWORD_D);
CLASS_FILENAME_NOMAGIC: '\\f' -> type(CLASS_FILENAME);
CLASS_FILENAME_D_NOMAGIC: '\\F' -> type(CLASS_FILENAME_D);
CLASS_PRINTABLE_NOMAGIC: '\\p' -> type(CLASS_PRINTABLE);
CLASS_PRINTABLE_D_NOMAGIC: '\\P' -> type(CLASS_PRINTABLE_D);
CLASS_WS_NOMAGIC: '\\s' -> type(CLASS_WS);
CLASS_NOT_WS_NOMAGIC: '\\S' -> type(CLASS_NOT_WS);
CLASS_DIGIT_NOMAGIC: '\\d' -> type(CLASS_DIGIT);
CLASS_NOT_DIGIT_NOMAGIC: '\\D' -> type(CLASS_NOT_DIGIT);
CLASS_HEX_NOMAGIC: '\\x' -> type(CLASS_HEX);
CLASS_NOT_HEX_NOMAGIC: '\\X' -> type(CLASS_NOT_HEX);
CLASS_OCTAL_NOMAGIC: '\\o' -> type(CLASS_OCTAL);
CLASS_NOT_OCTAL_NOMAGIC: '\\O' -> type(CLASS_NOT_OCTAL);
CLASS_WORD_NOMAGIC: '\\w' -> type(CLASS_WORD);
CLASS_NOT_WORD_NOMAGIC: '\\W' -> type(CLASS_NOT_WORD);
CLASS_HEADWORD_NOMAGIC: '\\h' -> type(CLASS_HEADWORD);
CLASS_NOT_HEADWORD_NOMAGIC: '\\H' -> type(CLASS_NOT_HEADWORD);
CLASS_ALPHA_NOMAGIC: '\\a' -> type(CLASS_ALPHA);
CLASS_NOT_ALPHA_NOMAGIC: '\\A' -> type(CLASS_NOT_ALPHA);
CLASS_LCASE_NOMAGIC: '\\l' -> type(CLASS_LCASE);
CLASS_NOT_LCASE_NOMAGIC: '\\L' -> type(CLASS_NOT_LCASE);
CLASS_UCASE_NOMAGIC: '\\u' -> type(CLASS_UCASE);
CLASS_NOT_UCASE_NOMAGIC: '\\U' -> type(CLASS_NOT_UCASE);
CLASS_ESC_NOMAGIC: '\\e' -> type(CLASS_ESC);
CLASS_TAB_NOMAGIC: '\\t' -> type(CLASS_TAB);
CLASS_CR_NOMAGIC: '\\r' -> type(CLASS_CR);
CLASS_BS_NOMAGIC: '\\b' -> type(CLASS_BS);
CLASS_NL_NOMAGIC: '\\n' -> type(CLASS_NL);

// tokens related to changing lexer mode. These are only used by the lexer and not sent to the parser
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
// Mode for when inside the range quantifier
mode INSIDE_RANGE;
RANGE_END: '}' -> popMode;
INT: [0-9]+;
COMMA: ',';

// Mode for when inside the collection
mode INSIDE_COLLECTION;
COLLECTION_END: ']' -> popMode;
CARET: '^';
DASH: '-' -> type(COLLECTION_LITERAL_CHAR);
ESCAPED_CHAR: ('\\\\' | '\\-' | '\\^' | '\\]') -> type(COLLECTION_LITERAL_CHAR);
COLLECTION_CHAR: [a-zA-Z0-9] -> type(COLLECTION_LITERAL_CHAR);