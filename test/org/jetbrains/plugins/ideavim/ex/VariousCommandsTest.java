package org.jetbrains.plugins.ideavim.ex;

import com.maddyhome.idea.vim.command.CommandState;
import com.maddyhome.idea.vim.helper.StringHelper;
import com.maddyhome.idea.vim.insert.InsertToCommandStateTimer;
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

  public void testINoRemapAllowsCustomEscape() {
    myFixture.configureByText("a.txt", "Hello <caret>World!\n");
    runExCommand("inoremap jk <esc>");
    typeText(StringHelper.stringToKeys("i"));
    assertMode(CommandState.Mode.INSERT);
    typeText(StringHelper.stringToKeys("jk"));
    assertMode(CommandState.Mode.COMMAND);
  }

  public void testINoRemapAllowsLeaderKey() {
    myFixture.configureByText("a.txt", "Hello <caret>World!\n");
    runExCommand("inoremap jk <esc>");
    typeText(StringHelper.stringToKeys("i"));
    typeText(StringHelper.stringToKeys("j"));
    typeText(StringHelper.stringToKeys("q"));
    myFixture.checkResult("Hello jq<caret>World!\n");
  }

  public void testINoRemapAllowsForTimeout() {
    myFixture.configureByText("a.txt", "Hello <caret>World!\n");
    runExCommand("inoremap jj <esc>");
    typeText(StringHelper.stringToKeys("i"));
    typeText(StringHelper.stringToKeys("j"));
    try {
      Thread.sleep(InsertToCommandStateTimer.DEFAULT_KEY_SEQUENCE_TIMEOUT + 500);
    }
    catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
    typeText(StringHelper.stringToKeys("j"));
    myFixture.checkResult("Hello jj<caret>World!\n");
  }

  public void testINoRemapAllowsForTimeoutForThreeTimeouts() {
    myFixture.configureByText("a.txt", "Hello <caret>World!\n");
    runExCommand("inoremap jj <esc>");
    typeText(StringHelper.stringToKeys("i"));
    typeText(StringHelper.stringToKeys("j"));
    try {
      Thread.sleep(InsertToCommandStateTimer.DEFAULT_KEY_SEQUENCE_TIMEOUT + 500);
    }
    catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
    typeText(StringHelper.stringToKeys("j"));
    try {
      Thread.sleep(InsertToCommandStateTimer.DEFAULT_KEY_SEQUENCE_TIMEOUT + 500);
    }
    catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
    typeText(StringHelper.stringToKeys("j"));
    try {
      Thread.sleep(InsertToCommandStateTimer.DEFAULT_KEY_SEQUENCE_TIMEOUT + 500);
    }
    catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
    typeText(StringHelper.stringToKeys("j"));
    myFixture.checkResult("Hello jjjj<caret>World!\n");
  }
  public void testInsertWorksCorrectlyWithTimeout() {
    myFixture.configureByText("testInsertWorksCorrectlyWithTimeout.txt", "Hello <caret>World!\n");
    typeText(StringHelper.stringToKeys("i"));
    typeText(StringHelper.stringToKeys("h"));
    typeText(StringHelper.stringToKeys("e"));
    typeText(StringHelper.stringToKeys("l"));
    myFixture.checkResult("Hello hel<caret>World!\n");

  }
}
