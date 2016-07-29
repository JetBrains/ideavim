package org.jetbrains.plugins.ideavim.ex;

import org.jetbrains.plugins.ideavim.VimTestCase;

public class ShiftCommandTest extends VimTestCase {

  // VIM-1154 |:>|
  public void testShiftRightSingleLine() {
    myFixture.configureByText("a.txt", "ABC\n" +
                                       "<caret>DEF\n" +
                                       "GHI");
    typeText(commandToKeys(">"));
    myFixture.checkResult("ABC\n" +
                          "    <caret>DEF\n" +
                          "GHI");
  }

  // VIM-1154 |:>>|
  public void testShiftRightSingleLineTwoCount() {
    myFixture.configureByText("a.txt", "ABC\n" +
            "<caret>DEF\n" +
            "GHI");
    typeText(commandToKeys(">>"));
    myFixture.checkResult("ABC\n" +
            "        <caret>DEF\n" +
            "GHI");
  }

  // VIM-1154 |2:>|
  public void testShiftRightMultipleLine() {
    myFixture.configureByText("a.txt",  "ABC\n" +
                                        "<caret>DEF\n" +
                                        "GHI");
    typeText(commandToKeys(">2"));
    myFixture.checkResult("ABC\n" +
                          "    <caret>DEF\n" +
                          "    GHI");
  }

  // VIM-1154 |:<|
  public void testShiftLeftSingleLine() {
    myFixture.configureByText("a.txt", "ABC\n" +
            "        <caret>DEF\n" +
            "GHI");
    typeText(commandToKeys("<"));
    myFixture.checkResult("ABC\n" +
            "    <caret>DEF\n" +
            "GHI");
  }

  // VIM-1154 |:<<|
  public void testShiftLeftSingleLineTwoCount() {
    myFixture.configureByText("a.txt", "ABC\n" +
            "        <caret>DEF\n" +
            "GHI");
    typeText(commandToKeys("<<"));
    myFixture.checkResult("ABC\n" +
            "<caret>DEF\n" +
            "GHI");
  }

  // VIM-1154 |2:<|
  public void testShiftLeftMultipleLine() {
    myFixture.configureByText("a.txt", "ABC\n" +
            "    <caret>DEF\n" +
            "    GHI");
    typeText(commandToKeys("<2"));
    myFixture.checkResult("ABC\n" +
            "<caret>DEF\n" +
            "GHI");
  }

}
