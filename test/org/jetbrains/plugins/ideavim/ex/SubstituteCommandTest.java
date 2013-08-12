package org.jetbrains.plugins.ideavim.ex;

import org.jetbrains.plugins.ideavim.VimTestCase;

/**
 * @author vlan
 */
public class SubstituteCommandTest extends VimTestCase {
  // |:substitute|
  public void testOneLetter() {
    doTest("s/a/b/",
           "a<caret>baba\n" +
           "ab\n",
           "bbaba\n" +
           "ab\n");
  }

  public void testOneLetterMultiPerLine() {
    doTest("s/a/b/g",
           "a<caret>baba\n" +
           "ab\n",
           "bbbbb\n" +
           "ab\n");
  }

  public void testOneLetterMultiPerLineWholeFile() {
    doTest("%s/a/b/g",
           "a<caret>baba\n" +
           "ab\n",
           "bbbbb\n" +
           "bb\n");
  }

  // VIM-146 |:substitute|
  public void testEOLtoQuote() {
    doTest("s/$/'/g",
           "<caret>one\n" +
           "two\n",
           "one'\n" +
           "two\n");
  }

  public void testSOLtoQuote() {
    doTest("s/^/'/g",
           "<caret>one\n" +
           "two\n",
           "'one\n" +
           "two\n");
  }

  public void testDotToNul() {
    doTest("s/\\./\\n/g",
           "<caret>one.two.three\n",
           "one\u0000two\u0000three\n");
  }

  public void testToNL() {
    doTest("s/\\./\\r/g",
           "<caret>one.two.three\n",
           "one\ntwo\nthree\n");
  }

  // VIM-289 |:substitute|
  public void testDotToNLDot() {
    doTest("s/\\./\\r\\./g",
           "<caret>one.two.three\n",
           "one\n.two\n.three\n");
  }

  private void doTest(final String command, String before, String after) {
    myFixture.configureByText("a.java", before);
    runExCommand(command);
    myFixture.checkResult(after);
  }
}
