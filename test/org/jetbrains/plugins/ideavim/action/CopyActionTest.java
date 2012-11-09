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
  // TODO:
  public void _testWrongYankQuoteMotion() {
    final Editor editor = typeTextInFile(stringToKeys("y\""),
                                         "one <caret>two\n" +
                                         "three\n" +
                                         "four\n");
  }

  // TODO:
  public void _testWrongYankRegisterMotion() {
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
