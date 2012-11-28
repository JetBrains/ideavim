package org.jetbrains.plugins.ideavim.action;

import com.intellij.openapi.editor.Editor;
import org.jetbrains.plugins.ideavim.VimTestCase;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

import static com.maddyhome.idea.vim.helper.StringHelper.stringToKeys;

/**
 * @author vlan
 */
public class CopyActionTest extends VimTestCase {
  // |y| |p| |count|
  public void testYankPutCharacters() {
    typeTextInFile(stringToKeys("y2hp"),
                   "one two<caret> three\n");
    myFixture.checkResult("one twwoo three\n");
  }

  // |yy|
  public void testYankLine() {
    typeTextInFile(stringToKeys("yyp"),
                   "one\n" +
                   "tw<caret>o\n" +
                   "three\n");
    myFixture.checkResult("one\n" +
                          "two\n" +
                          "two\n" +
                          "three\n");
  }

  // |register| |y|
  public void testYankRegister() {
    typeTextInFile(stringToKeys("\"ayll\"byl\"ap\"bp"),
                   "hel<caret>lo world\n");
    myFixture.checkResult("hellolo world\n");
  }

  // |P|
  public void testYankPutBefore() {
    typeTextInFile(stringToKeys("y2lP"),
                   "<caret>two\n");
    myFixture.checkResult("twtwo\n");
  }

  public void testWrongYankQuoteMotion() {
    assertPluginError(false);
    typeTextInFile(stringToKeys("y\""),
                   "one <caret>two\n" +
                   "three\n" +
                   "four\n");
    assertPluginError(true);
  }

  public void testWrongYankQuoteYankLine() {
    assertPluginError(false);
    typeTextInFile(stringToKeys("y\"yyp"),
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
    final Editor editor = typeTextInFile(stringToKeys("y\"0"),
                                         "one <caret>two\n" +
                                         "three\n" +
                                         "four\n");
    assertEquals(0, editor.getCaretModel().getOffset());
  }

   // |v_y|
  public void testYankVisualBlock() {
    final List<KeyStroke> keys = new ArrayList<KeyStroke>();
    keys.add(KeyStroke.getKeyStroke("control V"));
    keys.addAll(stringToKeys("jlylp"));
    typeTextInFile(keys,
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
