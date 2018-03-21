package org.jetbrains.plugins.ideavim.action;

import com.intellij.openapi.editor.Editor;
import org.jetbrains.plugins.ideavim.VimTestCase;

import static com.maddyhome.idea.vim.helper.StringHelper.parseKeys;

public class MultiCaretTest extends VimTestCase {
  public void testMultipleCaretsInsert() {
    Editor editor = typeTextInFile(parseKeys("ix"),
                                   "a<caret>b<caret>c<caret>d");
    myFixture.checkResult("ax<caret>bx<caret>cx<caret>d");
  }

  public void testMultipleCaretsLeftMove() {
    Editor editor = typeTextInFile(parseKeys("3h"),
                                   "abc<caret>de<caret>");
    myFixture.checkResult("<caret>ab<caret>cde");
  }

  public void testMultipleCaretsRightMove() {
    Editor editor = typeTextInFile(parseKeys("l"),
                                   "ab<caret>cd<caret>ef");
    myFixture.checkResult("abc<caret>de<caret>f");
  }

  public void testMutlipleCaretsUpMove() {
    Editor editor = typeTextInFile(parseKeys("2k"),
                                   "one two\n" +
                                   "three four\n" +
                                   "fi<caret>ve<caret> six<caret> seven\n");
    myFixture.checkResult("on<caret>e <caret>two<caret>\n" +
                          "three four\n" +
                          "five six seven\n");
  }

  public void testMultipleCaretsDownMove() {
    Editor editor = typeTextInFile(parseKeys("2j"),
                                   "on<caret>e <caret>two<caret>\n" +
                                   "three four\n" +
                                   "five six seven\n");
    myFixture.checkResult("one two\n" +
                          "three four\n" +
                          "fi<caret>ve<caret> si<caret>x seven");
  }

  public void testMultipleCaretsMovementMerging() {
    Editor editor = typeTextInFile(parseKeys("2h"),
                                   "o<caret>n<caret>e");
    assertEquals(1, editor.getCaretModel().getCaretCount());
    myFixture.checkResult("<caret>one");
  }
}
