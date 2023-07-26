parser grammar RegexParser;

options { tokenVocab=RegexLexer; }

pattern : sub_pattern EOF
        ;

sub_pattern : (branch ALTERNATION)* branch
            ;

branch : (concat AND)* concat
       ;

concat : piece+
       ;

piece : atom multi?
      ;

atom : ordinary_atom                       #OrdinaryAtom
     | LEFT_PAREN sub_pattern? RIGHT_PAREN #Grouping
     ;

multi : STAR     #ZeroOrMore
      | PLUS     #OneOrMore
      | OPTIONAL #ZeroOrOne
      | range    #RangeQuantifier
      ;

ordinary_atom : LITERAL_CHAR #LiteralChar
              | DOT          #AnyChar
              | char_class   #CharClass
              | collection   #Collec
              ;

range : RANGE_START INT? (COMMA INT?)? RANGE_END
      ;

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

collection : COLLECTION_START collection_elem* COLLECTION_END       #CollectionPos
           | COLLECTION_START CARET collection_elem* COLLECTION_END #CollectionNeg
           ;

collection_elem : COLLECTION_LITERAL_CHAR DASH COLLECTION_LITERAL_CHAR #RangeColElem
                | COLLECTION_LITERAL_CHAR                              #SingleColElem
                ;