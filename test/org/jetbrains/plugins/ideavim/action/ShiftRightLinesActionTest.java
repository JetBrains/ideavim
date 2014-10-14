package org.jetbrains.plugins.ideavim.action;

import org.jetbrains.plugins.ideavim.VimTestCase;

import static com.maddyhome.idea.vim.helper.StringHelper.parseKeys;


/**
 * @author abrookins
 */
public class ShiftRightLinesActionTest extends VimTestCase {
  // VIM-407
  public void testShiftShiftsOneCharacterSingleLine() {
    myFixture.configureByText("a.txt", "<caret>w\n");
    typeText(parseKeys(">>"));
    myFixture.checkResult("    w\n");
  }

  // VIM-407
  public void testShiftShiftsOneCharacterMultiLine() {
    myFixture.configureByText("a.txt", "Hello\n<caret>w\nWorld");
    typeText(parseKeys(">>"));
    myFixture.checkResult("Hello\n    w\nWorld");
  }

  public void testShiftShiftsMultipleCharactersOneLine() {
    myFixture.configureByText("a.txt", "<caret>Hello, world!\n");
    typeText(parseKeys(">>"));
    myFixture.checkResult("    Hello, world!\n");
  }

  public void testShiftShiftsMultipleCharactersMultipleLines() {
    myFixture.configureByText("a.txt", "<caret>Hello,\nworld!\n");
    typeText(parseKeys("j>>"));
    myFixture.checkResult("Hello,\n    world!\n");
  }

  public void testShiftsSingleLineSelection() {
    myFixture.configureByText("a.txt", "<caret>Hello,\nworld!\n");
    typeText(parseKeys("jv$>>"));
    myFixture.checkResult("Hello,\n    world!\n");
  }

  public void testShiftsMultiLineSelection() {
    myFixture.configureByText("a.txt", "<caret>Hello,\nworld!\n");
    typeText(parseKeys("vj$>>"));
    myFixture.checkResult("    Hello,\n    world!\n");
  }

  public void testShiftsMultiLineSelectionSkipsNewline() {
    myFixture.configureByText("a.txt", "<caret>Hello,\nworld!\n\n");
    typeText(parseKeys("vG$>>"));
    myFixture.checkResult("    Hello,\n    world!\n\n");
  }
}
