package org.jetbrains.plugins.ideavim.action;

import com.maddyhome.idea.vim.VimPlugin;
import com.maddyhome.idea.vim.common.Mark;
import org.jetbrains.plugins.ideavim.VimTestCase;

import static com.maddyhome.idea.vim.helper.StringHelper.parseKeys;

/**
 * @author Tuomas Tynkkynen
 */
public class MarkTest extends VimTestCase {
  // |m|
  public void testLocalMark() {
    typeTextInFile(parseKeys("ma"), "    foo\n" +
                                    "    ba<caret>r\n" +
                                    "    baz\n");
    Mark mark = VimPlugin.getMark().getMark(myFixture.getEditor(), 'a');
    assertNotNull(mark);
    assertEquals(1, mark.getLogicalLine());
    assertEquals(6, mark.getCol());
  }

  // |m|
  public void testGlobalMark() {
    typeTextInFile(parseKeys("mG"), "    foo\n" +
                                    "    ba<caret>r\n" +
                                    "    baz\n");
    Mark mark = VimPlugin.getMark().getMark(myFixture.getEditor(), 'G');
    assertNotNull(mark);
    assertEquals(1, mark.getLogicalLine());
    assertEquals(6, mark.getCol());
  }

  // |m|
  public void testMarkIsDeletedWhenLineIsDeleted() {
    typeTextInFile(parseKeys("mx", "dd"), "    foo\n" +
                                          "    ba<caret>r\n" +
                                          "    baz\n");
    Mark mark = VimPlugin.getMark().getMark(myFixture.getEditor(), 'x');
    assertNull(mark);
  }

  // |m|
  public void testMarkIsMovedUpWhenLinesAreDeletedAbove() {
    typeTextInFile(parseKeys("mx", "2k", "2dd"), "    foo\n" +
                                                 "    ba<r\n" +
                                                 "    ba<caret>z\n");
    Mark mark = VimPlugin.getMark().getMark(myFixture.getEditor(), 'x');
    assertNotNull(mark);
    assertEquals(0, mark.getLogicalLine());
    assertEquals(6, mark.getCol());
  }

  // |m|
  public void testMarkIsMovedDownWhenLinesAreInsertedAbove() {
    typeTextInFile(parseKeys("mY", "Obiff"), "    foo\n" +
                                             "    ba<caret>r\n" +
                                             "    baz\n");
    Mark mark = VimPlugin.getMark().getMark(myFixture.getEditor(), 'Y');
    assertNotNull(mark);
    assertEquals(2, mark.getLogicalLine());
    assertEquals(6, mark.getCol());
  }

  // |m| |`|
  public void testMarkAndJumpToMark() {
    typeTextInFile(parseKeys("6l", "mZ", "G$", "`Z"), "    foo\n" +
                                                      "    bar\n" +
                                                      "    baz\n");
    assertOffset(6);
  }

  // |m| |'|
  public void testMarkAndJumpToMarkLeadingSpace() {
    typeTextInFile(parseKeys("6l", "mb", "G$", "'b"), "    foo\n" +
                                                      "    bar\n" +
                                                      "    baz\n");
    assertOffset(4);
  }

  // |m| |`|
  public void testDeleteBacktickMotionIsCharacterWise() {
    typeTextInFile(parseKeys("mk", "kh", "d`k"), "    abcd\n" +
                                                 "    efgh\n" +
                                                 "    ij<caret>kl\n" +
                                                 "    mnop\n");
    myFixture.checkResult("    abcd\n" +
                          "    ekl\n" +
                          "    mnop\n");
  }

  // |m| |`|
  public void testDeleteSingleQuoteMotionIsLineWise() {
    typeTextInFile(parseKeys("mk", "kh", "d'k"), "    abcd\n" +
                                                 "    efgh\n" +
                                                 "    ij<caret>kl\n" +
                                                 "    mnop\n");
    myFixture.checkResult("    abcd\n" +
                          "    mnop\n");
  }

  // VIM-43 |i| |`.|
  public void testGotoLastChangePosition() {
    typeTextInFile(parseKeys("i", "hello ", "<Esc>", "gg", "`."), "one two\n" +
                                                                  "<caret>hello world\n" +
                                                                  "three four\n");
    assertOffset(13);
  }

  // VIM-43 |p| |`.|
  public void testGotoLastPutPosition() {
    typeTextInFile(parseKeys("yy", "p", "gg", "`."), "one two\n" +
                                                     "<caret>three\n" +
                                                     "four five\n");
    assertOffset(14);
  }
}
