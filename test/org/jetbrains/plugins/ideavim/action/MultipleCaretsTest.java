package org.jetbrains.plugins.ideavim.action;

import com.intellij.openapi.editor.Editor;
import org.jetbrains.plugins.ideavim.VimTestCase;

import static com.maddyhome.idea.vim.helper.StringHelper.parseKeys;

/**
 * @author Vasily Alferov
 */
public class MultipleCaretsTest extends VimTestCase {
  // com.maddyhome.idea.vim.action.motion.leftright

  public void testMotionLeftAction() {
    typeTextInFile(parseKeys("3h"),
                   "abc<caret>de<caret>");
    myFixture.checkResult("<caret>ab<caret>cde");
  }

  public void testMotionRightAction() {
    typeTextInFile(parseKeys("l"),
                   "ab<caret>cd<caret>ef");
    myFixture.checkResult("abc<caret>de<caret>f");
  }

  public void testMovementMerging() {
    Editor editor = typeTextInFile(parseKeys("2h"),
                                   "o<caret>n<caret>e");
    assertEquals(1, editor.getCaretModel().getCaretCount());
    myFixture.checkResult("<caret>one");
  }

  public void testMotionColumnAction() {
    typeTextInFile(parseKeys("4|"),
                   "one<caret> two\n" +
                   "three four fiv<caret>e\n" +
                   "si<caret>x seven<caret>\n" +
                   "<caret>eig<caret>ht nine ten<caret>");
    myFixture.checkResult("one<caret> two\n" +
                          "thr<caret>ee four five\n" +
                          "six<caret> seven\n" +
                          "eig<caret>ht nine ten");
  }

  public void testMotionFirstColumnAction() {
    typeTextInFile(parseKeys("0"),
                   "one<caret> two\n" +
                   "three four fiv<caret>e\n" +
                   "si<caret>x seven<caret>\n" +
                   "<caret>eig<caret>ht nine te<caret>n");
    myFixture.checkResult("<caret>one two\n" +
                          "<caret>three four five\n" +
                          "<caret>six seven\n" +
                          "<caret>eight nine ten");
  }

  public void testMotionFirstNonSpaceAction() {
    typeTextInFile(parseKeys("^"),
                   "     one<caret> two\n" +
                   "three<caret> four\n" +
                   "  five<caret> six\n" +
                   " <caret>  seven eight");
    myFixture.checkResult("     <caret>one two\n" +
                          "<caret>three four\n" +
                          "  <caret>five six\n" +
                          "   <caret>seven eight");
  }

  public void testMotionLastNonSpaceAction() {
    typeTextInFile(parseKeys("g_"),
                   "one<caret> two   \n" +
                   "three<caret> four      \n" +
                   " five si<caret>x\n" +
                   "seven eight    <caret>  \n");
    myFixture.checkResult("one tw<caret>o   \n" +
                          "three fou<caret>r      \n" +
                          " five si<caret>x\n" +
                          "seven eigh<caret>t      \n");
  }

  public void testMotionLastColumnAction() {
    typeTextInFile(parseKeys("$"),
                   "one <caret>two\n" +
                   "three fou<caret>r");
    myFixture.checkResult("one tw<caret>o\n" +
                          "three fou<caret>r");
  }

  public void testMotionLeftMatchCharAction() {
    typeTextInFile(parseKeys("2Fa"),
                   "a<caret>a<caret>abab<caret>ab<caret>ab<caret>b<caret>x");
    myFixture.checkResult("<caret>a<caret>a<caret>ab<caret>ab<caret>ababbx");
  }

  public void testMotionRightMatchCharAction() {
    typeTextInFile(parseKeys("2fb"),
                   "a<caret>a<caret>abab<caret>ab<caret>ab<caret>b<caret>x");
    myFixture.checkResult("aaaba<caret>baba<caret>b<caret>b<caret>x");
  }

  public void testMotionLeftTillMatchCharAction() {
    typeTextInFile(parseKeys("2Ta"),
                   "b<caret>a<caret>ba<caret>a<caret>a<caret>ba<caret>b");
    myFixture.checkResult("b<caret>a<caret>ba<caret>a<caret>a<caret>bab");
  }

  public void testMotionRightTillMatchCharAction() {
    typeTextInFile(parseKeys("2ta"),
                   "<caret>b<caret>a<caret>b<caret>a<caret>a<caret>a<caret>ba<caret>b");
    myFixture.checkResult("ba<caret>b<caret>a<caret>a<caret>a<caret>ba<caret>b");
  }

  public void testMotionLastLeftMatchChar() {
    typeTextInFile(parseKeys("Fa;"),
                   "a<caret>a<caret>abab<caret>ab<caret>ab<caret>b<caret>x");
    myFixture.checkResult("<caret>aa<caret>ab<caret>ab<caret>ababbx");
  }

  public void testMotionLastRightMatchChar() {
    typeTextInFile(parseKeys("fb;"),
                   "<caret>a<caret>aabab<caret>ab<caret>ab<caret>b<caret>x");
    myFixture.checkResult("aaaba<caret>baba<caret>b<caret>b<caret>x");
  }

  public void testMotionLastLeftTillMatchChar() {
    typeTextInFile(parseKeys("Ta,"),
                   "b<caret>aba<caret>a<caret>a<caret>ba<caret>b");
    myFixture.checkResult("ba<caret>baa<caret>a<caret>ba<caret>b");
  }

  public void testMotionLastRightTillMatchChar() {
    typeTextInFile(parseKeys("ta;"),
                   "<caret>b<caret>a<caret>b<caret>a<caret>a<caret>a<caret>ba<caret>b");
    myFixture.checkResult("ba<caret>b<caret>a<caret>aa<caret>ba<caret>b");
  }

  public void testMotionLastMatchCharReverse() {
    typeTextInFile(parseKeys("fa", "2;", "3,"),
                   "abaab<caret>a<caret>baaa<caret>abaaba");
    myFixture.checkResult("abaab<caret>abaaa<caret>abaaba");
  }
}
