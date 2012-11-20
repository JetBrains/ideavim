package org.jetbrains.plugins.ideavim.action;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.VisualPosition;
import com.maddyhome.idea.vim.command.CommandState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.ideavim.VimTestCase;

import javax.swing.*;
import java.util.List;

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
    final Editor editor = typeTextInFile(keys,
                                         "on<caret>e two\n" +
                                         "three\n");
    assertOffset(editor, 2);
    assertMode(editor, CommandState.Mode.COMMAND);
  }

  // |h| |l|
  public void testLeftRightMove() {
    final Editor editor = typeTextInFile(stringToKeys("14l2h"),
                                         "on<caret>e two three four five six seven\n");
    assertOffset(editor, 14);
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
    final Editor editor = typeTextInFile(keys,
                                         "on<caret>e two three four five six seven\n");
    assertOffset(editor, 6);
  }

  // |f|
  public void testForwardToTab() {
    final List<KeyStroke> keys = stringToKeys("f");
    keys.add(KeyStroke.getKeyStroke("TAB"));
    final Editor editor = typeTextInFile(keys,
                                         "on<caret>e two\tthree\nfour\n");
    assertOffset(editor, 7);
    assertMode(editor, CommandState.Mode.COMMAND);
  }

  public void testIllegalCharArgument() {
    final List<KeyStroke> keys = stringToKeys("f");
    keys.add(KeyStroke.getKeyStroke("INSERT"));
    final Editor editor = typeTextInFile(keys,
                                         "on<caret>e two three four five six seven\n");
    assertOffset(editor, 2);
    assertMode(editor, CommandState.Mode.COMMAND);
  }

  // |F| |i_CTRL-K|
  public void testBackToDigraph() {
    final List<KeyStroke> keys = stringToKeys("F");
    keys.add(KeyStroke.getKeyStroke("control K"));
    keys.add(KeyStroke.getKeyStroke('O'));
    keys.add(KeyStroke.getKeyStroke(':'));
    final Editor editor = typeTextInFile(keys,
                                         "Hallo, \u00d6ster<caret>reich!\n");
    assertOffset(editor, 7);
    assertMode(editor, CommandState.Mode.COMMAND);
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
    final Editor editor = typeTextInFile(stringToKeys("ci{"),
                                         "foo {\n" +
                                         "    <caret>bar\n" +
                                         "}\n");
    myFixture.checkResult("foo {\n" +
                          "\n" +
                          "}\n");
    assertOffset(editor, 6);
  }

  // |%|
  public void testPercentMatchSimple() {
    final Editor editor = typeTextInFile(stringToKeys("%"),
                                         "foo(b<caret>ar)\n");
    assertOffset(editor, 3);
  }

  // |%|
  public void testPercentMatchMultiLine() {
    final Editor editor = typeTextInFile(stringToKeys("%"),
                                         "foo(bar,\n" +
                                         "    baz,\n" +
                                         "    <caret>quux)\n");
    assertOffset(editor, 3);
  }

  // |%|
  public void testPercentMatchParensInString() {
    final Editor editor = typeTextInFile(stringToKeys("%"),
                                         "foo(bar, \"foo(bar\", <caret>baz)\n");
    assertOffset(editor, 3);
  }

  // |[(|
  public void testUnmatchedOpenParenthesis() {
    final Editor editor = typeTextInFile(stringToKeys("[("),
                                         "foo(bar, foo(bar, <caret>baz\n" +
                                         "bar(foo)\n");
    assertOffset(editor, 12);
  }

  // |[{|
  public void testUnmatchedOpenBracketMultiLine() {
    final Editor editor = typeTextInFile(stringToKeys("[{"),
                                         "foo {\n" +
                                         "    bar,\n" +
                                         "    b<caret>az\n");
    assertOffset(editor, 4);
  }

  // |])|
  public void testUnmatchedCloseParenthesisMultiLine() {
    final Editor editor = typeTextInFile(stringToKeys("])"),
                                         "foo(bar, <caret>baz,\n" +
                                         "   quux)\n");
    assertOffset(editor, 21);
  }

  // |]}|
  public void testUnmatchedCloseBracket() {
    final Editor editor = typeTextInFile(stringToKeys("]}"),
                                         "{bar, <caret>baz}\n");
    assertOffset(editor, 9);
  }


  public void assertOffset(@NotNull Editor editor, int expectedOffset) {
    final int offset = editor.getCaretModel().getOffset();
    assertEquals(expectedOffset, offset);
  }

  public void assertMode(@NotNull Editor editor, @NotNull CommandState.Mode expectedMode) {
    final CommandState.Mode mode = CommandState.getInstance(editor).getMode();
    assertEquals(expectedMode, mode);
  }
}
