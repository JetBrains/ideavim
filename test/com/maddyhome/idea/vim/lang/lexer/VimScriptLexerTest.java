package com.maddyhome.idea.vim.lang.lexer;

import com.intellij.psi.tree.IElementType;
import org.junit.*;

import java.io.*;
import java.util.ArrayList;

/**
 * <p>Date: 02.11.11</p>
 * <p>Test class for VimScriptLexer.</p>
 *
 * @author Ksenia V. Mamich
 * @version 1.0
 */
public class VimScriptLexerTest {
  String dir = "test/com/maddyhome/idea/vim/lang/lexer/files/";

  /* Tests only separate tokens */
  String testTokensFile = "testTokens";

  /* Pattern for more advanced tests */
  String test = "test";

  @Test
  public void testTokenFile() {
    final IElementType [] expected = {
        VimScriptTokenTypes.OP_EQUAL_TO,
        VimScriptTokenTypes.OP_NOT_EQUAL_TO,
        VimScriptTokenTypes.OP_GT_EQ,
        VimScriptTokenTypes.OP_GT,
        VimScriptTokenTypes.OP_LT_EQ,
        VimScriptTokenTypes.OP_LT,
        VimScriptTokenTypes.OP_MATCHES,
        VimScriptTokenTypes.OP_NOT_MATCHES,
        VimScriptTokenTypes.OP_LOGICAL_OR,
        VimScriptTokenTypes.OP_LOGICAL_AND,
        VimScriptTokenTypes.OP_PLUS,
        VimScriptTokenTypes.OP_MINUS,
        VimScriptTokenTypes.OP_MULT,
        VimScriptTokenTypes.OP_DIV,
        VimScriptTokenTypes.OP_MOD,
        VimScriptTokenTypes.OP_ASSIGN,
        VimScriptTokenTypes.COMMENT,
        VimScriptTokenTypes.INTEGER,
        VimScriptTokenTypes.INTEGER,
        VimScriptTokenTypes.WHITESPACE,
        VimScriptTokenTypes.INTEGER,
        VimScriptTokenTypes.COMMENT,
        VimScriptTokenTypes.FLOAT,
        VimScriptTokenTypes.FLOAT,
        VimScriptTokenTypes.FLOAT,
        VimScriptTokenTypes.FLOAT,
        VimScriptTokenTypes.FLOAT,
        VimScriptTokenTypes.FLOAT,
        VimScriptTokenTypes.FLOAT,
        VimScriptTokenTypes.INTEGER,
        VimScriptTokenTypes.DOT,
        VimScriptTokenTypes.WHITESPACE,
        VimScriptTokenTypes.COMMENT,
        VimScriptTokenTypes.INTEGER,
        VimScriptTokenTypes.IDENTIFIER,
        VimScriptTokenTypes.WHITESPACE,
        VimScriptTokenTypes.COMMENT,
        VimScriptTokenTypes.COMMENT,
        VimScriptTokenTypes.VARIABLE_WITH_PREFIX,
        VimScriptTokenTypes.VARIABLE_WITH_PREFIX,
        VimScriptTokenTypes.VARIABLE_WITH_PREFIX,
        VimScriptTokenTypes.IDENTIFIER,
        VimScriptTokenTypes.COLON,
        VimScriptTokenTypes.IDENTIFIER,
        VimScriptTokenTypes.WHITESPACE,
        VimScriptTokenTypes.COMMENT,
        VimScriptTokenTypes.ENVIRONMENT_VARIABLE,
        VimScriptTokenTypes.OPTION,
        VimScriptTokenTypes.REGISTER,
        VimScriptTokenTypes.IDENTIFIER,
        VimScriptTokenTypes.IDENTIFIER,
        VimScriptTokenTypes.IDENTIFIER,
        VimScriptTokenTypes.STRING,
        VimScriptTokenTypes.STRING,
        VimScriptTokenTypes.LEFT_ROUND_BRACKET,
        VimScriptTokenTypes.RIGHT_ROUND_BRACKET,
        VimScriptTokenTypes.LEFT_SQUARE_BRACKET,
        VimScriptTokenTypes.RIGHT_SQUARE_BRACKET,
        VimScriptTokenTypes.LEFT_CURLY_BRACKET,
        VimScriptTokenTypes.RIGHT_CURLY_BRACKET,
        VimScriptTokenTypes.COLON,
        VimScriptTokenTypes.DOT,
        VimScriptTokenTypes.BAD_CHARACTER,
        VimScriptTokenTypes.WHITESPACE,
        VimScriptTokenTypes.COMMENT,
        VimScriptTokenTypes.QUESTION_MARK,
        VimScriptTokenTypes.EXCLAMATION_MARK,
        null
    };

    try {
      File file = new File(dir + testTokensFile + ".vim");
      ArrayList<IElementType> actual = new ArrayList<IElementType>();

      VimScriptFlexLexer lexer = new VimScriptFlexLexer();
      final String data = read(file);
      lexer.start(data);
      while (true) {
        IElementType token = lexer.getTokenType();
        actual.add(token);
        if (token == null) {
          break;
        }
        lexer.advance();
      }

      Assert.assertArrayEquals(expected, actual.toArray());

    } catch (IOException ioe) {
      ioe.printStackTrace();
    }
  }

  @Test
  public void testExpression() {
    IElementType [] expected = {
        VimScriptTokenTypes.IDENTIFIER,
        VimScriptTokenTypes.WHITESPACE,
        VimScriptTokenTypes.VARIABLE_WITH_PREFIX,
        VimScriptTokenTypes.WHITESPACE,
        VimScriptTokenTypes.OP_ASSIGN,
        VimScriptTokenTypes.WHITESPACE,
        VimScriptTokenTypes.INTEGER,
        null
    };
    ArrayList<IElementType> actual = new ArrayList<IElementType>();
    try {
      File file = new File(dir + test + "1.vim");

      VimScriptFlexLexer lexer = new VimScriptFlexLexer();
      final String data = readFile(file);

      lexer.start(data);

      while (true) {
        IElementType token = lexer.getTokenType();
        actual.add(token);
        if (token == null) {
          break;
        }
        lexer.advance();
      }

    } catch (IOException e) {
      e.printStackTrace();
    }

    Assert.assertArrayEquals(expected, actual.toArray());
  }

  private static String readFile(File file) throws IOException {
    StringBuilder b = new StringBuilder();
    FileReader fr = new FileReader(file);
    char[] buf = new char[4096];
    while (true) {
      int n = fr.read(buf);
      if (n < 0) {
        break;
      }
      b.append(buf, 0, n);
    }
    return b.toString();
  }

  private static String read(File file) throws IOException {
    StringBuilder sb = new StringBuilder();
    BufferedReader br = new BufferedReader(new FileReader(file));
    while (true) {
      String line = br.readLine();
      if (line == null) {
        break;
      }
      sb.append(line).append('\n');
    }
    return sb.toString();
  }
}
