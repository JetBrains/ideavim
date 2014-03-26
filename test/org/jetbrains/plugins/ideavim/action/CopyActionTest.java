package org.jetbrains.plugins.ideavim.action;

import com.intellij.openapi.editor.Editor;
import org.jetbrains.plugins.ideavim.VimTestCase;

import static com.maddyhome.idea.vim.helper.StringHelper.parseKeys;

/**
 * @author vlan
 */
public class CopyActionTest extends VimTestCase {
  // |y| |p| |count|
  public void testYankPutCharacters() {
    typeTextInFile(parseKeys("y2h", "p"),
                   "one two<caret> three\n");
    myFixture.checkResult("one twwoo three\n");
  }

  // |yy|
  public void testYankLine() {
    typeTextInFile(parseKeys("yy", "p"),
                   "one\n" +
                   "tw<caret>o\n" +
                   "three\n");
    myFixture.checkResult("one\n" +
                          "two\n" +
                          "two\n" +
                          "three\n");
  }

  // VIM-390 |yy| |p|
  public void testYankLinePasteAtLastLine() {
    typeTextInFile(parseKeys("yy", "p"),
                   "one two\n" +
                   "<caret>three four\n");
    myFixture.checkResult("one two\n" +
                          "three four\n" +
                          "three four\n");
  }

  // |register| |y|
  public void testYankRegister() {
    typeTextInFile(parseKeys("\"ayl", "l", "\"byl", "\"ap", "\"bp"),
                   "hel<caret>lo world\n");
    myFixture.checkResult("hellolo world\n");
  }

  // |P|
  public void testYankPutBefore() {
    typeTextInFile(parseKeys("y2l", "P"),
                   "<caret>two\n");
    myFixture.checkResult("twtwo\n");
  }

  public void testWrongYankQuoteMotion() {
    assertPluginError(false);
    typeTextInFile(parseKeys("y\""),
                   "one <caret>two\n" +
                   "three\n" +
                   "four\n");
    assertPluginError(true);
  }

  public void testWrongYankQuoteYankLine() {
    assertPluginError(false);
    typeTextInFile(parseKeys("y\"", "yy", "p"),
                   "one <caret>two\n" +
                   "three\n" +
                   "four\n");
    assertPluginError(false);
    myFixture.checkResult("one two\n" +
                          "one two\n" +
                          "three\n" +
                          "four\n");
  }

  public void testWrongYankRegisterMotion() {
    final Editor editor = typeTextInFile(parseKeys("y\"", "0"),
                                         "one <caret>two\n" +
                                         "three\n" +
                                         "four\n");
    assertEquals(0, editor.getCaretModel().getOffset());
  }

   // |v_y|
  public void testYankVisualBlock() {
    typeTextInFile(parseKeys("<C-V>", "jl", "yl", "p"),
                   "<caret>* one\n" +
                   "* two\n");

    // XXX:
    // The correct output should be:
    //
    // * * one
    // * * two
    //
    // The problem is that the selection range should be 1-char wide when entering the visual block mode

    myFixture.checkResult("* *one\n" +
                          "* *two\n");
  }
}
