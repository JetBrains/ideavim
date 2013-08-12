package org.jetbrains.plugins.ideavim.ex;

import com.maddyhome.idea.vim.helper.StringHelper;
import org.jetbrains.plugins.ideavim.VimTestCase;

/**
 * @author vlan
 */
public class VariousCommandsTest extends VimTestCase {
  // VIM-550 |:put|
  public void testPutCreatesNewLine() {
    myFixture.configureByText("a.txt", "Test\n" +
                                       "Hello <caret>World!\n");
    typeText(StringHelper.stringToKeys("\"ayw"));
    runExCommand("put a");
    myFixture.checkResult("Test\n" +
                          "Hello World!\n" +
                          "<caret>World\n");
  }
}
