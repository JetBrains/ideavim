parser grammar RegexParser;

options { tokenVocab=RegexLexer; }

/**
 * A pattern is a sub_pattern, followed by an end-of-file.
 */
pattern : sub_pattern EOF
        ;

/**
 * A sub-pattern is one or more branches, separated by "\|".  It matches anything
 * that matches one of the branches. Example: "vim\|VIM" matches "vim" and
 * matches "VIM". If more than one branch matches, the first one is used.
 */
sub_pattern : (branches+=branch ALTERNATION)* branches+=branch
            ;
/**
 * A branch is one or more concats, separated by "\&". It matches the last
 * concat, but only if all the preceding concats also match at the same
 * position. Example: "IdeaVim\&Idea matches "Idea" in "IdeaVim".
 */
branch : CARET
       | DOLLAR
       | CARET? (concats+=concat AND)* concats+=concat DOLLAR
       | CARET? (concats+=concat AND)* concats+=concat
       ;

/**
 * A concat is one or more pieces, concatenated.  It matches a match for the
 * first piece, followed by a match for the second piece, etc. Example:
 * "i[0-9]v", first matches "i", then a digit and then "v".
 */
concat : pieces+=piece+
       ;

/**
 * A piece is an atom, possibly followed by a multi, an indication of how many
 * times the atom can be matched. Example: "a*" matches any sequence of "a"
 * characters: "", "a", "aa", etc.
 */
piece : atom multi?
      ;

/**
 * An atom is an ordinary_atom, or a sub_pattern surrounded with parenthesis.
 * If the left parenthesis is preceded by a %, it doesn't count as a
 * capture group.
 *
 * It can also be a sequence of optionally matched atoms. See :help \%[]
 */
atom : ordinary_atom                                                         #OrdinaryAtom
     | LEFT_PAREN sub_pattern? RIGHT_PAREN                                   #GroupingCapture
     | LEFT_PAREN_NOCAPTURE sub_pattern? RIGHT_PAREN                         #GroupingNoCapture
     | OPTIONALLY_MATCHED_START atoms+=atom* OPTIONALLY_MATCHED_END          #OptionallyMatched
     ;

/**
 * A multi is an indication of how many times the preceding atom should be
 * matched. It can be a "*" for zero or more times, "\+" for one or more times,
 * "\?" or "\=" for zero or one times, or a range for a more customizable
 * number of times.
 */
multi : STAR                        #ZeroOrMore
      | PLUS                        #OneOrMore
      | OPTIONAL                    #ZeroOrOne
      | range                       #RangeQuantifier
      | ATOMIC                      #Atomic
      | POSITIVE_LOOKAHEAD          #PositiveLookahead
      | NEGATIVE_LOOKAHEAD          #NegativeLookahead
      | POSITIVE_LOOKBEHIND         #PositiveLookbehind
      | NEGATIVE_LOOKBEHIND         #NegativeLookbehind
      | POSITIVE_LIMITED_LOOKBEHIND #PositiveLimitedLookbehind
      | NEGATIVE_LIMITED_LOOKBEHIND #NegativeLimitedLookbehind
      ;

/**
 * A range is a custom quantification of the number of times that the
 * preceding atom can be matched. It can be a range from a number to another,
 * up to a number, more that a number, or an absolute number of times.
 * Examples: "a\{3,5}" matches any sequence of 3 to 5 "a" characters;
 * "a\{,5}" matches any sequence of up to, and including, 5 "a" characters;
 * "a\{3,}" matches any sequence of 3 or more "a" characters;
 * "a\{4}" matches a sequence of exactly 4 "a" characters;
 * "a\{}" matches any sequence of "a" characters.
 */
range : RANGE_START lower_bound=INT? (COMMA upper_bound=INT?)? RANGE_END      #RangeGreedy
      | RANGE_START_LAZY lower_bound=INT? (COMMA upper_bound=INT?)? RANGE_END #RangeLazy
      ;

/**
 * An ordinary_atom can be a single character that matches itself, a token with
 * a special meaning, or a collection of characters.
 */
ordinary_atom : (LITERAL_CHAR | CARET | DOLLAR | OPTIONALLY_MATCHED_END) #LiteralChar
              | DOT                                                      #AnyChar
              | DOTNL                                                    #AnyCharNL
              | BACKREFERENCE                                            #Backreference
              | LAST_SUBSTITUTE                                          #LastSubstitute
              | zero_width                                               #ZeroWidth
              | char_class                                               #CharClass
              | collection                                               #Collec
              | char_code                                                #CharCode
              ;

/**
 * A character class matches any character that is in that class. Example:
 * \d matches any digit from 0 to 9.
 */
char_class : (CLASS_IDENTIFIER   | CLASS_IDENTIFIER_NL)   #Identifier
           | (CLASS_IDENTIFIER_D | CLASS_IDENTIFIER_D_NL) #IdentifierNotDigit
           | (CLASS_KEYWORD      | CLASS_KEYWORD_NL)      #Keyword
           | (CLASS_KEYWORD_D    | CLASS_KEYWORD_D_NL)    #KeywordNotDigit
           | (CLASS_FILENAME     | CLASS_FILENAME_NL)     #Filename
           | (CLASS_FILENAME_D   | CLASS_FILENAME_D_NL)   #FilenameNotDigit
           | (CLASS_PRINTABLE    | CLASS_PRINTABLE_NL)    #Printable
           | (CLASS_PRINTABLE_D  | CLASS_PRINTABLE_D_NL)  #PrintableNotDigit
           | (CLASS_WS           | CLASS_WS_NL)           #Whitespace
           | (CLASS_NOT_WS       | CLASS_NOT_WS_NL)       #NotWhitespace
           | (CLASS_DIGIT        | CLASS_DIGIT_NL)        #Digit
           | (CLASS_NOT_DIGIT    | CLASS_NOT_DIGIT_NL)    #NotDigit
           | (CLASS_HEX          | CLASS_HEX_NL)          #Hex
           | (CLASS_NOT_HEX      | CLASS_NOT_HEX_NL)      #NotHex
           | (CLASS_OCTAL        | CLASS_OCTAL_NL)        #Octal
           | (CLASS_NOT_OCTAL    | CLASS_NOT_OCTAL_NL)    #NotOctal
           | (CLASS_WORD         | CLASS_WORD_NL)         #Wordchar
           | (CLASS_NOT_WORD     | CLASS_NOT_WORD_NL)     #Notwordchar
           | (CLASS_HEADWORD     | CLASS_HEADWORD_NL)     #Headofword
           | (CLASS_NOT_HEADWORD | CLASS_NOT_HEADWORD_NL) #NotHeadOfWord
           | (CLASS_ALPHA        | CLASS_ALPHA_NL)        #Alpha
           | (CLASS_NOT_ALPHA    | CLASS_NOT_ALPHA_NL)    #NotAlpha
           | (CLASS_LCASE        | CLASS_LCASE_NL)        #Lcase
           | (CLASS_NOT_LCASE    | CLASS_NOT_LCASE_NL)    #NotLcase
           | (CLASS_UCASE        | CLASS_UCASE_NL)        #Ucase
           | (CLASS_NOT_UCASE    | CLASS_NOT_UCASE_NL)    #NotUcase
           | CLASS_ESC                                    #Esc
           | CLASS_TAB                                    #Tab
           | CLASS_CR                                     #CR
           | CLASS_BS                                     #BS
           | CLASS_NL                                     #NL
           ;

/**
 * A collection is a sequence of characters inside square brackets. It
 * matches any single caracter in the collection. If two characters in
 * the sequence are separated by '-', this is shorthand for the full list
 * of ASCII characters between them. Examples:
 * "[abc]" matches either "a", "b", or "c". Equivalent to "a\|b\|c";
 * "[0-9]" matches any digit from 0 to 9;
 * "[a-zA-Z]" matches any alphabetic character.
 */
collection : COLLECTION_START CARET collection_elems+=collection_elem* COLLECTION_END #CollectionNeg
           | COLLECTION_START collection_elems+=collection_elem* COLLECTION_END       #CollectionPos
           ;

collection_elem : collection_char_class_expression                                                                 #CharClassColElem
                | start=(COLLECTION_LITERAL_CHAR | DASH | CARET) DASH end=(COLLECTION_LITERAL_CHAR | DASH | CARET) #RangeColElem
                | (COLLECTION_LITERAL_CHAR | DASH | CARET)                                                         #SingleColElem
                ;

collection_char_class_expression : ALPHA_CLASS     #AlphaClass
                                 | ALNUM_CLASS     #AlnumClass
                                 | BLANK_CLASS     #BlankClass
                                 | CNTRL_CLASS     #CntrlClass
                                 | DIGIT_CLASS     #DigitClass
                                 | GRAPH_CLASS     #GraphClass
                                 | LOWER_CLASS     #LowerClass
                                 | PRINT_CLASS     #PrintClass
                                 | PUNCT_CLASS     #PunctClass
                                 | SPACE_CLASS     #SpaceClass
                                 | UPPER_CLASS     #UpperClass
                                 | XDIGIT_CLASS    #XdigitClass
                                 | RETURN_CLASS    #ReturnClass
                                 | TAB_CLASS       #TabClass
                                 | ESCAPE_CLASS    #EscapeClass
                                 | BACKSPACE_CLASS #BackspaceClass
                                 | IDENT_CLASS     #IdentClass
                                 | KEYWORD_CLASS   #KeywordClass
                                 | FNAME_CLASS     #FnameClass
                                 ;

/**
 * When using zero-width tokens, no characters are
 * included in the match.
 */
zero_width : CURSOR               #Cursor
           | VISUAL               #Visual
           | START_MATCH          #StartMatch
           | END_MATCH            #EndMatch
           | START_OF_FILE        #StartOfFile
           | END_OF_FILE          #EndOfFile
           | START_OF_LINE        #StartOfLine
           | END_OF_LINE          #EndOfLine
           | START_OF_WORD        #StartOfWord
           | END_OF_WORD          #EndOfWord
           | LINE                 #Line
           | BEFORE_LINE          #BeforeLine
           | AFTER_LINE           #AfterLine
           | LINE_CURSOR          #LineCursor
           | BEFORE_LINE_CURSOR   #BeforeLineCursor
           | AFTER_LINE_CURSOR    #AfterLineCursor
           | COLUMN               #Column
           | BEFORE_COLUMN        #BeforeColumn
           | AFTER_COLUMN         #AfterColumn
           | COLUMN_CURSOR        #ColumnCursor
           | BEFORE_COLUMN_CURSOR #BeforeColumnCursor
           | AFTER_COLUMN_CURSOR  #AfterColumnCursor
           | MARK                 #Mark
           | BEFORE_MARK          #BeforeMark
           | AFTER_MARK           #AfterMark
           ;

/**
 * Literal characters represented by their code.
 * E.g. \%d97 matches with the character 'a'
 */
char_code : DECIMAL_CODE      #DecimalCode
          | OCTAL_CODE        #OctalCode
          | HEXADECIMAL_CODE  #HexCode
          | UNICODE_CODE      #HexCode
          | WIDE_UNICODE_CODE #HexCode
          ;
