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


  // VIM-528 |s/|
  public void testGroups() {
    doTest("s/\\(a\\|b\\)/z\\1/g",
           "<caret>abcdefg",
           "zazbcdefg");
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

  // VIM-702 |:substitute|
  public void testEndOfLineToNL() {
    doTest("%s/$/\\r/g",
           "<caret>one\ntwo\nthree\n",
           "one\n\ntwo\n\nthree\n\n");
  }

  // VIM-702 |:substitute|
  public void testStartOfLineToNL() {
    doTest("%s/^/\\r/g",
           "<caret>one\ntwo\nthree\n",
           "\none\n\ntwo\n\nthree\n");
  }

  public void testOffsetRange() {
    doTest(".,+2s/a/b/g",
           "aaa\naa<caret>a\naaa\naaa\naaa\n",
           "aaa\nbbb\nbbb\nbbb\naaa\n");
  }

  private void doTest(final String command, String before, String after) {
    myFixture.configureByText("a.java", before);
    typeText(commandToKeys(command));
    myFixture.checkResult(after);
  }
}
