/**
* VimScript lexer.
*/

package com.maddyhome.idea.vim.lang.lexer;

import com.intellij.lexer.FlexLexer;
import com.intellij.psi.tree.IElementType;
import static com.maddyhome.idea.vim.lang.lexer.VimScriptTokenTypes.*;

%%

%class _VimScriptLexer
%implements FlexLexer
%final
%public

%unicode

%function advance
%type IElementType

InputChar = [^\r\n]
SpaceChar = [\ \t]
NewLineChar = \r|\n|\r\n

UppercaseLetter = [A-Z]
Letter = [a-zA-Z]
Digit = [0-9]

//Integers
Decimal = 0 | [-+]?[1-9]{Digit}*
Hexadecimal = 0[xX]{Digit}+
Octal = 0[0-7]+
Integer = {Decimal} | {Hexadecimal} | {Octal}

//Floating point number
Float = [-+]?(0 | [1-9]{Digit}*)\.({Digit}+)([eE][-+]?{Digit}*)?

//String
String = {InputChar}+

//Comment
Comment = {InputChar}*[^\'\"]

//Identifier
Name = [A-Za-z_][_A-Za-z0-9]*
VariableWithPrefix = [abglstvw]:{Name}
EnvironmentVariable = \${Name}
Option = &{Name}
Register = @{Name}
Identifier = {Name}

%%

// operators
//logic operators
"=="                        { return OP_EQUAL_TO; }
"!="                        { return OP_NOT_EQUAL_TO; }
">="                        { return OP_GT_EQ; }
">"                         { return OP_GT; }
"<="                        { return OP_LT_EQ; }
"<"                         { return OP_LT; }
"=~"                        { return OP_MATCHES; }
"!~"                        { return OP_NOT_MATCHES; }
"||"                        { return OP_LOGICAL_OR; }
"&&"                        { return OP_LOGICAL_AND; }

//unary operators
"+"                         { return OP_PLUS; }
"-"                         { return OP_MINUS; }
"*"                         { return OP_MULT; }
"/"                         { return OP_DIV; }
"%"                         { return OP_MOD; }

"="                         { return OP_ASSIGN; }


{Float}                     { return FLOAT; }
{Integer}                   { return INTEGER; }
{VariableWithPrefix}        { return VARIABLE_WITH_PREFIX; }
{EnvironmentVariable}       { return ENVIRONMENT_VARIABLE; }
{Option}                    { return OPTION; }
{Register}                  { return REGISTER; }
{Identifier}                { return IDENTIFIER; }

\"{Comment}{NewLineChar}    { return COMMENT; }
\'{String}\'                |
\"{String}\"                { return STRING; }

// braces
"("                         { return LEFT_ROUND_BRACKET; }
")"                         { return RIGHT_ROUND_BRACKET; }
"["                         { return LEFT_SQUARE_BRACKET; }
"]"                         { return RIGHT_SQUARE_BRACKET; }
"{"                         { return LEFT_CURLY_BRACKET; }
"}"                         { return RIGHT_CURLY_BRACKET; }

// separators
":"                         { return COLON; }
"."                         { return DOT; }
"?"                         { return QUESTION_MARK; }
"!"                         { return EXCLAMATION_MARK; }

// quotes
"\'"                        { return SINGLE_QUOTE; }
"\""                        { return DOUBLE_QUOTE; }

{SpaceChar}                 { return WHITESPACE; }
{NewLineChar}               {}

<<EOF>>                     { return null; }
.                           { return BAD_CHARACTER; }
