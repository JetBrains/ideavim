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

  public void testMotionLeftWrap() {
    typeTextInFile(parseKeys("5<BS>"),
                   "one\n" +
                   "t<caret>wo three\n" +
                   "fo<caret>ur\n");
    myFixture.checkResult("<caret>one\ntwo thr<caret>ee\nfour\n");
  }

  public void testMotionRightWrap() {
    typeTextInFile(parseKeys("5<Space>"),
                   "<caret>one\n" +
                   "two thr<caret>ee\n" +
                   "four\n");
    myFixture.checkResult("one\nt<caret>wo three\nfo<caret>ur\n");
  }

  // com.maddyhome.idea.vim.action.motion.updown

  public void testMotionUpAction() {
    typeTextInFile(parseKeys("k"),
                   "o<caret>ne\n" +
                   "t<caret>wo<caret> \n" +
                   "t<caret>hree<caret> ");
    myFixture.checkResult("o<caret>n<caret>e\n" +
                          "t<caret>wo<caret> \n" +
                          "three ");
  }

  public void testMotionDownAction() {
    typeTextInFile(parseKeys("2j"),
                   "o<caret>n<caret>e\n" +
                   "<caret>tw<caret>o          <caret> \n" +
                   "three\n" +
                   "four");
    myFixture.checkResult("one\n" +
                          "two           \n" +
                          "t<caret>h<caret>ree\n" +
                          "<caret>fo<caret>u<caret>r");
  }

  public void testMotionDownFirstNonSpaceAction() {
    typeTextInFile(parseKeys("+"),
                   " <caret> on<caret>e<caret> two\n" +
                   "<caret>   three<caret> four\n" +
                   " five six\n");
    myFixture.checkResult("  one two\n" +
                          "   <caret>three four\n" +
                          " <caret>five six\n");
  }

  public void testMotionDownLess1FirstNonSpaceActionWithNoCount() {
    typeTextInFile(parseKeys("_"),
                   "     one<caret> two\n" +
                   "three<caret> four\n" +
                   "  five<caret> six\n" +
                   " <caret>  seven eight");
    myFixture.checkResult("     <caret>one two\n" +
                          "<caret>three four\n" +
                          "  <caret>five six\n" +
                          "   <caret>seven eight");
  }

  public void testMotionDownLess1FirstNonSpaceActionWithCount() {
    typeTextInFile(parseKeys("3_"),
                   "x<caret>y<caret>z\n" +
                   "  skip this <caret>line\n" +
                   "   don't skip this line\n" +
                   "    stop there\n");
    myFixture.checkResult("xyz\n" +
                          "  skip this line\n" +
                          "   <caret>don't skip this line\n" +
                          "    <caret>stop there\n");
  }

  public void testMotionUpFirstNonSpaceAction() {
    typeTextInFile(parseKeys("-"),
                   " one\n" +
                   "<caret>  tw<caret>o\n");
    myFixture.checkResult(" <caret>one\n" +
                          "  two\n");
  }
}
