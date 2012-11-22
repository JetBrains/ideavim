package org.jetbrains.plugins.ideavim.action;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.VisualPosition;
import org.jetbrains.plugins.ideavim.VimTestCase;

import javax.swing.*;
import java.util.List;

import static com.maddyhome.idea.vim.command.CommandState.Mode.COMMAND;
import static com.maddyhome.idea.vim.helper.StringHelper.stringToKeys;

/**
 * @author vlan
 */
public class MotionActionTest extends VimTestCase {
  // VIM-198 |v_iw|
  public void testVisualMotionInnerWordNewLineAtEOF() {
    final Editor editor = typeTextInFile(stringToKeys("viw"),
                                         "one tw<caret>o\n");
    final String selected = editor.getSelectionModel().getSelectedText();
    assertEquals("two", selected);
  }

  // |v_iW|
  public void testVisualMotionInnerBigWord() {
    final Editor editor = typeTextInFile(stringToKeys("viW"),
                                         "one tw<caret>o.three four\n");
    final String selected = editor.getSelectionModel().getSelectedText();
    assertEquals("two.three", selected);
  }

  public void testEscapeInCommand() {
    final List<KeyStroke> keys = stringToKeys("f");
    keys.add(KeyStroke.getKeyStroke("ESCAPE"));
    // We cannot check yet that the plugin indicates an error (see 'visualbell') after the second ESCAPE, so we just check the caret
    // position and the editor mode
    keys.add(KeyStroke.getKeyStroke("ESCAPE"));
    typeTextInFile(keys,
                   "on<caret>e two\n" +
                   "three\n");
    assertOffset(2);
    assertMode(COMMAND);
  }

  // |h| |l|
  public void testLeftRightMove() {
    typeTextInFile(stringToKeys("14l2h"),
                   "on<caret>e two three four five six seven\n");
    assertOffset(14);
  }

  // |j| |k|
  public void testUpDownMove() {
    final Editor editor = typeTextInFile(stringToKeys("2jk"),
                                         "one\n" +
                                         "tw<caret>o\n" +
                                         "three\n" +
                                         "four\n");
    final VisualPosition position = editor.getCaretModel().getVisualPosition();
    assertEquals(new VisualPosition(2, 2), position);
  }

  public void testDeleteDigitsInCount() {
    final List<KeyStroke> keys = stringToKeys("42");
    keys.add(KeyStroke.getKeyStroke("DELETE"));
    keys.add(KeyStroke.getKeyStroke('l'));
    typeTextInFile(keys,
                   "on<caret>e two three four five six seven\n");
    assertOffset(6);
  }

  // |f|
  public void testForwardToTab() {
    final List<KeyStroke> keys = stringToKeys("f");
    keys.add(KeyStroke.getKeyStroke("TAB"));
    typeTextInFile(keys,
                   "on<caret>e two\tthree\nfour\n");
    assertOffset(7);
    assertMode(COMMAND);
  }

  public void testIllegalCharArgument() {
    final List<KeyStroke> keys = stringToKeys("f");
    keys.add(KeyStroke.getKeyStroke("INSERT"));
    typeTextInFile(keys,
                   "on<caret>e two three four five six seven\n");
    assertOffset(2);
    assertMode(COMMAND);
  }

  // |F| |i_CTRL-K|
  public void testBackToDigraph() {
    final List<KeyStroke> keys = stringToKeys("F");
    keys.add(KeyStroke.getKeyStroke("control K"));
    keys.add(KeyStroke.getKeyStroke('O'));
    keys.add(KeyStroke.getKeyStroke(':'));
    typeTextInFile(keys,
                   "Hallo, \u00d6ster<caret>reich!\n");
    assertOffset(7);
    assertMode(COMMAND);
  }

  // VIM-326 |d| |v_ib|
  public void testDeleteInnerBlock() {
    typeTextInFile(stringToKeys("di)"),
                   "foo(\"b<caret>ar\")\n");
    myFixture.checkResult("foo()\n");
  }

  // VIM-314 |d| |v_iB|
  public void testDeleteInnerBracketBlock() {
    typeTextInFile(stringToKeys("di{"),
                   "{foo, b<caret>ar, baz}\n");
    myFixture.checkResult("{}\n");
  }

  // VIM-261 |c| |v_iB|
  public void testChangeInnerBracketBlockMultiLine() {
    typeTextInFile(stringToKeys("ci{"),
                   "foo {\n" +
                   "    <caret>bar\n" +
                   "}\n");
    myFixture.checkResult("foo {\n" +
                          "\n" +
                          "}\n");
    assertOffset(6);
  }

  // VIM-275 |d| |v_ib|
  public void testDeleteInnerParensBlockBeforeOpen() {
    typeTextInFile(stringToKeys("di)"),
                   "foo<caret>(bar)\n");
    myFixture.checkResult("foo()\n");
    assertOffset(4);
  }

  // |d| |v_ib|
  public void testDeleteInnerParensBlockBeforeClose() {
    typeTextInFile(stringToKeys("di)"),
                   "foo(bar<caret>)\n");
    myFixture.checkResult("foo()\n");
  }

  // |%|
  public void testPercentMatchSimple() {
    typeTextInFile(stringToKeys("%"),
                   "foo(b<caret>ar)\n");
    assertOffset(3);
  }

  // |%|
  public void testPercentMatchMultiLine() {
    typeTextInFile(stringToKeys("%"),
                   "foo(bar,\n" +
                   "    baz,\n" +
                   "    <caret>quux)\n");
    assertOffset(3);
  }

  // |%|
  public void testPercentMatchParensInString() {
    typeTextInFile(stringToKeys("%"),
                   "foo(bar, \"foo(bar\", <caret>baz)\n");
    assertOffset(3);
  }

  // |[(|
  public void testUnmatchedOpenParenthesis() {
    typeTextInFile(stringToKeys("[("),
                   "foo(bar, foo(bar, <caret>baz\n" +
                   "bar(foo)\n");
    assertOffset(12);
  }

  // |[{|
  public void testUnmatchedOpenBracketMultiLine() {
    typeTextInFile(stringToKeys("[{"),
                   "foo {\n" +
                   "    bar,\n" +
                   "    b<caret>az\n");
    assertOffset(4);
  }

  // |])|
  public void testUnmatchedCloseParenthesisMultiLine() {
    typeTextInFile(stringToKeys("])"),
                   "foo(bar, <caret>baz,\n" +
                   "   quux)\n");
    assertOffset(21);
  }

  // |]}|
  public void testUnmatchedCloseBracket() {
    typeTextInFile(stringToKeys("]}"),
                   "{bar, <caret>baz}\n");
    assertOffset(9);
  }

  // VIM-331 |w|
  public void testNonAsciiLettersInWord() {
    typeTextInFile(stringToKeys("w"),
                   "Če<caret>ská republika");
    assertOffset(6);
  }

  // |w|
  public void testEmptyLineIsWord() {
    typeTextInFile(stringToKeys("w"),
                   "<caret>one\n" +
                   "\n" +
                   "two\n");
    assertOffset(4);
  }

  // |w|
  public void testNotEmptyLineIsNotWord() {
    typeTextInFile(stringToKeys("w"),
                   "<caret>one\n" +
                   " \n" +
                   "two\n");
    assertOffset(6);
  }

  // VIM-312 |w|
  public void testLastWord() {
    typeTextInFile(stringToKeys("w"),
                   "<caret>one\n");
    assertOffset(2);
  }
}
