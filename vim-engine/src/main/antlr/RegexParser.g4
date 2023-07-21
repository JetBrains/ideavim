parser grammar RegexParser;

options { tokenVocab=RegexLexer; }

pattern : (branch (ALTERNATION_MAGIC | ALTERNATION_NOMAGIC))* branch
        ;

branch : (concat (AND_MAGIC | AND_NOMAGIC))* concat
       ;

concat : piece+
       ;

piece : atom multi?
      ;

atom : ordinary_atom                                                                              #OrdinaryAtom
     | (LEFT_PAREN_MAGIC | LEFT_PAREN_NOMAGIC) pattern? (RIGHT_PAREN_MAGIC | RIGHT_PAREN_NOMAGIC) #Grouping
     ;

multi : (STAR_MAGIC | STAR_NOMAGIC)         #ZeroOrMore
      | (PLUS_MAGIC | PLUS_NOMAGIC)         #OneOrMore
      | (OPTIONAL_MAGIC | OPTIONAL_NOMAGIC) #ZeroOrOne
      | range                               #RangeQuantifier
      ;

ordinary_atom : LITERAL_CHAR_MAGIC        #LiteralChar
              | LITERAL_CHAR_NOMAGIC      #LiteralChar
              | (DOT_MAGIC | DOT_NOMAGIC) #AnyChar
              | char_class                #CharClass
              | collection                #Collec
              ;

range : (RANGE_START_MAGIC | RANGE_START_NOMAGIC) INT? (COMMA INT?)? RANGE_END
      ;

char_class : (CLASS_IDENTIFIER_MAGIC | CLASS_IDENTIFIER_NOMAGIC)     #Identifier
           | (CLASS_IDENTIFIER_D_MAGIC | CLASS_IDENTIFIER_D_NOMAGIC) #IdentifierNotDigit
           | (CLASS_KEYWORD_MAGIC | CLASS_KEYWORD_NOMAGIC)           #Keyword
           | (CLASS_KEYWORD_D_MAGIC | CLASS_KEYWORD_D_NOMAGIC)       #KeywordNotDigit
           | (CLASS_FILENAME_MAGIC | CLASS_FILENAME_NOMAGIC)         #Filename
           | (CLASS_FILENAME_D_MAGIC | CLASS_FILENAME_D_NOMAGIC)     #FilenameNotDigit
           | (CLASS_PRINTABLE_MAGIC | CLASS_PRINTABLE_NOMAGIC)       #Printable
           | (CLASS_PRINTABLE_D_MAGIC | CLASS_PRINTABLE_D_NOMAGIC)   #PrintableNotDigit
           | (CLASS_WS_MAGIC | CLASS_WS_NOMAGIC)                     #Whitespace
           | (CLASS_NOT_WS_MAGIC | CLASS_NOT_WS_NOMAGIC)             #NotWhitespace
           | (CLASS_DIGIT_MAGIC | CLASS_DIGIT_NOMAGIC)               #Digit
           | (CLASS_NOT_DIGIT_MAGIC | CLASS_NOT_DIGIT_NOMAGIC)       #NotDigit
           | (CLASS_HEX_MAGIC | CLASS_HEX_NOMAGIC)                   #Hex
           | (CLASS_NOT_HEX_MAGIC | CLASS_NOT_HEX_NOMAGIC)           #NotHex
           | (CLASS_OCTAL_MAGIC | CLASS_OCTAL_NOMAGIC)               #Octal
           | (CLASS_NOT_OCTAL_MAGIC | CLASS_NOT_OCTAL_NOMAGIC)       #NotOctal
           | (CLASS_WORD_MAGIC | CLASS_WORD_NOMAGIC)                 #Wordchar
           | (CLASS_NOT_WORD_MAGIC | CLASS_NOT_WORD_NOMAGIC)         #Notwordchar
           | (CLASS_HEADWORD_MAGIC | CLASS_HEADWORD_NOMAGIC)         #Headofword
           | (CLASS_NOT_HEADWORD_MAGIC | CLASS_NOT_HEADWORD_NOMAGIC) #NotHeadOfWord
           | (CLASS_ALPHA_MAGIC | CLASS_ALPHA_NOMAGIC)               #Alpha
           | (CLASS_NOT_ALPHA_MAGIC | CLASS_NOT_ALPHA_NOMAGIC)       #NotAlpha
           | (CLASS_LCASE_MAGIC | CLASS_LCASE_NOMAGIC)               #Lcase
           | (CLASS_NOT_LCASE_MAGIC | CLASS_NOT_LCASE_NOMAGIC)       #NotLcase
           | (CLASS_UCASE_MAGIC | CLASS_UCASE_NOMAGIC)               #Ucase
           | (CLASS_NOT_UCASE_MAGIC | CLASS_NOT_UCASE_NOMAGIC)       #NotUcase
           | (CLASS_ESC_MAGIC | CLASS_ESC_NOMAGIC)                   #Esc
           | (CLASS_TAB_MAGIC | CLASS_TAB_NOMAGIC)                   #Tab
           | (CLASS_CR_MAGIC | CLASS_CR_NOMAGIC)                     #CR
           | (CLASS_BS_MAGIC | CLASS_BS_NOMAGIC)                     #BS
           | (CLASS_NL_MAGIC | CLASS_NL_NOMAGIC)                     #NL
           ;

collection : (COLLECTION_START_MAGIC | COLLECTION_START_NOMAGIC) collection_elem* COLLECTION_END       #CollectionPos
           | (COLLECTION_START_MAGIC | COLLECTION_START_NOMAGIC) CARET collection_elem* COLLECTION_END #CollectionNeg
           ;

collection_elem : COLLECTION_CHAR                      #SingleColChar
                | COLLECTION_CHAR DASH COLLECTION_CHAR #RangeColChar
                ;