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

  // VIM-551 |:put|
  public void testPutDefault() {
    myFixture.configureByText("a.txt", "<caret>Hello World!\n");
    typeText(StringHelper.stringToKeys("yw"));
    runExCommand("put");
    myFixture.checkResult("Hello World!\n" +
                          "<caret>Hello \n");
  }
}
