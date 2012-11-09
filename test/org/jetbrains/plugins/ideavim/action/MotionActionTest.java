package org.jetbrains.plugins.ideavim.action;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.VisualPosition;
import com.maddyhome.idea.vim.command.CommandState;
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
    final int offset = editor.getCaretModel().getOffset();
    assertEquals(2, offset);
    final CommandState.Mode mode = CommandState.getInstance(editor).getMode();
    assertEquals(CommandState.Mode.COMMAND, mode);
  }

  // |h| |l|
  public void testLeftRightMove() {
    final Editor editor = typeTextInFile(stringToKeys("14l2h"),
                                         "on<caret>e two three four five six seven\n");
    final int offset = editor.getCaretModel().getOffset();
    assertEquals(14, offset);
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
    final int offset = editor.getCaretModel().getOffset();
    assertEquals(6, offset);
  }

  // |f|
  public void testForwardToTab() {
    final List<KeyStroke> keys = stringToKeys("f");
    keys.add(KeyStroke.getKeyStroke("TAB"));
    final Editor editor = typeTextInFile(keys,
                                         "on<caret>e two\tthree\nfour\n");
    final int offset = editor.getCaretModel().getOffset();
    assertEquals(7, offset);
    final CommandState.Mode mode = CommandState.getInstance(editor).getMode();
    assertEquals(CommandState.Mode.COMMAND, mode);
  }

  public void testIllegalCharArgument() {
    final List<KeyStroke> keys = stringToKeys("f");
    keys.add(KeyStroke.getKeyStroke("INSERT"));
    final Editor editor = typeTextInFile(keys,
                                         "on<caret>e two three four five six seven\n");
    final int offset = editor.getCaretModel().getOffset();
    assertEquals(2, offset);
    final CommandState.Mode mode = CommandState.getInstance(editor).getMode();
    assertEquals(CommandState.Mode.COMMAND, mode);
  }

  // |F| |i_CTRL-K|
  public void testBackToDigraph() {
    final List<KeyStroke> keys = stringToKeys("F");
    keys.add(KeyStroke.getKeyStroke("control K"));
    keys.add(KeyStroke.getKeyStroke('O'));
    keys.add(KeyStroke.getKeyStroke(':'));
    final Editor editor = typeTextInFile(keys,
                                         "Hallo, \u00d6ster<caret>reich!\n");
    final int offset = editor.getCaretModel().getOffset();
    assertEquals(7, offset);
    final CommandState.Mode mode = CommandState.getInstance(editor).getMode();
    assertEquals(CommandState.Mode.COMMAND, mode);
  }
}
