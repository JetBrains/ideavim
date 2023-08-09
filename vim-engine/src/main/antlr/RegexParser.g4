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
branch : (concats+=concat AND)* concats+=concat
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
multi : STAR     #ZeroOrMore
      | PLUS     #OneOrMore
      | OPTIONAL #ZeroOrOne
      | range    #RangeQuantifier
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
ordinary_atom : LITERAL_CHAR #LiteralChar
              | DOT          #AnyChar
              | DOTNL        #AnyCharNL
              | zero_width   #ZeroWidth
              | char_class   #CharClass
              | collection   #Collec
              ;

/**
 * A character class matches any character that is in that class. Example:
 * \d matches any digit from 0 to 9.
 */
char_class : CLASS_IDENTIFIER   #Identifier
           | CLASS_IDENTIFIER_D #IdentifierNotDigit
           | CLASS_KEYWORD      #Keyword
           | CLASS_KEYWORD_D    #KeywordNotDigit
           | CLASS_FILENAME     #Filename
           | CLASS_FILENAME_D   #FilenameNotDigit
           | CLASS_PRINTABLE    #Printable
           | CLASS_PRINTABLE_D  #PrintableNotDigit
           | CLASS_WS           #Whitespace
           | CLASS_NOT_WS       #NotWhitespace
           | CLASS_DIGIT        #Digit
           | CLASS_NOT_DIGIT    #NotDigit
           | CLASS_HEX          #Hex
           | CLASS_NOT_HEX      #NotHex
           | CLASS_OCTAL        #Octal
           | CLASS_NOT_OCTAL    #NotOctal
           | CLASS_WORD         #Wordchar
           | CLASS_NOT_WORD     #Notwordchar
           | CLASS_HEADWORD     #Headofword
           | CLASS_NOT_HEADWORD #NotHeadOfWord
           | CLASS_ALPHA        #Alpha
           | CLASS_NOT_ALPHA    #NotAlpha
           | CLASS_LCASE        #Lcase
           | CLASS_NOT_LCASE    #NotLcase
           | CLASS_UCASE        #Ucase
           | CLASS_NOT_UCASE    #NotUcase
           | CLASS_ESC          #Esc
           | CLASS_TAB          #Tab
           | CLASS_CR           #CR
           | CLASS_BS           #BS
           | CLASS_NL           #NL
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
zero_width : CURSOR      #Cursor
           | START_MATCH #StartMatch
           | END_MATCH   #EndMatch
           ;