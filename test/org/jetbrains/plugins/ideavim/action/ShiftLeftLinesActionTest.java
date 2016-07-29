package org.jetbrains.plugins.ideavim.action;

import org.jetbrains.plugins.ideavim.VimTestCase;

import static com.maddyhome.idea.vim.helper.StringHelper.parseKeys;


public class ShiftLeftLinesActionTest extends VimTestCase {

  public void testShiftShiftsSingleLine() {
    myFixture.configureByText("a.txt", "        <caret>w\n");
    typeText(parseKeys("<<"));
    myFixture.checkResult("    w\n");
  }

  public void testShiftShiftsMultiLine() {
    myFixture.configureByText("a.txt", "Hello\n        <caret>w\nWorld");
    typeText(parseKeys("<<"));
    myFixture.checkResult("Hello\n    w\nWorld");
  }

}
