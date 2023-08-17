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
branch : CARET? (concats+=concat AND)* concats+=concat DOLLAR
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
 */
atom : ordinary_atom                                 #OrdinaryAtom
     | LEFT_PAREN sub_pattern? RIGHT_PAREN           #GroupingCapture
     | LEFT_PAREN_NOCAPTURE sub_pattern? RIGHT_PAREN #GroupingNoCapture
     ;

/**
 * A multi is an indication of how many times the preceding atom should be
 * matched. It can be a "*" for zero or more times, "\+" for one or more times,
 * "\?" or "\=" for zero or one times, or a range for a more customizable
 * number of times.
 */
multi : STAR               #ZeroOrMore
      | PLUS               #OneOrMore
      | OPTIONAL           #ZeroOrOne
      | range              #RangeQuantifier
      | ATOMIC             #Atomic
      | POSITIVE_LOOKAHEAD #PositiveLookahead
      | NEGATIVE_LOOKAHEAD #NegativeLookahead
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
ordinary_atom : (LITERAL_CHAR | CARET | DOLLAR)  #LiteralChar
              | DOT                              #AnyChar
              | DOTNL                            #AnyCharNL
              | BACKREFERENCE                    #Backreference
              | zero_width                       #ZeroWidth
              | char_class                       #CharClass
              | collection                       #Collec
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

collection_elem : start=(COLLECTION_LITERAL_CHAR | DASH | CARET) DASH end=(COLLECTION_LITERAL_CHAR | DASH | CARET) #RangeColElem
                | (COLLECTION_LITERAL_CHAR | DASH | CARET)                                                         #SingleColElem
                ;

/**
 * When using zero-width tokens, no characters are
 * included in the match.
 */
zero_width : CURSOR        #Cursor
           | START_MATCH   #StartMatch
           | END_MATCH     #EndMatch
           | START_OF_FILE #StartOfFile
           | END_OF_FILE   #EndOfFile
           | START_OF_LINE #StartOfLine
           | END_OF_LINE   #EndOfLine
           | START_OF_WORD #StartOfWord
           | END_OF_WORD   #EndOfWord
           ;