lexer grammar RegexLexer;

tokens {
  ALTERNATION, AND, LEFT_PAREN, RIGHT_PAREN, LITERAL_CHAR, DOT, STAR, PLUS, OPTIONAL,
  RANGE_START, COLLECTION_START, CLASS_IDENTIFIER, CLASS_IDENTIFIER_D, CLASS_KEYWORD,
  CLASS_KEYWORD_D, CLASS_FILENAME, CLASS_FILENAME_D, CLASS_PRINTABLE, CLASS_PRINTABLE_D,
  CLASS_WS, CLASS_NOT_WS, CLASS_DIGIT, CLASS_NOT_DIGIT, CLASS_HEX, CLASS_NOT_HEX,
  CLASS_OCTAL, CLASS_NOT_OCTAL, CLASS_WORD, CLASS_NOT_WORD, CLASS_HEADWORD, CLASS_NOT_HEADWORD,
  CLASS_ALPHA, CLASS_NOT_ALPHA, CLASS_LCASE, CLASS_NOT_LCASE, CLASS_UCASE, CLASS_NOT_UCASE,
  CLASS_ESC, CLASS_TAB, CLASS_CR, CLASS_BS, CLASS_NL, COLLECTION_LITERAL_CHAR, CURSOR,
  LEFT_PAREN_NOCAPTURE, START_MATCH, END_MATCH, DOTNL, RANGE_START_LAZY, BACKREFERENCE,
  CLASS_IDENTIFIER_NL, CLASS_IDENTIFIER_D_NL, CLASS_KEYWORD_NL, CLASS_KEYWORD_D_NL,
  CLASS_FILENAME_NL, CLASS_FILENAME_D_NL, CLASS_PRINTABLE_NL, CLASS_PRINTABLE_D_NL,
  CLASS_WS_NL, CLASS_NOT_WS_NL, CLASS_DIGIT_NL, CLASS_NOT_DIGIT_NL, CLASS_HEX_NL,
  CLASS_NOT_HEX_NL, CLASS_OCTAL_NL, CLASS_NOT_OCTAL_NL, CLASS_WORD_NL, CLASS_NOT_WORD_NL,
  CLASS_HEADWORD_NL, CLASS_NOT_HEADWORD_NL, CLASS_ALPHA_NL, CLASS_NOT_ALPHA_NL, CLASS_LCASE_NL,
  CLASS_NOT_LCASE_NL, CLASS_UCASE_NL, CLASS_NOT_UCASE_NL, START_OF_FILE, END_OF_FILE,
  CARET, DOLLAR, START_OF_LINE, END_OF_LINE, ATOMIC, START_OF_WORD, END_OF_WORD,
  POSITIVE_LOOKAHEAD, NEGATIVE_LOOKAHEAD, POSITIVE_LOOKBEHIND, NEGATIVE_LOOKBEHIND,
  POSITIVE_LIMITED_LOOKBEHIND, NEGATIVE_LIMITED_LOOKBEHIND, LAST_SUBSTITUTE, VISUAL,
  DECIMAL_CODE, OCTAL_CODE, HEXADECIMAL_CODE, UNICODE_CODE, WIDE_UNICODE_CODE,
  LINE, BEFORE_LINE, AFTER_LINE, COLUMN, BEFORE_COLUMN, AFTER_COLUMN, LINE_CURSOR,
  BEFORE_LINE_CURSOR, AFTER_LINE_CURSOR, COLUMN_CURSOR, BEFORE_COLUMN_CURSOR,
  AFTER_COLUMN_CURSOR, OPTIONALLY_MATCHED_START, OPTIONALLY_MATCHED_END, MARK,
  BEFORE_MARK, AFTER_MARK
}

@members {
    public Boolean ignoreCase = null;

    void setIgnoreCase() { ignoreCase = true; }
    void setNoIgnoreCase() { if (ignoreCase == null) ignoreCase = false; }
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
BACKREFERENCE_MAGIC: '\\' [0-9] -> type(BACKREFERENCE);
LAST_SUBSTITUTE_MAGIC: '~' -> type(LAST_SUBSTITUTE);
DECIMAL_CODE_MAGIC: '\\%d' [0-9]+ -> type(DECIMAL_CODE);
OCTAL_CODE_MAGIC: '\\%o' [0-7] [0-7]? [0-7]? -> type(OCTAL_CODE);
HEXADECIMAL_CODE_MAGIC: '\\%x' [a-fA-F0-9] [a-fA-F0-9]? -> type(HEXADECIMAL_CODE);
UNICODE_CODE_MAGIC: '\\%u' [a-fA-F0-9] [a-fA-F0-9]? [a-fA-F0-9]? [a-fA-F0-9]? -> type(UNICODE_CODE);
WIDE_UNICODE_CODE_MAGIC: '\\%U' [a-fA-F0-9] [a-fA-F0-9]? [a-fA-F0-9]? [a-fA-F0-9]? [a-fA-F0-9]? [a-fA-F0-9]? [a-fA-F0-9]? [a-fA-F0-9]? -> type(WIDE_UNICODE_CODE);

// multi
STAR_MAGIC: '*' -> type(STAR);
PLUS_MAGIC: '\\+' -> type(PLUS);
OPTIONAL_MAGIC: ('\\=' | '\\?') -> type(OPTIONAL);
ATOMIC_MAGIC: '\\@>' -> type(ATOMIC);
POSITIVE_LOOKAHEAD_MAGIC: '\\@=' -> type(POSITIVE_LOOKAHEAD);
NEGATIVE_LOOKAHEAD_MAGIC: '\\@!' -> type(NEGATIVE_LOOKAHEAD);
POSITIVE_LOOKBEHIND_MAGIC: '\\@<=' -> type(POSITIVE_LOOKBEHIND);
NEGATIVE_LOOKBEHIND_MAGIC: '\\@<!' -> type(NEGATIVE_LOOKBEHIND);
POSITIVE_LIMITED_LOOKBEHIND_MAGIC: '\\@' [0-9]+ '<=' -> type(POSITIVE_LIMITED_LOOKBEHIND);
NEGATIVE_LIMITED_LOOKBEHIND_MAGIC: '\\@' [0-9]+ '<!' -> type(NEGATIVE_LIMITED_LOOKBEHIND);
RANGE_START_MAGIC: '\\{' -> pushMode(INSIDE_RANGE), type(RANGE_START);
RANGE_START_LAZY_MAGIC: '\\{-' -> pushMode(INSIDE_RANGE), type(RANGE_START_LAZY);

COLLECTION_START_MAGIC: '[' -> pushMode(INSIDE_COLLECTION), type(COLLECTION_START);
COLLECTION_START_EOL_MAGIC: '\\_[' -> pushMode(INSIDE_COLLECTION), type(COLLECTION_START);
OPTIONALLY_MATCHED_START_MAGIC: '\\%[' -> type(OPTIONALLY_MATCHED_START);
OPTIONALLY_MATCHED_END_MAGIC: ']' -> type(OPTIONALLY_MATCHED_END);

// zero-width tokens
CURSOR_MAGIC: '\\%#' -> type(CURSOR);
START_MATCH_MAGIC: '\\zs' -> type(START_MATCH);
END_MATCH_MAGIC: '\\ze' -> type(END_MATCH);
START_OF_FILE_MAGIC: '\\%^' -> type(START_OF_FILE);
END_OF_FILE_MAGIC: '\\%$' -> type(END_OF_FILE);
START_OF_LINE_MAGIC: '\\_^' -> type(START_OF_LINE);
END_OF_LINE_MAGIC: '\\_$' -> type(END_OF_LINE);
CARET_MAGIC: '^' -> type(CARET);
DOLLAR_MAGIC: '$' -> type(DOLLAR);
START_OF_WORD_MAGIC: '\\<' -> type(START_OF_WORD);
END_OF_WORD_MAGIC: '\\>' -> type(END_OF_WORD);
VISUAL_MAGIC: '\\%V' -> type(VISUAL);
LINE_MAGIC: '\\%' [0-9]+ 'l' -> type(LINE);
BEFORE_LINE_MAGIC: '\\%<' [0-9]+ 'l' -> type(BEFORE_LINE);
AFTER_LINE_MAGIC: '\\%>' [0-9]+ 'l' -> type(AFTER_LINE);
LINE_CURSOR_MAGIC: '\\%.l' -> type(LINE_CURSOR);
BEFORE_LINE_CURSOR_MAGIC: '\\%<.l' -> type(BEFORE_LINE_CURSOR);
AFTER_LINE_CURSOR_MAGIC: '\\%>.l' -> type(AFTER_LINE_CURSOR);
COLUMN_MAGIC: '\\%' [0-9]+ [cv] -> type(COLUMN);
BEFORE_COLUMN_MAGIC: '\\%<' [0-9]+ [cv] -> type(BEFORE_COLUMN);
AFTER_COLUMN_MAGIC: '\\%>' [0-9]+ [cv] -> type(AFTER_COLUMN);
COLUMN_CURSOR_MAGIC: '\\%.' [cv] -> type(COLUMN_CURSOR);
BEFORE_COLUMN_CURSOR_MAGIC: '\\%<.' [cv] -> type(BEFORE_COLUMN_CURSOR);
AFTER_COLUMN_CURSOR_MAGIC: '\\%>.' [cv] -> type(AFTER_COLUMN_CURSOR);
MARK_MAGIC: '\\%\'' ([a-zA-Z0-9<>'`"^.(){}] | '[' | ']') -> type(MARK);
BEFORE_MARK_MAGIC: '\\%<\'' ([a-zA-Z0-9<>'`"^.(){}] | '[' | ']') -> type(BEFORE_MARK);
AFTER_MARK_MAGIC: '\\%>\'' ([a-zA-Z0-9<>'`"^.(){}] | '[' | ']') -> type(AFTER_MARK);

// case-related tokens
IGNORE_CASE_MAGIC: '\\c' { setIgnoreCase(); } -> skip;
NO_IGNORE_CASE_MAGIC: '\\C' { setNoIgnoreCase(); } -> skip;

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

CLASS_IDENTIFIER_NL_MAGIC: '\\_i' -> type(CLASS_IDENTIFIER_NL);
CLASS_IDENTIFIER_D_NL_MAGIC: '\\_I' -> type(CLASS_IDENTIFIER_D_NL);
CLASS_KEYWORD_NL_MAGIC: '\\_k' -> type(CLASS_KEYWORD_NL);
CLASS_KEYWORD_D_NL_MAGIC: '\\_K' -> type(CLASS_KEYWORD_D_NL);
CLASS_FILENAME_NL_MAGIC: '\\_f' -> type(CLASS_FILENAME_NL);
CLASS_FILENAME_D_NL_MAGIC: '\\_F' -> type(CLASS_FILENAME_D_NL);
CLASS_PRINTABLE_NL_MAGIC: '\\_p' -> type(CLASS_PRINTABLE_NL);
CLASS_PRINTABLE_D_NL_MAGIC: '\\_P' -> type(CLASS_PRINTABLE_D_NL);
CLASS_WS_NL_MAGIC: '\\_s' -> type(CLASS_WS_NL);
CLASS_NOT_WS_NL_MAGIC: '\\_S' -> type(CLASS_NOT_WS_NL);
CLASS_DIGIT_NL_MAGIC: '\\_d' -> type(CLASS_DIGIT_NL);
CLASS_NOT_DIGIT_NL_MAGIC: '\\_D' -> type(CLASS_NOT_DIGIT_NL);
CLASS_HEX_NL_MAGIC: '\\_x' -> type(CLASS_HEX_NL);
CLASS_NOT_HEX_NL_MAGIC: '\\_X' -> type(CLASS_NOT_HEX_NL);
CLASS_OCTAL_NL_MAGIC: '\\_o' -> type(CLASS_OCTAL_NL);
CLASS_NOT_OCTAL_NL_MAGIC: '\\_O' -> type(CLASS_NOT_OCTAL_NL);
CLASS_WORD_NL_MAGIC: '\\_w' -> type(CLASS_WORD_NL);
CLASS_NOT_WORD_NL_MAGIC: '\\_W' -> type(CLASS_NOT_WORD_NL);
CLASS_HEADWORD_NL_MAGIC: '\\_h' -> type(CLASS_HEADWORD_NL);
CLASS_NOT_HEADWORD_NL_MAGIC: '\\_H' -> type(CLASS_NOT_HEADWORD_NL);
CLASS_ALPHA_NL_MAGIC: '\\_a' -> type(CLASS_ALPHA_NL);
CLASS_NOT_ALPHA_NL_MAGIC: '\\_A' -> type(CLASS_NOT_ALPHA_NL);
CLASS_LCASE_NL_MAGIC: '\\_l' -> type(CLASS_LCASE_NL);
CLASS_NOT_LCASE_NL_MAGIC: '\\_L' -> type(CLASS_NOT_LCASE_NL);
CLASS_UCASE_NL_MAGIC: '\\_u' -> type(CLASS_UCASE_NL);
CLASS_NOT_UCASE_NL_MAGIC: '\\_U' -> type(CLASS_NOT_UCASE_NL);

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
BACKREFERENCE_NOMAGIC: '\\' [0-9] -> type(BACKREFERENCE);
LAST_SUBSTITUTE_NOMAGIC: '\\~' -> type(LAST_SUBSTITUTE);
DECIMAL_CODE_NOMAGIC: '\\%d' [0-9]+ -> type(DECIMAL_CODE);
OCTAL_CODE_NOMAGIC: '\\%o' [0-7] [0-7]? [0-7]? -> type(OCTAL_CODE);
HEXADECIMAL_CODE_NOMAGIC: '\\%x' [a-fA-F0-9] [a-fA-F0-9]? -> type(HEXADECIMAL_CODE);
UNICODE_CODE_NOMAGIC: '\\%u' [a-fA-F0-9] [a-fA-F0-9]? [a-fA-F0-9]? [a-fA-F0-9]? -> type(UNICODE_CODE);
WIDE_UNICODE_CODE_NOMAGIC: '\\%U' [a-fA-F0-9] [a-fA-F0-9]? [a-fA-F0-9]? [a-fA-F0-9]? [a-fA-F0-9]? [a-fA-F0-9]? [a-fA-F0-9]? [a-fA-F0-9]? -> type(WIDE_UNICODE_CODE);

// multi
STAR_NOMAGIC: '\\*' -> type(STAR);
PLUS_NOMAGIC: '\\+' -> type(PLUS);
OPTIONAL_NOMAGIC: ('\\=' | '\\?') -> type(OPTIONAL);
ATOMIC_NOMAGIC: '\\@>' -> type(ATOMIC);
POSITIVE_LOOKAHEAD_NOMAGIC: '\\@=' -> type(POSITIVE_LOOKAHEAD);
NEGATIVE_LOOKAHEAD_NOMAGIC: '\\@!' -> type(NEGATIVE_LOOKAHEAD);
POSITIVE_LOOKBEHIND_NOMAGIC: '\\@<=' -> type(POSITIVE_LOOKBEHIND);
NEGATIVE_LOOKBEHIND_NOMAGIC: '\\@<!' -> type(NEGATIVE_LOOKBEHIND);
POSITIVE_LIMITED_LOOKBEHIND_NOMAGIC: '\\@' [0-9]+ '<=' -> type(POSITIVE_LIMITED_LOOKBEHIND);
NEGATIVE_LIMITED_LOOKBEHIND_NOMAGIC: '\\@' [0-9]+ '<!' -> type(NEGATIVE_LIMITED_LOOKBEHIND);
RANGE_START_NOMAGIC: '\\{' -> pushMode(INSIDE_RANGE), type(RANGE_START);
RANGE_START_LAZY_NOMAGIC: '\\{-' -> pushMode(INSIDE_RANGE), type(RANGE_START_LAZY);

COLLECTION_START_NOMAGIC: '\\[' -> pushMode(INSIDE_COLLECTION), type(COLLECTION_START);
COLLECTION_START_EOL_NOMAGIC: '\\_[' -> pushMode(INSIDE_COLLECTION), type(COLLECTION_START);
OPTIONALLY_MATCHED_START_NOMAGIC: '\\%[' -> type(OPTIONALLY_MATCHED_START);
OPTIONALLY_MATCHED_END_NOMAGIC: ']' -> type(OPTIONALLY_MATCHED_END);

// zero-width tokens
CURSOR_NOMAGIC: '\\%#' -> type(CURSOR);
START_MATCH_NOMAGIC: '\\zs' -> type(START_MATCH);
END_MATCH_NOMAGIC: '\\ze' -> type(END_MATCH);
START_OF_FILE_NOMAGIC: '\\%^' -> type(START_OF_FILE);
END_OF_FILE_NOMAGIC: '\\%$' -> type(END_OF_FILE);
START_OF_LINE_NOMAGIC: '\\_^' -> type(START_OF_LINE);
END_OF_LINE_NOMAGIC: '\\_$' -> type(END_OF_LINE);
CARET_NOMAGIC: '^' -> type(CARET);
DOLLAR_NOMAGIC: '$' -> type(DOLLAR);
START_OF_WORD_NOMAGIC: '\\<' -> type(START_OF_WORD);
END_OF_WORD_NOMAGIC: '\\>' -> type(END_OF_WORD);
VISUAL_NOMAGIC: '\\%V' -> type(VISUAL);
LINE_NOMAGIC: '\\%' [0-9]+ 'l' -> type(LINE);
BEFORE_LINE_NOMAGIC: '\\%<' [0-9]+ 'l' -> type(BEFORE_LINE);
AFTER_LINE_NOMAGIC: '\\%>' [0-9]+ 'l' -> type(AFTER_LINE);
LINE_CURSOR_NOMAGIC: '\\%.l' -> type(LINE_CURSOR);
BEFORE_LINE_CURSOR_NOMAGIC: '\\%<.l' -> type(BEFORE_LINE_CURSOR);
AFTER_LINE_CURSOR_NOMAGIC: '\\%>.l' -> type(AFTER_LINE_CURSOR);
COLUMN_NOMAGIC: '\\%' [0-9]+ [cv] -> type(COLUMN);
BEFORE_COLUMN_NOMAGIC: '\\%<' [0-9]+ [cv] -> type(BEFORE_COLUMN);
AFTER_COLUMN_NOMAGIC: '\\%>' [0-9]+ [cv] -> type(AFTER_COLUMN);
COLUMN_CURSOR_NOMAGIC: '\\%.' [cv] -> type(COLUMN_CURSOR);
BEFORE_COLUMN_CURSOR_NOMAGIC: '\\%<.' [cv] -> type(BEFORE_COLUMN_CURSOR);
AFTER_COLUMN_CURSOR_NOMAGIC: '\\%>.' [cv] -> type(AFTER_COLUMN_CURSOR);
MARK_NOMAGIC: '\\%\'' ([a-zA-Z0-9<>'`"^.(){}] | '[' | ']') -> type(MARK);
BEFORE_MARK_NOMAGIC: '\\%<\'' ([a-zA-Z0-9<>'`"^.(){}] | '[' | ']') -> type(BEFORE_MARK);
AFTER_MARK_NOMAGIC: '\\%>\'' ([a-zA-Z0-9<>'`"^.(){}] | '[' | ']') -> type(AFTER_MARK);

// case-related tokens
IGNORE_CASE_NOMAGIC: '\\c' { setIgnoreCase(); } -> skip;
NO_IGNORE_CASE_NOMAGIC: '\\C' { setNoIgnoreCase(); } -> skip;

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

CLASS_IDENTIFIER_NL_NOMAGIC: '\\_i' -> type(CLASS_IDENTIFIER_NL);
CLASS_IDENTIFIER_D_NL_NOMAGIC: '\\_I' -> type(CLASS_IDENTIFIER_D_NL);
CLASS_KEYWORD_NL_NOMAGIC: '\\_k' -> type(CLASS_KEYWORD_NL);
CLASS_KEYWORD_D_NL_NOMAGIC: '\\_K' -> type(CLASS_KEYWORD_D_NL);
CLASS_FILENAME_NL_NOMAGIC: '\\_f' -> type(CLASS_FILENAME_NL);
CLASS_FILENAME_D_NL_NOMAGIC: '\\_F' -> type(CLASS_FILENAME_D_NL);
CLASS_PRINTABLE_NL_NOMAGIC: '\\_p' -> type(CLASS_PRINTABLE_NL);
CLASS_PRINTABLE_D_NL_NOMAGIC: '\\_P' -> type(CLASS_PRINTABLE_D_NL);
CLASS_WS_NL_NOMAGIC: '\\_s' -> type(CLASS_WS_NL);
CLASS_NOT_WS_NL_NOMAGIC: '\\_S' -> type(CLASS_NOT_WS_NL);
CLASS_DIGIT_NL_NOMAGIC: '\\_d' -> type(CLASS_DIGIT_NL);
CLASS_NOT_DIGIT_NL_NOMAGIC: '\\_D' -> type(CLASS_NOT_DIGIT_NL);
CLASS_HEX_NL_NOMAGIC: '\\_x' -> type(CLASS_HEX_NL);
CLASS_NOT_HEX_NL_NOMAGIC: '\\_X' -> type(CLASS_NOT_HEX_NL);
CLASS_OCTAL_NL_NOMAGIC: '\\_o' -> type(CLASS_OCTAL_NL);
CLASS_NOT_OCTAL_NL_NOMAGIC: '\\_O' -> type(CLASS_NOT_OCTAL_NL);
CLASS_WORD_NL_NOMAGIC: '\\_w' -> type(CLASS_WORD_NL);
CLASS_NOT_WORD_NL_NOMAGIC: '\\_W' -> type(CLASS_NOT_WORD_NL);
CLASS_HEADWORD_NL_NOMAGIC: '\\_h' -> type(CLASS_HEADWORD_NL);
CLASS_NOT_HEADWORD_NL_NOMAGIC: '\\_H' -> type(CLASS_NOT_HEADWORD_NL);
CLASS_ALPHA_NL_NOMAGIC: '\\_a' -> type(CLASS_ALPHA_NL);
CLASS_NOT_ALPHA_NL_NOMAGIC: '\\_A' -> type(CLASS_NOT_ALPHA_NL);
CLASS_LCASE_NL_NOMAGIC: '\\_l' -> type(CLASS_LCASE_NL);
CLASS_NOT_LCASE_NL_NOMAGIC: '\\_L' -> type(CLASS_NOT_LCASE_NL);
CLASS_UCASE_NL_NOMAGIC: '\\_u' -> type(CLASS_UCASE_NL);
CLASS_NOT_UCASE_NL_NOMAGIC: '\\_U' -> type(CLASS_NOT_UCASE_NL);

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
BACKREFERENCE_VMAGIC: '\\' [0-9] -> type(BACKREFERENCE);
LAST_SUBSTITUTE_VMAGIC: '~' -> type(LAST_SUBSTITUTE);
DECIMAL_CODE_VMAGIC: '%d' [0-9]+ -> type(DECIMAL_CODE);
OCTAL_CODE_VMAGIC: '%o' [0-7] [0-7]? [0-7]? -> type(OCTAL_CODE);
HEXADECIMAL_CODE_VMAGIC: '%x' [a-fA-F0-9] [a-fA-F0-9]? -> type(HEXADECIMAL_CODE);
UNICODE_CODE_VMAGIC: '%u' [a-fA-F0-9] [a-fA-F0-9]? [a-fA-F0-9]? [a-fA-F0-9]? -> type(UNICODE_CODE);
WIDE_UNICODE_CODE_VMAGIC: '%U' [a-fA-F0-9] [a-fA-F0-9]? [a-fA-F0-9]? [a-fA-F0-9]? [a-fA-F0-9]? [a-fA-F0-9]? [a-fA-F0-9]? [a-fA-F0-9]? -> type(WIDE_UNICODE_CODE);

// multi
STAR_VMAGIC: '*' -> type(STAR);
PLUS_VMAGIC: '+' -> type(PLUS);
OPTIONAL_VMAGIC: ('=' | '?') -> type(OPTIONAL);
ATOMIC_VMAGIC: '@>' -> type(ATOMIC);
POSITIVE_LOOKAHEAD_VMAGIC: '@=' -> type(POSITIVE_LOOKAHEAD);
NEGATIVE_LOOKAHEAD_VMAGIC: '@!' -> type(NEGATIVE_LOOKAHEAD);
POSITIVE_LOOKBEHIND_VMAGIC: '@<=' -> type(POSITIVE_LOOKBEHIND);
NEGATIVE_LOOKBEHIND_VMAGIC: '@<!' -> type(NEGATIVE_LOOKBEHIND);
POSITIVE_LIMITED_LOOKBEHIND_VMAGIC: '@' [0-9]+ '<=' -> type(POSITIVE_LIMITED_LOOKBEHIND);
NEGATIVE_LIMITED_LOOKBEHIND_VMAGIC: '@' [0-9]+ '<!' -> type(NEGATIVE_LIMITED_LOOKBEHIND);
RANGE_START_VMAGIC: '{' -> pushMode(INSIDE_RANGE), type(RANGE_START);
RANGE_START_LAZY_VMAGIC: '{-' -> pushMode(INSIDE_RANGE), type(RANGE_START_LAZY);

COLLECTION_START_VMAGIC: '[' -> pushMode(INSIDE_COLLECTION), type(COLLECTION_START);
COLLECTION_START_EOL_VMAGIC: '\\_[' -> pushMode(INSIDE_COLLECTION), type(COLLECTION_START);
OPTIONALLY_MATCHED_START_VMAGIC: '%[' -> type(OPTIONALLY_MATCHED_START);
OPTIONALLY_MATCHED_END_VMAGIC: ']' -> type(OPTIONALLY_MATCHED_END);

// zero-width tokens
CURSOR_VMAGIC: '%#' -> type(CURSOR);
START_MATCH_VMAGIC: '\\zs' -> type(START_MATCH);
END_MATCH_VMAGIC: '\\ze' -> type(END_MATCH);
START_OF_FILE_VMAGIC: '%^' -> type(START_OF_FILE);
END_OF_FILE_VMAGIC: '%$' -> type(END_OF_FILE);
START_OF_LINE_VMAGIC: '\\_^' -> type(START_OF_LINE);
END_OF_LINE_VMAGIC: '\\_$' -> type(END_OF_LINE);
CARET_VMAGIC: '^' -> type(CARET);
DOLLAR_VMAGIC: '$' -> type(DOLLAR);
START_OF_WORD_VMAGIC: '<' -> type(START_OF_WORD);
END_OF_WORD_VMAGIC: '>' -> type(END_OF_WORD);
VISUAL_VMAGIC: '%V' -> type(VISUAL);
LINE_VMAGIC: '%' [0-9]+ 'l' -> type(LINE);
BEFORE_LINE_VMAGIC: '%<' [0-9]+ 'l' -> type(BEFORE_LINE);
AFTER_LINE_VMAGIC: '%>' [0-9]+ 'l' -> type(AFTER_LINE);
LINE_CURSOR_VMAGIC: '%.l' -> type(LINE_CURSOR);
BEFORE_LINE_CURSOR_VMAGIC: '%<.l' -> type(BEFORE_LINE_CURSOR);
AFTER_LINE_CURSOR_VMAGIC: '%>.l' -> type(AFTER_LINE_CURSOR);
COLUMN_VMAGIC: '%' [0-9]+ [cv] -> type(COLUMN);
BEFORE_COLUMN_VMAGIC: '%<' [0-9]+ [cv] -> type(BEFORE_COLUMN);
AFTER_COLUMN_VMAGIC: '%>' [0-9]+ [cv] -> type(AFTER_COLUMN);
COLUMN_CURSOR_VMAGIC: '%.' [cv] -> type(COLUMN_CURSOR);
BEFORE_COLUMN_CURSOR_VMAGIC: '%<.' [cv] -> type(BEFORE_COLUMN_CURSOR);
AFTER_COLUMN_CURSOR_VMAGIC: '%>.' [cv] -> type(AFTER_COLUMN_CURSOR);
MARK_VMAGIC: '%\'' ([a-zA-Z0-9<>'`"^.(){}] | '[' | ']') -> type(MARK);
BEFORE_MARK_VMAGIC: '%<\'' ([a-zA-Z0-9<>'`"^.(){}] | '[' | ']') -> type(BEFORE_MARK);
AFTER_MARK_VMAGIC: '%>\'' ([a-zA-Z0-9<>'`"^.(){}] | '[' | ']') -> type(AFTER_MARK);

// case-related tokens
IGNORE_CASE_VMAGIC: '\\c' { setIgnoreCase(); } -> skip;
NO_IGNORE_CASE_VMAGIC: '\\C' { setNoIgnoreCase(); } -> skip;

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

CLASS_IDENTIFIER_NL_VMAGIC: '\\_i' -> type(CLASS_IDENTIFIER_NL);
CLASS_IDENTIFIER_D_NL_VMAGIC: '\\_I' -> type(CLASS_IDENTIFIER_D_NL);
CLASS_KEYWORD_NL_VMAGIC: '\\_k' -> type(CLASS_KEYWORD_NL);
CLASS_KEYWORD_D_NL_VMAGIC: '\\_K' -> type(CLASS_KEYWORD_D_NL);
CLASS_FILENAME_NL_VMAGIC: '\\_f' -> type(CLASS_FILENAME_NL);
CLASS_FILENAME_D_NL_VMAGIC: '\\_F' -> type(CLASS_FILENAME_D_NL);
CLASS_PRINTABLE_NL_VMAGIC: '\\_p' -> type(CLASS_PRINTABLE_NL);
CLASS_PRINTABLE_D_NL_VMAGIC: '\\_P' -> type(CLASS_PRINTABLE_D_NL);
CLASS_WS_NL_VMAGIC: '\\_s' -> type(CLASS_WS_NL);
CLASS_NOT_WS_NL_VMAGIC: '\\_S' -> type(CLASS_NOT_WS_NL);
CLASS_DIGIT_NL_VMAGIC: '\\_d' -> type(CLASS_DIGIT_NL);
CLASS_NOT_DIGIT_NL_VMAGIC: '\\_D' -> type(CLASS_NOT_DIGIT_NL);
CLASS_HEX_NL_VMAGIC: '\\_x' -> type(CLASS_HEX_NL);
CLASS_NOT_HEX_NL_VMAGIC: '\\_X' -> type(CLASS_NOT_HEX_NL);
CLASS_OCTAL_NL_VMAGIC: '\\_o' -> type(CLASS_OCTAL_NL);
CLASS_NOT_OCTAL_NL_VMAGIC: '\\_O' -> type(CLASS_NOT_OCTAL_NL);
CLASS_WORD_NL_VMAGIC: '\\_w' -> type(CLASS_WORD_NL);
CLASS_NOT_WORD_NL_VMAGIC: '\\_W' -> type(CLASS_NOT_WORD_NL);
CLASS_HEADWORD_NL_VMAGIC: '\\_h' -> type(CLASS_HEADWORD_NL);
CLASS_NOT_HEADWORD_NL_VMAGIC: '\\_H' -> type(CLASS_NOT_HEADWORD_NL);
CLASS_ALPHA_NL_VMAGIC: '\\_a' -> type(CLASS_ALPHA_NL);
CLASS_NOT_ALPHA_NL_VMAGIC: '\\_A' -> type(CLASS_NOT_ALPHA_NL);
CLASS_LCASE_NL_VMAGIC: '\\_l' -> type(CLASS_LCASE_NL);
CLASS_NOT_LCASE_NL_VMAGIC: '\\_L' -> type(CLASS_NOT_LCASE_NL);
CLASS_UCASE_NL_VMAGIC: '\\_u' -> type(CLASS_UCASE_NL);
CLASS_NOT_UCASE_NL_VMAGIC: '\\_U' -> type(CLASS_NOT_UCASE_NL);

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
BACKREFERENCE_VNOMAGIC: '\\' [0-9] -> type(BACKREFERENCE);
LAST_SUBSTITUTE_VNOMAGIC: '\\~' -> type(LAST_SUBSTITUTE);
DECIMAL_CODE_VNOMAGIC: '\\%d' [0-9]+ -> type(DECIMAL_CODE);
OCTAL_CODE_VNOMAGIC: '\\%o' [0-7] [0-7]? [0-7]? -> type(OCTAL_CODE);
HEXADECIMAL_CODE_VNOMAGIC: '\\%x' [a-fA-F0-9] [a-fA-F0-9]? -> type(HEXADECIMAL_CODE);
UNICODE_CODE_VNOMAGIC: '\\%u' [a-fA-F0-9] [a-fA-F0-9]? [a-fA-F0-9]? [a-fA-F0-9]? -> type(UNICODE_CODE);
WIDE_UNICODE_CODE_VNOMAGIC: '\\%U' [a-fA-F0-9] [a-fA-F0-9]? [a-fA-F0-9]? [a-fA-F0-9]? [a-fA-F0-9]? [a-fA-F0-9]? [a-fA-F0-9]? [a-fA-F0-9]? -> type(WIDE_UNICODE_CODE);

// multi
STAR_VNOMAGIC: '\\*' -> type(STAR);
PLUS_VNOMAGIC: '\\+' -> type(PLUS);
OPTIONAL_VNOMAGIC: ('\\=' | '\\?') -> type(OPTIONAL);
ATOMIC_VNOMAGIC: '\\@>' -> type(ATOMIC);
POSITIVE_LOOKAHEAD_VNOMAGIC: '\\@=' -> type(POSITIVE_LOOKAHEAD);
NEGATIVE_LOOKAHEAD_VNOMAGIC: '\\@!' -> type(NEGATIVE_LOOKAHEAD);
POSITIVE_LOOKBEHIND_VNOMAGIC: '\\@<=' -> type(POSITIVE_LOOKBEHIND);
NEGATIVE_LOOKBEHIND_VNOMAGIC: '\\@<!' -> type(NEGATIVE_LOOKBEHIND);
POSITIVE_LIMITED_LOOKBEHIND_VNOMAGIC: '\\@' [0-9]+ '<=' -> type(POSITIVE_LIMITED_LOOKBEHIND);
NEGATIVE_LIMITED_LOOKBEHIND_VNOMAGIC: '\\@' [0-9]+ '<!' -> type(NEGATIVE_LIMITED_LOOKBEHIND);
RANGE_START_VNOMAGIC: '\\{' -> pushMode(INSIDE_RANGE), type(RANGE_START);
RANGE_START_LAZY_VNOMAGIC: '\\{-' -> pushMode(INSIDE_RANGE), type(RANGE_START_LAZY);

COLLECTION_START_VNOMAGIC: '\\[' -> pushMode(INSIDE_COLLECTION), type(COLLECTION_START);
COLLECTION_START_EOL_VNOMAGIC: '\\_[' -> pushMode(INSIDE_COLLECTION), type(COLLECTION_START);
OPTIONALLY_MATCHED_START_VNOMAGIC: '\\%[' -> type(OPTIONALLY_MATCHED_START);
OPTIONALLY_MATCHED_END_VNOMAGIC: ']' -> type(OPTIONALLY_MATCHED_END);

// zero-width tokens
CURSOR_VNOMAGIC: '\\%#' -> type(CURSOR);
START_MATCH_VNOMAGIC: '\\zs' -> type(START_MATCH);
END_MATCH_VNOMAGIC: '\\ze' -> type(END_MATCH);
START_OF_FILE_VNOMAGIC: '\\%^' -> type(START_OF_FILE);
END_OF_FILE_VNOMAGIC: '\\%$' -> type(END_OF_FILE);
START_OF_LINE_VNOMAGIC: '\\_^' -> type(START_OF_LINE);
END_OF_LINE_VNOMAGIC: '\\_$' -> type(END_OF_LINE);
CARET_VNOMAGIC: '\\^' -> type(CARET);
DOLLAR_VNOMAGIC: '\\$' -> type(DOLLAR);
START_OF_WORD_VNOMAGIC: '\\<' -> type(START_OF_WORD);
END_OF_WORD_VNOMAGIC: '\\>' -> type(END_OF_WORD);
VISUAL_VNOMAGIC: '\\%V' -> type(VISUAL);
LINE_VNOMAGIC: '\\%' [0-9]+ 'l' -> type(LINE);
BEFORE_LINE_VNOMAGIC: '\\%<' [0-9]+ 'l' -> type(BEFORE_LINE);
AFTER_LINE_VNOMAGIC: '\\%>' [0-9]+ 'l' -> type(AFTER_LINE);
LINE_CURSOR_VNOMAGIC: '\\%.l' -> type(LINE_CURSOR);
BEFORE_LINE_CURSOR_VNOMAGIC: '\\%<.l' -> type(BEFORE_LINE_CURSOR);
AFTER_LINE_CURSOR_VNOMAGIC: '\\%>.l' -> type(AFTER_LINE_CURSOR);
COLUMN_VNOMAGIC: '\\%' [0-9]+ [cv] -> type(COLUMN);
BEFORE_COLUMN_VNOMAGIC: '\\%<' [0-9]+ [cv] -> type(BEFORE_COLUMN);
AFTER_COLUMN_VNOMAGIC: '\\%>' [0-9]+ [cv] -> type(AFTER_COLUMN);
COLUMN_CURSOR_VNOMAGIC: '\\%.' [cv] -> type(COLUMN_CURSOR);
BEFORE_COLUMN_CURSOR_VNOMAGIC: '\\%<.' [cv] -> type(BEFORE_COLUMN_CURSOR);
AFTER_COLUMN_CURSOR_VNOMAGIC: '\\%>.' [cv] -> type(AFTER_COLUMN_CURSOR);
MARK_VNOMAGIC: '\\%\'' ([a-zA-Z0-9<>'`"^.(){}] | '[' | ']') -> type(MARK);
BEFORE_MARK_VNOMAGIC: '\\%<\'' ([a-zA-Z0-9<>'`"^.(){}] | '[' | ']') -> type(BEFORE_MARK);
AFTER_MARK_VNOMAGIC: '\\%>\'' ([a-zA-Z0-9<>'`"^.(){}] | '[' | ']') -> type(AFTER_MARK);

// case-related tokens
IGNORE_CASE_VNOMAGIC: '\\c' { setIgnoreCase(); } -> skip;
OT_IGNORE_CASE_VNOMAGIC: '\\C' { setNoIgnoreCase(); } -> skip;

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

CLASS_IDENTIFIER_NL_VNOMAGIC: '\\_i' -> type(CLASS_IDENTIFIER_NL);
CLASS_IDENTIFIER_D_NL_VNOMAGIC: '\\_I' -> type(CLASS_IDENTIFIER_D_NL);
CLASS_KEYWORD_NL_VNOMAGIC: '\\_k' -> type(CLASS_KEYWORD_NL);
CLASS_KEYWORD_D_NL_VNOMAGIC: '\\_K' -> type(CLASS_KEYWORD_D_NL);
CLASS_FILENAME_NL_VNOMAGIC: '\\_f' -> type(CLASS_FILENAME_NL);
CLASS_FILENAME_D_NL_VNOMAGIC: '\\_F' -> type(CLASS_FILENAME_D_NL);
CLASS_PRINTABLE_NL_VNOMAGIC: '\\_p' -> type(CLASS_PRINTABLE_NL);
CLASS_PRINTABLE_D_NL_VNOMAGIC: '\\_P' -> type(CLASS_PRINTABLE_D_NL);
CLASS_WS_NL_VNOMAGIC: '\\_s' -> type(CLASS_WS_NL);
CLASS_NOT_WS_NL_VNOMAGIC: '\\_S' -> type(CLASS_NOT_WS_NL);
CLASS_DIGIT_NL_VNOMAGIC: '\\_d' -> type(CLASS_DIGIT_NL);
CLASS_NOT_DIGIT_NL_VNOMAGIC: '\\_D' -> type(CLASS_NOT_DIGIT_NL);
CLASS_HEX_NL_VNOMAGIC: '\\_x' -> type(CLASS_HEX_NL);
CLASS_NOT_HEX_NL_VNOMAGIC: '\\_X' -> type(CLASS_NOT_HEX_NL);
CLASS_OCTAL_NL_VNOMAGIC: '\\_o' -> type(CLASS_OCTAL_NL);
CLASS_NOT_OCTAL_NL_VNOMAGIC: '\\_O' -> type(CLASS_NOT_OCTAL_NL);
CLASS_WORD_NL_VNOMAGIC: '\\_w' -> type(CLASS_WORD_NL);
CLASS_NOT_WORD_NL_VNOMAGIC: '\\_W' -> type(CLASS_NOT_WORD_NL);
CLASS_HEADWORD_NL_VNOMAGIC: '\\_h' -> type(CLASS_HEADWORD_NL);
CLASS_NOT_HEADWORD_NL_VNOMAGIC: '\\_H' -> type(CLASS_NOT_HEADWORD_NL);
CLASS_ALPHA_NL_VNOMAGIC: '\\_a' -> type(CLASS_ALPHA_NL);
CLASS_NOT_ALPHA_NL_VNOMAGIC: '\\_A' -> type(CLASS_NOT_ALPHA_NL);
CLASS_LCASE_NL_VNOMAGIC: '\\_l' -> type(CLASS_LCASE_NL);
CLASS_NOT_LCASE_NL_VNOMAGIC: '\\_L' -> type(CLASS_NOT_LCASE_NL);
CLASS_UCASE_NL_VNOMAGIC: '\\_u' -> type(CLASS_UCASE_NL);
CLASS_NOT_UCASE_NL_VNOMAGIC: '\\_U' -> type(CLASS_NOT_UCASE_NL);

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
DECIMAL_ESCAPE: '\\d' [0-9] [0-9]+ -> type(COLLECTION_LITERAL_CHAR);
OCTAL_ESCAPE: '\\o' [0-7] [0-7]? [0-7]? -> type(COLLECTION_LITERAL_CHAR);
HEXADECIMAL_ESCAPE: '\\x' [a-fA-F0-9] [a-fA-F0-9]? -> type(COLLECTION_LITERAL_CHAR);
UNICODE_ESCAPE: '\\u' [a-fA-F0-9] [a-fA-F0-9]? [a-fA-F0-9]? [a-fA-F0-9]? -> type(COLLECTION_LITERAL_CHAR);
UNICODE_ESCAPE_WIDE: '\\U' [a-fA-F0-9] [a-fA-F0-9]? [a-fA-F0-9]? [a-fA-F0-9]? [a-fA-F0-9]? [a-fA-F0-9]? [a-fA-F0-9]? [a-fA-F0-9]? -> type(COLLECTION_LITERAL_CHAR);
ESC_ESCAPE: '\\e' -> type(COLLECTION_LITERAL_CHAR);
TAB_ESCAPE: '\\t' -> type(COLLECTION_LITERAL_CHAR);
CR_ESCAPE: '\\r' -> type(COLLECTION_LITERAL_CHAR);
BS_ESCAPE: '\\b' -> type(COLLECTION_LITERAL_CHAR);
NL_ESCAPE: '\\n' -> type(COLLECTION_LITERAL_CHAR);
ALNUM_CLASS: '[:alnum:]';
ALPHA_CLASS: '[:alpha:]';
BLANK_CLASS: '[:blank:]';
CNTRL_CLASS: '[:cntrl:]';
DIGIT_CLASS: '[:digit:]';
GRAPH_CLASS: '[:graph:]';
LOWER_CLASS: '[:lower:]';
PRINT_CLASS: '[:print:]';
PUNCT_CLASS: '[:punct:]';
SPACE_CLASS: '[:space:]';
UPPER_CLASS: '[:upper:]';
XDIGIT_CLASS: '[:xdigit:]';
RETURN_CLASS: '[:return:]';
TAB_CLASS: '[:tab:]';
ESCAPE_CLASS: '[:escape:]';
BACKSPACE_CLASS: '[:backspace:]';
IDENT_CLASS: '[:ident:]';
KEYWORD_CLASS: '[:keyword:]';
FNAME_CLASS: '[:fname:]';
ESCAPED_CHAR: ('\\\\' | '\\-' | '\\^' | '\\]') -> type(COLLECTION_LITERAL_CHAR);
COLLECTION_CHAR: . -> type(COLLECTION_LITERAL_CHAR);