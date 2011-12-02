package com.maddyhome.idea.vim.lang.lexer;

import com.intellij.lang.ParserDefinition;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;

/**
 * <p>Date: 25.10.11</p>
 * <p>Tokens of VimScript.</p>
 *
 * @author Ksenia V. Mamich
 * @version 1.0
 */
public interface VimScriptTokenTypes {
  public static final IElementType BAD_CHARACTER = TokenType.BAD_CHARACTER;
  public static final IElementType WHITESPACE = TokenType.WHITE_SPACE;

  public static final TokenSet whitespaces = TokenSet.create(WHITESPACE);

  /* brackets */
  public static final IElementType LEFT_ROUND_BRACKET = new VimScriptElementType("(");
  public static final IElementType RIGHT_ROUND_BRACKET = new VimScriptElementType(")");
  public static final IElementType LEFT_SQUARE_BRACKET = new VimScriptElementType("[");
  public static final IElementType RIGHT_SQUARE_BRACKET = new VimScriptElementType("]");
  public static final IElementType LEFT_CURLY_BRACKET = new VimScriptElementType("{");
  public static final IElementType RIGHT_CURLY_BRACKET = new VimScriptElementType("}");

  /* quotes */
  public static final IElementType SINGLE_QUOTE = new VimScriptElementType("\'");
  public static final IElementType DOUBLE_QUOTE = new VimScriptElementType("\"");

  /* operators */
  public static final IElementType OP_ASSIGN = new VimScriptElementType("=");

  public static final IElementType OP_PLUS = new VimScriptElementType("+");
  public static final IElementType OP_MINUS = new VimScriptElementType("-");
  public static final IElementType OP_MULT = new VimScriptElementType("*");
  public static final IElementType OP_DIV = new VimScriptElementType("/");
  public static final IElementType OP_MOD = new VimScriptElementType("%");

  //logic
  public static final IElementType OP_EQUAL_TO = new VimScriptElementType("==");
  public static final IElementType OP_NOT_EQUAL_TO = new VimScriptElementType("!=");
  public static final IElementType OP_GT = new VimScriptElementType(">");
  public static final IElementType OP_GT_EQ = new VimScriptElementType(">=");
  public static final IElementType OP_LT = new VimScriptElementType("<");
  public static final IElementType OP_LT_EQ = new VimScriptElementType("<=");
  public static final IElementType OP_MATCHES = new VimScriptElementType("=~");
  public static final IElementType OP_NOT_MATCHES = new VimScriptElementType("!~");

  public static final IElementType OP_LOGICAL_OR = new VimScriptElementType("||");
  public static final IElementType OP_LOGICAL_AND = new VimScriptElementType("&&");

  /* separators */
  public static final IElementType COLON = new VimScriptElementType(":");
  public static final IElementType DOT = new VimScriptElementType(".");
  public static final IElementType QUESTION_MARK = new VimScriptElementType("?");
  public static final IElementType EXCLAMATION_MARK = new VimScriptElementType("!");

  /* identifiers */
  public static final IElementType ENVIRONMENT_VARIABLE = new VimScriptElementType("envvar");
  public static final IElementType OPTION = new VimScriptElementType("option");
  public static final IElementType REGISTER = new VimScriptElementType("register");
  public static final IElementType VARIABLE_WITH_PREFIX = new VimScriptElementType("prefix:varname");
  public static final IElementType IDENTIFIER = new VimScriptElementType("identifier");

  /* numbers */
  public static final IElementType FLOAT = new VimScriptElementType("float");
  public static final IElementType INTEGER = new VimScriptElementType("int");

  /* string */
  public static final IElementType STRING = new VimScriptElementType("string");
  public static final TokenSet strings = TokenSet.create(STRING);

  /* comment */
  public static final IElementType COMMENT = new VimScriptElementType("comment");
  public static final TokenSet comments = TokenSet.create(COMMENT);
}
