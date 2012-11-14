package org.jetbrains.plugins.ideavim.action;

import org.jetbrains.plugins.ideavim.VimTestCase;

import javax.swing.*;
import java.util.List;

import static com.maddyhome.idea.vim.helper.StringHelper.stringToKeys;

/**
 * @author vlan
 */
public class InsertActionTest extends VimTestCase {
  // VIM-112 |i| |i_CTRL-W|
  public void testInsertDeletePreviousWord() {
    final List<KeyStroke> keys = stringToKeys("ione two three");
    keys.add(KeyStroke.getKeyStroke("control W"));
    typeTextInFile(keys,
                   "hello\n" +
                   "<caret>\n");
    myFixture.checkResult("hello\n" +
                          "one two \n");
  }
}
