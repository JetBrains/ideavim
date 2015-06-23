package org.jetbrains.plugins.ideavim.action;

import com.maddyhome.idea.vim.VimPlugin;
import org.jetbrains.plugins.ideavim.VimTestCase;

import static com.maddyhome.idea.vim.helper.StringHelper.parseKeys;
import static com.maddyhome.idea.vim.helper.StringHelper.stringToKeys;

/**
 * @author vlan
 */
public class ChangeActionTest extends VimTestCase {
  // |c| |t|
  public void testChangeLinesTillForwards() {
    doTest(parseKeys("ct(", "for "), "<caret>if (condition) {\n" + "}\n", "for (condition) {\n" + "}\n");
  }

  // VIM-276 |c| |T|
  public void testChangeLinesTillBackwards() {
    doTest(parseKeys("cT("), "if (condition) {<caret>\n" + "}\n", "if (\n" + "}\n");
  }

  // VIM-276 |c| |F|
  public void testChangeLinesToBackwards() {
    doTest(parseKeys("cFc"),
           "if (condition) {<caret>\n" +
           "}\n",
           "if (\n" +
           "}\n");
  }

  // VIM-311 |i_CTRL-O|
  public void testInsertSingleCommand() {
    doTest(parseKeys("i", "def", "<C-O>", "d2h", "x"),
           "abc<caret>.\n",
           "abcdx.\n");
  }

  // VIM-321 |d| |count|
  public void testDeleteEmptyRange() {
    doTest(parseKeys("d0"), "<caret>hello\n", "hello\n");
  }

  // VIM-112 |i| |i_CTRL-W|
  public void testInsertDeletePreviousWord() {
    typeTextInFile(parseKeys("i", "one two three", "<C-W>"),
                   "hello\n" +
                   "<caret>\n");
    myFixture.checkResult("hello\n" + "one two \n");
  }

  // VIM-157 |~|
  public void testToggleCharCase() {
    doTest(parseKeys("~~"), "<caret>hello world\n", "HEllo world\n");
  }

  // VIM-157 |~|
  public void testToggleCharCaseLineEnd() {
    doTest(parseKeys("~~"),
           "hello wor<caret>ld\n",
           "hello worLD\n");
  }

  public void testToggleCaseMotion() {
    doTest(parseKeys("g~w"), "<caret>FooBar Baz\n", "fOObAR Baz\n");
  }

  public void testChangeUpperCase() {
    doTest(parseKeys("gUw"), "<caret>FooBar Baz\n", "FOOBAR Baz\n");
  }

  public void testChangeLowerCase() {
    doTest(parseKeys("guw"), "<caret>FooBar Baz\n", "foobar Baz\n");
  }

  public void testToggleCaseVisual() {
    doTest(parseKeys("ve~"), "<caret>FooBar Baz\n", "fOObAR Baz\n");
  }

  public void testChangeUpperCaseVisual() {
    doTest(parseKeys("veU"), "<caret>FooBar Baz\n", "FOOBAR Baz\n");
  }

  public void testChangeLowerCaseVisual() {
    doTest(parseKeys("veu"), "<caret>FooBar Baz\n", "foobar Baz\n");
  }

  // VIM-85 |i| |gi| |gg|
  public void testInsertAtPreviousAction() {
    doTest(parseKeys("i", "hello", "<Esc>", "gg", "gi", " world! "), "one\n" +
                                                                     "two <caret>three\n" +
                                                                     "four\n", "one\n" +
                                                                               "two hello world! three\n" +
                                                                               "four\n");
  }

  // VIM-312 |d| |w|
  public void testDeleteLastWordInFile() {
    doTest(parseKeys("dw"),
           "one\n" +
           "<caret>two\n",
           "one\n" +
           "\n");
    assertOffset(4);
  }

  // |d| |w|
  public void testDeleteLastWordBeforeEOL() {
    doTest(parseKeys("dw"), "one <caret>two\n" + "three\n", "one \n" + "three\n");
  }

  // VIM-105 |d| |w|
  public void testDeleteLastWordBeforeEOLs() {
    doTest(parseKeys("dw"), "one <caret>two\n" +
                            "\n" +
                            "three\n", "one \n" +
                                       "\n" +
                                       "three\n");
  }

  // VIM-105 |d| |w|
  public void testDeleteLastWordBeforeEOLAndWhitespace() {
    doTest(parseKeys("dw"),
           "one <caret>two\n" +
           " three\n",
           "one \n" +
           " three\n");
    assertOffset(3);
  }

  // VIM-105 |d| |w| |count|
  public void testDeleteTwoWordsOnTwoLines() {
    doTest(parseKeys("d2w"), "one <caret>two\n" + "three four\n", "one four\n");
  }

  // VIM-200 |c| |w|
  public void testChangeWordAtLastChar() {
    doTest(parseKeys("cw"), "on<caret>e two three\n", "on two three\n");
  }

  // VIM-515 |c| |W|
  public void testChangeBigWordWithPunctuationAndAlpha() {
    doTest(parseKeys("cW"), "foo<caret>(bar baz\n", "foo baz\n");
  }

  // VIM-300 |c| |w|
  public void testChangeWordTwoWordsWithoutWhitespace() {
    doTest(parseKeys("cw"), "<caret>$value\n", "value\n");
  }

  // VIM-296 |cc|
  public void testChangeLineAtLastLine() {
    doTest(parseKeys("cc"),
           "foo\n" +
           "<caret>bar\n",
           "foo\n" +
           "\n");
    assertOffset(4);
  }

  // VIM-536 |cc|
  public void testChangeLineAtSecondLastLine() {
    doTest(parseKeys("ccbaz"),
           "<caret>foo\n" +
           "bar\n",
           "baz\n" +
           "bar\n");
  }

  // VIM-394 |d| |v_aw|
  public void testDeleteIndentedWordBeforePunctuation() {
    doTest(parseKeys("daw"), "foo\n" + "  <caret>bar, baz\n", "foo\n" + "  , baz\n");
  }

  // |d| |v_aw|
  public void testDeleteLastWordAfterPunctuation() {
    doTest(parseKeys("daw"), "foo(<caret>bar\n" + "baz\n", "foo(\n" + "baz\n");
  }

  // VIM-244 |d| |l|
  public void testDeleteLastCharInLine() {
    doTest(parseKeys("dl"),
           "fo<caret>o\n" +
           "bar\n",
           "fo\n" +
           "bar\n");
    assertOffset(1);
  }

  // VIM-393 |d|
  public void testDeleteBadArgument() {
    doTest(parseKeys("dD", "dd"), "one\n" + "two\n", "two\n");
  }

  // VIM-262 |i_CTRL-R|
  public void testInsertFromRegister() {
    VimPlugin.getRegister().setKeys('a', stringToKeys("World"));
    doTest(parseKeys("A", ", ", "<C-R>", "a", "!"), "<caret>Hello\n", "Hello, World!\n");
  }

  // VIM-421 |c| |w|
  public void testChangeLastWordInLine() {
    doTest(parseKeys("cw"),
           "ab.<caret>cd\n",
           "ab.<caret>\n");
  }

  // VIM-421 |c| |iw|
  public void testChangeLastInnerWordInLine() {
    doTest(parseKeys("c", "iw", "baz"),
           "foo bar bo<caret>o\n",
           "foo bar baz\n");
  }

  // VIM-421 |c| |w|
  public void testChangeLastCharInLine() {
    doTest(parseKeys("cw"), "fo<caret>o\n", "fo<caret>\n");
  }

  // VIM-404 |O|
  public void testInsertNewLineAboveFirstLine() {
    doTest(parseKeys("O", "bar"),
           "fo<caret>o\n",
           "bar\nfoo\n");
  }

  // VIM-472 |v|
  public void testVisualSelectionRightMargin() {
    doTest(parseKeys("v", "k$d"),
           "foo\n<caret>bar\n",
           "fooar\n");
  }

  // VIM-569 |a| |i_CTRL-W|
  public void testDeletePreviousWordDotEOL() {
    doTest(parseKeys("a", "<C-W>"),
           "this is a sentence<caret>.\n",
           "this is a sentence<caret>\n");
  }

  // VIM-569 |a| |i_CTRL-W|
  public void testDeletePreviousWordLastAfterWhitespace() {
    doTest(parseKeys("A", "<C-W>"),
           "<caret>this is a sentence\n",
           "this is a <caret>\n");
  }

  // VIM-513 |A| |i_CTRL-W|
  public void testDeletePreviousWordEOL() {
    doTest(parseKeys("A", "<C-W>"),
           "<caret>$variable\n",
           "$<caret>\n");
  }

  // VIM-632 |CTRL-V| |v_b_I|
  public void testChangeVisualBlock() {
    doTest(parseKeys("<C-V>", "j", "I", "quux ", "<Esc>"),
           "foo bar\n" +
           "<caret>baz quux\n" +
           "spam eggs\n",
           "foo bar\n" +
           "<caret>quux baz quux\n" +
           "quux spam eggs\n");
  }

  // VIM-632 |CTRL-V| |v_d|
  public void testDeleteVisualBlock() {
    doTest(parseKeys("<C-V>", "jjl", "d"),
           "<caret>foo\n" +
           "bar\n" +
           "baz\n" +
           "quux\n",
           "<caret>o\n" +
           "r\n" +
           "z\n" +
           "quux\n");
  }

  public void testDeleteCharVisualBlock() {
    doTest(parseKeys("<C-V>", "jjl", "x"),
           "<caret>foo\n" +
           "bar\n" +
           "baz\n" +
           "quux\n",
           "<caret>o\n" +
           "r\n" +
           "z\n" +
           "quux\n");
  }

  public void testDeleteJoinLinesSpaces() {
    doTest(parseKeys("3J"),
           "    a<caret> 1\n" +
           "    b 2\n" +
           "    c 3\n" +
           "quux\n",
           "    a 1 b 2 c 3\n" +
           "quux\n");
  }

  public void testDeleteJoinLines() {
    doTest(parseKeys("3gJ"),
           "    a<caret> 1\n" +
           "    b 2\n" +
           "    c 3\n" +
           "quux\n",
           "    a 1    b 2    c 3\n" +
           "quux\n");
  }

  public void testDeleteJoinLinesWithTrailingSpaceThenEmptyLine() {
    doTest(parseKeys("3J"),
           "foo \n" +
           "\n" +
           "bar",
           "foo bar");
  }

  public void testDeleteJoinLinesWithTwoTrailingSpaces() {
    doTest(parseKeys("J"),
           "foo  \n" +
           "bar",
           "foo  bar");
  }

  public void testDeleteJoinVisualLinesSpaces() {
    doTest(parseKeys("v2jJ"),
           "    a<caret> 1\n" +
           "    b 2\n" +
           "    c 3\n" +
           "quux\n",
           "    a 1 b 2 c 3\n" +
           "quux\n");
  }

  public void testDeleteJoinVisualLines() {
    doTest(parseKeys("v2jgJ"),
           "    a<caret> 1\n" +
           "    b 2\n" +
           "    c 3\n" +
           "quux\n",
           "    a 1    b 2    c 3\n" +
           "quux\n");
  }

  public void testDeleteCharVisualBlockOnLastCharOfLine() {
    doTest(parseKeys("<C-V>", "x"),
           "fo<caret>o\n",
           "fo\n");
  }

  public void testDeleteCharVisualBlockOnEmptyLinesDoesntDeleteAnything() {
    doTest(parseKeys("<C-V>", "j", "x"),
           "\n\n",
           "\n\n");
  }

  // VIM-781 |CTRL-V| |j|
  public void testDeleteCharVisualBlockWithEmptyLineInTheMiddle() {
    doTest(parseKeys("l", "<C-V>", "jj", "x"),
           "foo\n" +
           "\n" +
           "bar\n",
           "fo\n" +
           "\n" +
           "br\n");
  }

  // VIM-781 |CTRL-V| |j|
  public void testDeleteCharVisualBlockWithShorterLineInTheMiddle() {
    doTest(parseKeys("l", "<C-V>", "jj", "x"),
           "foo\n" +
           "x\n" +
           "bar\n",
           "fo\n" +
           "x\n" +
           "br\n");
  }

  // VIM-845 |CTRL-V| |x|
  public void testDeleteVisualBlockOneCharWide() {
    configureByText("foo\n" +
                    "bar\n");
    typeText(parseKeys("<C-V>", "j", "x"));
    myFixture.checkResult("oo\n" +
                          "ar\n");
  }

  // |r|
  public void testReplaceOneChar() {
    doTest(parseKeys("rx"),
           "b<caret>ar\n",
           "b<caret>xr\n");
  }

  // |r|
  public void testReplaceMultipleCharsWithCount() {
    doTest(parseKeys("3rX"),
           "fo<caret>obar\n",
           "fo<caret>XXXr\n");
  }

  // |r|
  public void testReplaceMultipleCharsWithCountPastEndOfLine() {
    doTest(parseKeys("6rX"),
           "fo<caret>obar\n",
           "fo<caret>obar\n");
  }

  // |r|
  public void testReplaceMultipleCharsWithVisual() {
    doTest(parseKeys("v", "ll", "j", "rZ"),
           "fo<caret>obar\n" +
           "foobaz\n",
           "foZZZZ\n" +
           "ZZZZZz\n");
  }

  // |r|
  public void testReplaceOneCharWithNewline() {
    doTest(parseKeys("r<Enter>"),
           "    fo<caret>obar\n" +
           "foobaz\n",
           "    fo\n" +
           "    bar\n" +
           "foobaz\n");
  }

  // |r|
  public void testReplaceCharWithNewlineAndCountAddsOnlySingleNewline() {
    doTest(parseKeys("3r<Enter>"),
           "    fo<caret>obar\n" +
           "foobaz\n",
           "    fo\n" +
           "    r\n" +
           "foobaz\n");
  }

  // |s|
  public void testReplaceOneCharWithText() {
    doTest(parseKeys("sxy<Esc>"),
           "b<caret>ar\n",
           "bx<caret>yr\n");
  }

  // |s|
  public void testReplaceMultipleCharsWithTextWithCount() {
    doTest(parseKeys("3sxy<Esc>"),
           "fo<caret>obar\n",
           "fox<caret>yr\n");
  }

  // |s|
  public void testReplaceMultipleCharsWithTextWithCountPastEndOfLine() {
    doTest(parseKeys("99sxyz<Esc>"),
           "foo<caret>bar\n" +
           "biff\n",
           "fooxy<caret>z\n" +
           "biff\n");
  }

  // |R|
  public void testReplaceMode() {
    doTest(parseKeys("Rbaz<Esc>"),
           "foo<caret>bar\n",
           "fooba<caret>z\n");
  }

  // |R| |i_<Insert>|
  public void testReplaceModeSwitchToInsertModeAndBack() {
    doTest(parseKeys("RXXX<Ins>YYY<Ins>ZZZ<Esc>"),
           "aaa<caret>bbbcccddd\n",
           "aaaXXXYYYZZ<caret>Zddd\n");
  }

  // |i| |i_<Insert>|
  public void testInsertModeSwitchToReplaceModeAndBack() {
    doTest(parseKeys("iXXX<Ins>YYY<Ins>ZZZ<Esc>"),
           "aaa<caret>bbbcccddd\n",
           "aaaXXXYYYZZ<caret>Zcccddd\n");
  }

  // VIM-511 |.|
  public void testRepeatWithBackspaces() {
    doTest(parseKeys("ce", "foo", "<BS><BS><BS>", "foo", "<Esc>", "j0", "."),
           "<caret>foo baz\n" +
           "baz quux\n",
           "foo baz\n" +
           "fo<caret>o quux\n");
  }

  // VIM-511 |.|
  public void testRepeatWithParensAndQuotesAutoInsertion() {
    configureByJavaText("class C <caret>{\n" +
                        "}\n");
    typeText(parseKeys("o", "foo(\"<Right>, \"<Right><Right>;", "<Esc>", "."));
    myFixture.checkResult("class C {\n" +
                          "    foo(\"\", \"\");\n" +
                          "    foo(\"\", \"\");\n" +
                          "}\n");
  }

  // VIM-511 |.|
  public void testDeleteBothParensAndStartAgain() {
    configureByJavaText("class C <caret>{\n" +
                        "}\n");
    typeText(parseKeys("o", "C(", "<BS>", "(int i) {}", "<Esc>", "."));
    myFixture.checkResult("class C {\n" +
                          "    C(int i) {}\n" +
                          "    C(int i) {}\n" +
                          "}\n");
  }

  // VIM-613 |.|
  public void testDeleteEndOfLineAndAgain() {
    configureByText("<caret>- 1\n" +
                    "- 2\n" +
                    "- 3\n");
    typeText(parseKeys("d$", "j", "."));
    myFixture.checkResult("\n" +
                          "\n" +
                          "- 3\n");
  }

  // VIM-511 |.|
  public void testAutoCompleteCurlyBraceWithEnterWithinFunctionBody() {
    configureByJavaText("class C <caret>{\n" +
                        "}\n");
    typeText(parseKeys("o", "C(", "<BS>", "(int i) {", "<Enter>", "i = 3;", "<Esc>", "<Down>", "."));
    myFixture.checkResult("class C {\n" +
                          "    C(int i) {\n" +
                          "        i = 3;\n" +
                          "    }\n" +
                          "    C(int i) {\n" +
                          "        i = 3;\n" +
                          "    }\n" +
                          "}\n");
  }
}
