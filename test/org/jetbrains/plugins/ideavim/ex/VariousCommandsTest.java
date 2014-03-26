package org.jetbrains.plugins.ideavim.ex;

import org.jetbrains.plugins.ideavim.VimTestCase;

import static com.maddyhome.idea.vim.helper.StringHelper.parseKeys;

/**
 * @author vlan
 */
public class VariousCommandsTest extends VimTestCase {
  // VIM-550 |:put|
  public void testPutCreatesNewLine() {
    myFixture.configureByText("a.txt", "Test\n" +
                                       "Hello <caret>World!\n");
    typeText(parseKeys("\"ayw"));
    runExCommand("put a");
    myFixture.checkResult("Test\n" +
                          "Hello World!\n" +
                          "<caret>World\n");
  }

  // VIM-551 |:put|
  public void testPutDefault() {
    myFixture.configureByText("a.txt", "<caret>Hello World!\n");
    typeText(parseKeys("yw"));
    runExCommand("put");
    myFixture.checkResult("Hello World!\n" +
                          "<caret>Hello \n");
  }
}
