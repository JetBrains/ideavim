lexer grammar RegexLexer;

tokens {
  ALTERNATION, AND, LEFT_PAREN, RIGHT_PAREN, LITERAL_CHAR, DOT, STAR, PLUS, OPTIONAL,
  RANGE_START, COLLECTION_START, CLASS_IDENTIFIER, CLASS_IDENTIFIER_D, CLASS_KEYWORD,
  CLASS_KEYWORD_D, CLASS_FILENAME, CLASS_FILENAME_D, CLASS_PRINTABLE, CLASS_PRINTABLE_D,
  CLASS_WS, CLASS_NOT_WS, CLASS_DIGIT, CLASS_NOT_DIGIT, CLASS_HEX, CLASS_NOT_HEX,
  CLASS_OCTAL, CLASS_NOT_OCTAL, CLASS_WORD, CLASS_NOT_WORD, CLASS_HEADWORD, CLASS_NOT_HEADWORD,
  CLASS_ALPHA, CLASS_NOT_ALPHA, CLASS_LCASE, CLASS_NOT_LCASE, CLASS_UCASE, CLASS_NOT_UCASE,
  CLASS_ESC, CLASS_TAB, CLASS_CR, CLASS_BS, CLASS_NL, COLLECTION_LITERAL_CHAR, CURSOR,
  LEFT_PAREN_NOCAPTURE, START_MATCH, END_MATCH, DOTNL, RANGE_START_LAZY
}

// ------------------------------------------------------------------------------------------------ //
//                                                                                                  //
//                                                                                                  //
// DEFAULT_MODE - This is the default lexer mode, and can be set after seeing a \m token            //
//                                                                                                  //
//                                                                                                  //
// ------------------------------------------------------------------------------------------------ //
ALTERNATION_MAGIC: '\\|' -> type(ALTERNATION);
AND_MAGIC: '\\&' -> type(AND);
LEFT_PAREN_MAGIC: '\\(' -> type(LEFT_PAREN);
LEFT_PAREN_NOCAPTURE_MAGIC: '\\%(' -> type(LEFT_PAREN_NOCAPTURE);
RIGHT_PAREN_MAGIC: '\\)' -> type(RIGHT_PAREN);
DOT_MAGIC: '.' -> type(DOT);
DOTNL_MAGIC: '\\_.' -> type(DOTNL);
STAR_MAGIC: '*' -> type(STAR);
PLUS_MAGIC: '\\+' -> type(PLUS);
OPTIONAL_MAGIC: ('\\=' | '\\?') -> type(OPTIONAL);
RANGE_START_MAGIC: '\\{' -> pushMode(INSIDE_RANGE), type(RANGE_START);
RANGE_START_LAZY_MAGIC: '\\{-' -> pushMode(INSIDE_RANGE), type(RANGE_START_LAZY);
COLLECTION_START_MAGIC: '[' -> pushMode(INSIDE_COLLECTION), type(COLLECTION_START);

// zero-width tokens
CURSOR_MAGIC: '\\%#' -> type(CURSOR);
START_MATCH_MAGIC: '\\zs' -> type(START_MATCH);
END_MATCH_MAGIC: '\\ze' -> type(END_MATCH);

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
SETMAGIC_MAGIC: '\\m' -> skip; // already in magic mode
SETNOMAGIC_MAGIC: '\\M' -> mode(NO_MAGIC), skip;
SETVMAGIC_MAGIC: '\\v' -> mode(V_MAGIC), skip;
SETVNOMAGIC_MAGIC: '\\V' -> mode(V_NO_MAGIC), skip;

// everything else, either escaped or not, should be taken literally
LITERAL_CHAR_MAGIC: '\\'? . -> type(LITERAL_CHAR);

// ------------------------------------------------------------------------------------------------ //
//                                                                                                  //
//                                                                                                  //
// NO_MAGIC - This mode is set when the lexer comes across an \M token                              //
//                                                                                                  //
//                                                                                                  //
// ------------------------------------------------------------------------------------------------ //
mode NO_MAGIC;
ALTERNATION_NOMAGIC: '\\|' -> type(ALTERNATION);
AND_NOMAGIC: '\\&' -> type(AND);
LEFT_PAREN_NOMAGIC: '\\(' -> type(LEFT_PAREN);
LEFT_PAREN_NOCAPTURE_NOMAGIC: '\\%(' -> type(LEFT_PAREN_NOCAPTURE);
RIGHT_PAREN_NOMAGIC: '\\)' -> type(RIGHT_PAREN);
DOT_NOMAGIC: '\\.' -> type(DOT);
DOTNL_NOMAGIC: '\\_.' -> type(DOTNL);
STAR_NOMAGIC: '\\*' -> type(STAR);
PLUS_NOMAGIC: '\\+' -> type(PLUS);
OPTIONAL_NOMAGIC: ('\\=' | '\\?') -> type(OPTIONAL);
RANGE_START_NOMAGIC: '\\{' -> pushMode(INSIDE_RANGE), type(RANGE_START);
RANGE_START_LAZY_NOMAGIC: '\\{-' -> pushMode(INSIDE_RANGE), type(RANGE_START_LAZY);
COLLECTION_START_NOMAGIC: '\\[' -> pushMode(INSIDE_COLLECTION), type(COLLECTION_START);

// zero-width tokens
CURSOR_NOMAGIC: '\\%#' -> type(CURSOR);
START_MATCH_NOMAGIC: '\\zs' -> type(START_MATCH);
END_MATCH_NOMAGIC: '\\ze' -> type(END_MATCH);

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
SETVMAGIC_NOMAGIC: '\\v' -> mode(V_MAGIC), skip;
SETVNOMAGIC_NOMAGIC: '\\V' -> mode(V_NO_MAGIC), skip;

// everything else, either escaped or not, should be taken literally
LITERAL_CHAR_NOMAGIC: '\\'? . -> type(LITERAL_CHAR);


// ------------------------------------------------------------------------------------------------ //
//                                                                                                  //
//                                                                                                  //
// V_MAGIC - This mode is set when the lexer comes across an \v token                               //
//                                                                                                  //
//                                                                                                  //
// ------------------------------------------------------------------------------------------------ //
mode V_MAGIC;
ALTERNATION_VMAGIC: '|' -> type(ALTERNATION);
AND_VMAGIC: '&' -> type(AND);
LEFT_PAREN_VMAGIC: '(' -> type(LEFT_PAREN);
LEFT_PAREN_NOCAPTURE_VMAGIC: '%(' -> type(LEFT_PAREN_NOCAPTURE);
RIGHT_PAREN_VMAGIC: ')' -> type(RIGHT_PAREN);
DOT_VMAGIC: '.' -> type(DOT);
DOTNL_VMAGIC: '\\_.' -> type(DOTNL);
STAR_VMAGIC: '*' -> type(STAR);
PLUS_VMAGIC: '+' -> type(PLUS);
OPTIONAL_VMAGIC: ('=' | '?') -> type(OPTIONAL);
RANGE_START_VMAGIC: '{' -> pushMode(INSIDE_RANGE), type(RANGE_START);
RANGE_START_LAZY_VMAGIC: '{-' -> pushMode(INSIDE_RANGE), type(RANGE_START_LAZY);
COLLECTION_START_VMAGIC: '[' -> pushMode(INSIDE_COLLECTION), type(COLLECTION_START);

// zero-width tokens
CURSOR_VMAGIC: '%#' -> type(CURSOR);
START_MATCH_VMAGIC: '\\zs' -> type(START_MATCH);
END_MATCH_VMAGIC: '\\ze' -> type(END_MATCH);

// character classes
CLASS_IDENTIFIER_VMAGIC: '\\i' -> type(CLASS_IDENTIFIER);
CLASS_IDENTIFIER_D_VMAGIC: '\\I' -> type(CLASS_IDENTIFIER_D);
CLASS_KEYWORD_VMAGIC: '\\k' -> type(CLASS_KEYWORD);
CLASS_KEYWORD_D_VMAGIC: '\\K' -> type(CLASS_KEYWORD_D);
CLASS_FILENAME_VMAGIC: '\\f' -> type(CLASS_FILENAME);
CLASS_FILENAME_D_VMAGIC: '\\F' -> type(CLASS_FILENAME_D);
CLASS_PRINTABLE_VMAGIC: '\\p' -> type(CLASS_PRINTABLE);
CLASS_PRINTABLE_D_VMAGIC: '\\P' -> type(CLASS_PRINTABLE_D);
CLASS_WS_VMAGIC: '\\s' -> type(CLASS_WS);
CLASS_NOT_WS_VMAGIC: '\\S' -> type(CLASS_NOT_WS);
CLASS_DIGIT_VMAGIC: '\\d' -> type(CLASS_DIGIT);
CLASS_NOT_DIGIT_VMAGIC: '\\D' -> type(CLASS_NOT_DIGIT);
CLASS_HEX_VMAGIC: '\\x' -> type(CLASS_HEX);
CLASS_NOT_HEX_VMAGIC: '\\X' -> type(CLASS_NOT_HEX);
CLASS_OCTAL_VMAGIC: '\\o' -> type(CLASS_OCTAL);
CLASS_NOT_OCTAL_VMAGIC: '\\O' -> type(CLASS_NOT_OCTAL);
CLASS_WORD_VMAGIC: '\\w' -> type(CLASS_WORD);
CLASS_NOT_WORD_VMAGIC: '\\W' -> type(CLASS_NOT_WORD);
CLASS_HEADWORD_VMAGIC: '\\h' -> type(CLASS_HEADWORD);
CLASS_NOT_HEADWORD_VMAGIC: '\\H' -> type(CLASS_NOT_HEADWORD);
CLASS_ALPHA_VMAGIC: '\\a' -> type(CLASS_ALPHA);
CLASS_NOT_ALPHA_VMAGIC: '\\A' -> type(CLASS_NOT_ALPHA);
CLASS_LCASE_VMAGIC: '\\l' -> type(CLASS_LCASE);
CLASS_NOT_LCASE_VMAGIC: '\\L' -> type(CLASS_NOT_LCASE);
CLASS_UCASE_VMAGIC: '\\u' -> type(CLASS_UCASE);
CLASS_NOT_UCASE_VMAGIC: '\\U' -> type(CLASS_NOT_UCASE);
CLASS_ESC_VMAGIC: '\\e' -> type(CLASS_ESC);
CLASS_TAB_VMAGIC: '\\t' -> type(CLASS_TAB);
CLASS_CR_VMAGIC: '\\r' -> type(CLASS_CR);
CLASS_BS_VMAGIC: '\\b' -> type(CLASS_BS);
CLASS_NL_VMAGIC: '\\n' -> type(CLASS_NL);

// tokens related to changing lexer mode. These are only used by the lexer and not sent to the parser
SETMAGIC_VMAGIC: '\\m' -> mode(DEFAULT_MODE), skip;
SETNOMAGIC_VMAGIC: '\\M' -> mode(NO_MAGIC), skip;
SETVMAGIC_VMAGIC: '\\v' -> skip; // already in very magic mode
SETVNOMAGIC_VMAGIC: '\\V' -> mode(V_NO_MAGIC), skip;

// everything else, either escaped or not, should be taken literally
LITERAL_CHAR_VMAGIC: '\\'? . -> type(LITERAL_CHAR);

// ------------------------------------------------------------------------------------------------ //
//                                                                                                  //
//                                                                                                  //
// V_NO_MAGIC - This mode is set when the lexer comes across an \V token                            //
//                                                                                                  //
//                                                                                                  //
// ------------------------------------------------------------------------------------------------ //
mode V_NO_MAGIC;
ALTERNATION_VNOMAGIC: '\\|' -> type(ALTERNATION);
AND_VNOMAGIC: '\\&' -> type(AND);
LEFT_PAREN_VNOMAGIC: '\\(' -> type(LEFT_PAREN);
LEFT_PAREN_NOCAPTURE_VNOMAGIC: '\\%(' -> type(LEFT_PAREN_NOCAPTURE);
RIGHT_PAREN_VNOMAGIC: '\\)' -> type(RIGHT_PAREN);
DOT_VNOMAGIC: '\\.' -> type(DOT);
DOTNL_VNOMAGIC: '\\_.' -> type(DOTNL);
STAR_VNOMAGIC: '\\*' -> type(STAR);
PLUS_VNOMAGIC: '\\+' -> type(PLUS);
OPTIONAL_VNOMAGIC: ('\\=' | '\\?') -> type(OPTIONAL);
RANGE_START_VNOMAGIC: '\\{' -> pushMode(INSIDE_RANGE), type(RANGE_START);
RANGE_START_LAZY_VNOMAGIC: '\\{-' -> pushMode(INSIDE_RANGE), type(RANGE_START_LAZY);
COLLECTION_START_VNOMAGIC: '\\[' -> pushMode(INSIDE_COLLECTION), type(COLLECTION_START);

// zero-width tokens
CURSOR_VNOMAGIC: '\\%#' -> type(CURSOR);
START_MATCH_VNOMAGIC: '\\zs' -> type(START_MATCH);
END_MATCH_VNOMAGIC: '\\ze' -> type(END_MATCH);

// character classes
CLASS_IDENTIFIER_VNOMAGIC: '\\i' -> type(CLASS_IDENTIFIER);
CLASS_IDENTIFIER_D_VNOMAGIC: '\\I' -> type(CLASS_IDENTIFIER_D);
CLASS_KEYWORD_VNOMAGIC: '\\k' -> type(CLASS_KEYWORD);
CLASS_KEYWORD_D_VNOMAGIC: '\\K' -> type(CLASS_KEYWORD_D);
CLASS_FILENAME_VNOMAGIC: '\\f' -> type(CLASS_FILENAME);
CLASS_FILENAME_D_VNOMAGIC: '\\F' -> type(CLASS_FILENAME_D);
CLASS_PRINTABLE_VNOMAGIC: '\\p' -> type(CLASS_PRINTABLE);
CLASS_PRINTABLE_D_VNOMAGIC: '\\P' -> type(CLASS_PRINTABLE_D);
CLASS_WS_VNOMAGIC: '\\s' -> type(CLASS_WS);
CLASS_NOT_WS_VNOMAGIC: '\\S' -> type(CLASS_NOT_WS);
CLASS_DIGIT_VNOMAGIC: '\\d' -> type(CLASS_DIGIT);
CLASS_NOT_DIGIT_VNOMAGIC: '\\D' -> type(CLASS_NOT_DIGIT);
CLASS_HEX_VNOMAGIC: '\\x' -> type(CLASS_HEX);
CLASS_NOT_HEX_VNOMAGIC: '\\X' -> type(CLASS_NOT_HEX);
CLASS_OCTAL_VNOMAGIC: '\\o' -> type(CLASS_OCTAL);
CLASS_NOT_OCTAL_VNOMAGIC: '\\O' -> type(CLASS_NOT_OCTAL);
CLASS_WORD_VNOMAGIC: '\\w' -> type(CLASS_WORD);
CLASS_NOT_WORD_VNOMAGIC: '\\W' -> type(CLASS_NOT_WORD);
CLASS_HEADWORD_VNOMAGIC: '\\h' -> type(CLASS_HEADWORD);
CLASS_NOT_HEADWORD_VNOMAGIC: '\\H' -> type(CLASS_NOT_HEADWORD);
CLASS_ALPHA_VNOMAGIC: '\\a' -> type(CLASS_ALPHA);
CLASS_NOT_ALPHA_VNOMAGIC: '\\A' -> type(CLASS_NOT_ALPHA);
CLASS_LCASE_VNOMAGIC: '\\l' -> type(CLASS_LCASE);
CLASS_NOT_LCASE_VNOMAGIC: '\\L' -> type(CLASS_NOT_LCASE);
CLASS_UCASE_VNOMAGIC: '\\u' -> type(CLASS_UCASE);
CLASS_NOT_UCASE_VNOMAGIC: '\\U' -> type(CLASS_NOT_UCASE);
CLASS_ESC_VNOMAGIC: '\\e' -> type(CLASS_ESC);
CLASS_TAB_VNOMAGIC: '\\t' -> type(CLASS_TAB);
CLASS_CR_VNOMAGIC: '\\r' -> type(CLASS_CR);
CLASS_BS_VNOMAGIC: '\\b' -> type(CLASS_BS);
CLASS_NL_VNOMAGIC: '\\n' -> type(CLASS_NL);

// tokens related to changing lexer mode. These are only used by the lexer and not sent to the parser
SETMAGIC_VNOMAGIC: '\\m' -> mode(DEFAULT_MODE), skip;
SETNOMAGIC_VNOMAGIC: '\\M' -> mode(NO_MAGIC), skip;
SETVMAGIC_VNOMAGIC: '\\v' -> mode(V_MAGIC), skip;
SETVNOMAGIC_VNOMAGIC: '\\V' -> skip; // already in very nomagic mode

// everything else, either escaped or not, should be taken literally
LITERAL_CHAR_VNOMAGIC: '\\'? . -> type(LITERAL_CHAR);

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
RANGE_END: ('}' | '\\}') -> popMode;
INT: [0-9]+;
COMMA: ',';

// Mode for when inside the collection
mode INSIDE_COLLECTION;
COLLECTION_END: ']' -> popMode;
CARET: '^';
DASH: '-';
UNICODE_ESCAPE: '\\u' [a-fA-F0-9] [a-fA-F0-9]? [a-fA-F0-9]? [a-fA-F0-9]? -> type(COLLECTION_LITERAL_CHAR);
ESCAPED_CHAR: ('\\\\' | '\\-' | '\\^' | '\\]') -> type(COLLECTION_LITERAL_CHAR);
COLLECTION_CHAR: . -> type(COLLECTION_LITERAL_CHAR);