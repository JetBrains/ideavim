/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2019 The IdeaVim authors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package org.jetbrains.plugins.ideavim.action;

import com.maddyhome.idea.vim.VimPlugin;
import com.maddyhome.idea.vim.command.CommandState;
import org.jetbrains.plugins.ideavim.VimTestCase;

import static com.maddyhome.idea.vim.helper.StringHelper.parseKeys;
import static com.maddyhome.idea.vim.helper.StringHelper.stringToKeys;

/**
 * @author vlan
 */
public class ChangeActionTest extends VimTestCase {

  // VIM-620 |i_CTRL-O|
  public void testInsertSingleCommandAndInserting() {
    doTest(parseKeys("i", "<C-O>", "a", "123", "<Esc>", "x"), "abc<caret>d\n", "abcd12\n", CommandState.Mode.COMMAND,
           CommandState.SubMode.NONE);
  }

  // VIM-620 |i_CTRL-O|
  public void testInsertSingleCommandAndNewLineInserting() {
    doTest(parseKeys("i", "<C-O>", "o", "123", "<Esc>", "x"),
           "abc<caret>d\n", "abcd\n12\n", CommandState.Mode.COMMAND, CommandState.SubMode.NONE);
  }

  // VIM-311 |i_CTRL-O|
  public void testInsertSingleCommand() {
    doTest(parseKeys("i", "def", "<C-O>", "d2h", "x"),
           "abc<caret>.\n", "abcdx.\n", CommandState.Mode.INSERT, CommandState.SubMode.NONE);
  }

  // VIM-321 |d| |count|
  public void testDeleteEmptyRange() {
    doTest(parseKeys("d0"), "<caret>hello\n", "hello\n", CommandState.Mode.COMMAND, CommandState.SubMode.NONE);
  }

  // VIM-157 |~|
  public void testToggleCharCase() {
    doTest(parseKeys("~~"), "<caret>hello world\n", "HEllo world\n", CommandState.Mode.COMMAND,
           CommandState.SubMode.NONE);
  }

  // VIM-157 |~|
  public void testToggleCharCaseLineEnd() {
    doTest(parseKeys("~~"),
           "hello wor<caret>ld\n", "hello worLD\n", CommandState.Mode.COMMAND, CommandState.SubMode.NONE);
  }

  public void testToggleCaseMotion() {
    doTest(parseKeys("g~w"), "<caret>FooBar Baz\n", "fOObAR Baz\n", CommandState.Mode.COMMAND,
           CommandState.SubMode.NONE);
  }

  public void testChangeUpperCase() {
    doTest(parseKeys("gUw"), "<caret>FooBar Baz\n", "FOOBAR Baz\n", CommandState.Mode.COMMAND,
           CommandState.SubMode.NONE);
  }

  public void testChangeLowerCase() {
    doTest(parseKeys("guw"), "<caret>FooBar Baz\n", "foobar Baz\n", CommandState.Mode.COMMAND,
           CommandState.SubMode.NONE);
  }

  public void testToggleCaseVisual() {
    doTest(parseKeys("ve~"), "<caret>FooBar Baz\n", "fOObAR Baz\n", CommandState.Mode.COMMAND,
           CommandState.SubMode.NONE);
  }

  public void testChangeUpperCaseVisual() {
    doTest(parseKeys("veU"), "<caret>FooBar Baz\n", "FOOBAR Baz\n", CommandState.Mode.COMMAND,
           CommandState.SubMode.NONE);
  }

  public void testChangeLowerCaseVisual() {
    doTest(parseKeys("veu"), "<caret>FooBar Baz\n", "foobar Baz\n", CommandState.Mode.COMMAND,
           CommandState.SubMode.NONE);
  }

  // VIM-85 |i| |gi| |gg|
  public void testInsertAtPreviousAction() {
    doTest(parseKeys("i", "hello", "<Esc>", "gg", "gi", " world! "), "one\n" +
                                                                     "two <caret>three\n" +
                                                                     "four\n", "one\n" +
                                                                               "two hello world! three\n" + "four\n",
           CommandState.Mode.INSERT, CommandState.SubMode.NONE);
  }

  // VIM-312 |d| |w|
  public void testDeleteLastWordInFile() {
    doTest(parseKeys("dw"),
           "one\n" +
           "<caret>two\n",
           "one\n" + "\n", CommandState.Mode.COMMAND, CommandState.SubMode.NONE);
    assertOffset(4);
  }

  // |d| |w|
  public void testDeleteLastWordBeforeEOL() {
    doTest(parseKeys("dw"), "one <caret>two\n" + "three\n", "one \n" + "three\n", CommandState.Mode.COMMAND,
           CommandState.SubMode.NONE);
  }

  // VIM-105 |d| |w|
  public void testDeleteLastWordBeforeEOLs() {
    doTest(parseKeys("dw"), "one <caret>two\n" +
                            "\n" +
                            "three\n", "one \n" +
                                       "\n" + "three\n", CommandState.Mode.COMMAND, CommandState.SubMode.NONE);
  }

  // VIM-105 |d| |w|
  public void testDeleteLastWordBeforeEOLAndWhitespace() {
    doTest(parseKeys("dw"),
           "one <caret>two\n" +
           " three\n",
           "one \n" + " three\n", CommandState.Mode.COMMAND, CommandState.SubMode.NONE);
    assertOffset(3);
  }

  // VIM-105 |d| |w| |count|
  public void testDeleteTwoWordsOnTwoLines() {
    doTest(parseKeys("d2w"), "one <caret>two\n" + "three four\n", "one four\n", CommandState.Mode.COMMAND,
           CommandState.SubMode.NONE);
  }

  // VIM-1380 |d| |w| |count|
  public void testDeleteTwoWordsAtLastChar() {
    doTest(parseKeys("d2w"), "on<caret>e two three\n", "on<caret>three\n", CommandState.Mode.COMMAND,
           CommandState.SubMode.NONE);
  }

  // VIM-394 |d| |v_aw|
  public void testDeleteIndentedWordBeforePunctuation() {
    doTest(parseKeys("daw"), "foo\n" + "  <caret>bar, baz\n", "foo\n" + "  , baz\n", CommandState.Mode.COMMAND,
           CommandState.SubMode.NONE);
  }

  // |d| |v_aw|
  public void testDeleteLastWordAfterPunctuation() {
    doTest(parseKeys("daw"), "foo(<caret>bar\n" + "baz\n", "foo(\n" + "baz\n", CommandState.Mode.COMMAND,
           CommandState.SubMode.NONE);
  }

  // VIM-244 |d| |l|
  public void testDeleteLastCharInLine() {
    doTest(parseKeys("dl"),
           "fo<caret>o\n" +
           "bar\n",
           "fo\n" + "bar\n", CommandState.Mode.COMMAND, CommandState.SubMode.NONE);
    assertOffset(1);
  }

  // VIM-393 |d|
  public void testDeleteBadArgument() {
    doTest(parseKeys("dD", "dd"), "one\n" + "two\n", "two\n", CommandState.Mode.COMMAND, CommandState.SubMode.NONE);
  }

  // VIM-262 |i_CTRL-R|
  public void testInsertFromRegister() {
    VimPlugin.getRegister().setKeys('a', stringToKeys("World"));
    doTest(parseKeys("A", ", ", "<C-R>", "a", "!"), "<caret>Hello\n", "Hello, World!\n", CommandState.Mode.INSERT,
           CommandState.SubMode.NONE);
  }

  // VIM-404 |O|
  public void testInsertNewLineAboveFirstLine() {
    doTest(parseKeys("O", "bar"),
           "fo<caret>o\n", "bar\nfoo\n", CommandState.Mode.INSERT, CommandState.SubMode.NONE);
  }

  // VIM-472 |v|
  public void testVisualSelectionRightMargin() {
    doTest(parseKeys("v", "k$d"),
           "foo\n<caret>bar\n", "fooar\n", CommandState.Mode.COMMAND, CommandState.SubMode.NONE);
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
           "z\n" + "quux\n", CommandState.Mode.COMMAND, CommandState.SubMode.NONE);
  }

  public void testDeleteCharVisualBlock() {
    doTest(parseKeys("<C-V>", "jjl", "x"),
           "<caret>foo\n" +
           "bar\n" +
           "baz\n" +
           "quux\n",
           "<caret>o\n" +
           "r\n" +
           "z\n" + "quux\n", CommandState.Mode.COMMAND, CommandState.SubMode.NONE);
  }

  public void testDeleteJoinLinesSpaces() {
    doTest(parseKeys("3J"),
           "    a<caret> 1\n" +
           "    b 2\n" +
           "    c 3\n" +
           "quux\n",
           "    a 1 b 2 c 3\n" + "quux\n", CommandState.Mode.COMMAND, CommandState.SubMode.NONE);
  }

  public void testDeleteJoinLines() {
    doTest(parseKeys("3gJ"),
           "    a<caret> 1\n" +
           "    b 2\n" +
           "    c 3\n" +
           "quux\n",
           "    a 1    b 2    c 3\n" + "quux\n", CommandState.Mode.COMMAND, CommandState.SubMode.NONE);
  }

  public void testDeleteJoinLinesWithTrailingSpaceThenEmptyLine() {
    doTest(parseKeys("3J"),
           "foo \n" +
           "\n" +
           "bar", "foo bar", CommandState.Mode.COMMAND, CommandState.SubMode.NONE);
  }

  public void testDeleteJoinLinesWithTwoTrailingSpaces() {
    doTest(parseKeys("J"),
           "foo  \n" +
           "bar", "foo  bar", CommandState.Mode.COMMAND, CommandState.SubMode.NONE);
  }

  public void testDeleteJoinVisualLinesSpaces() {
    doTest(parseKeys("v2jJ"),
           "    a<caret> 1\n" +
           "    b 2\n" +
           "    c 3\n" +
           "quux\n",
           "    a 1 b 2 c 3\n" + "quux\n", CommandState.Mode.COMMAND, CommandState.SubMode.NONE);
  }

  public void testDeleteJoinVisualLines() {
    doTest(parseKeys("v2jgJ"),
           "    a<caret> 1\n" +
           "    b 2\n" +
           "    c 3\n" +
           "quux\n",
           "    a 1    b 2    c 3\n" + "quux\n", CommandState.Mode.COMMAND, CommandState.SubMode.NONE);
  }

  public void testDeleteCharVisualBlockOnLastCharOfLine() {
    doTest(parseKeys("<C-V>", "x"),
           "fo<caret>o\n", "fo\n", CommandState.Mode.COMMAND, CommandState.SubMode.NONE);
  }

  public void testDeleteCharVisualBlockOnEmptyLinesDoesntDeleteAnything() {
    doTest(parseKeys("<C-V>", "j", "x"),
           "\n\n", "\n\n", CommandState.Mode.COMMAND, CommandState.SubMode.NONE);
  }

  // VIM-781 |CTRL-V| |j|
  public void testDeleteCharVisualBlockWithEmptyLineInTheMiddle() {
    doTest(parseKeys("l", "<C-V>", "jj", "x"),
           "foo\n" +
           "\n" +
           "bar\n",
           "fo\n" +
           "\n" + "br\n", CommandState.Mode.COMMAND, CommandState.SubMode.NONE);
  }

  // VIM-781 |CTRL-V| |j|
  public void testDeleteCharVisualBlockWithShorterLineInTheMiddle() {
    doTest(parseKeys("l", "<C-V>", "jj", "x"),
           "foo\n" +
           "x\n" +
           "bar\n",
           "fo\n" +
           "x\n" + "br\n", CommandState.Mode.COMMAND, CommandState.SubMode.NONE);
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
           "b<caret>ar\n", "b<caret>xr\n", CommandState.Mode.COMMAND, CommandState.SubMode.NONE);
  }

  // |r|
  public void testReplaceMultipleCharsWithCount() {
    doTest(parseKeys("3rX"),
           "fo<caret>obar\n", "fo<caret>XXXr\n", CommandState.Mode.COMMAND, CommandState.SubMode.NONE);
  }

  // |r|
  public void testReplaceMultipleCharsWithCountPastEndOfLine() {
    doTest(parseKeys("6rX"),
           "fo<caret>obar\n", "fo<caret>obar\n", CommandState.Mode.COMMAND, CommandState.SubMode.NONE);
  }

  // |r|
  public void testReplaceMultipleCharsWithVisual() {
    doTest(parseKeys("v", "ll", "j", "rZ"),
           "fo<caret>obar\n" +
           "foobaz\n",
           "foZZZZ\n" + "ZZZZZz\n", CommandState.Mode.COMMAND, CommandState.SubMode.NONE);
  }

  // |r|
  public void testReplaceOneCharWithNewline() {
    doTest(parseKeys("r<Enter>"),
           "    fo<caret>obar\n" +
           "foobaz\n",
           "    fo\n" +
           "    bar\n" + "foobaz\n", CommandState.Mode.COMMAND, CommandState.SubMode.NONE);
  }

  // |r|
  public void testReplaceCharWithNewlineAndCountAddsOnlySingleNewline() {
    doTest(parseKeys("3r<Enter>"),
           "    fo<caret>obar\n" +
           "foobaz\n",
           "    fo\n" +
           "    r\n" + "foobaz\n", CommandState.Mode.COMMAND, CommandState.SubMode.NONE);
  }

  // |s|
  public void testReplaceOneCharWithText() {
    doTest(parseKeys("sxy<Esc>"),
           "b<caret>ar\n", "bx<caret>yr\n", CommandState.Mode.COMMAND, CommandState.SubMode.NONE);
  }

  // |s|
  public void testReplaceMultipleCharsWithTextWithCount() {
    doTest(parseKeys("3sxy<Esc>"),
           "fo<caret>obar\n", "fox<caret>yr\n", CommandState.Mode.COMMAND, CommandState.SubMode.NONE);
  }

  // |s|
  public void testReplaceMultipleCharsWithTextWithCountPastEndOfLine() {
    doTest(parseKeys("99sxyz<Esc>"),
           "foo<caret>bar\n" +
           "biff\n",
           "fooxy<caret>z\n" + "biff\n", CommandState.Mode.COMMAND, CommandState.SubMode.NONE);
  }

  // |R|
  public void testReplaceMode() {
    doTest(parseKeys("Rbaz<Esc>"),
           "foo<caret>bar\n", "fooba<caret>z\n", CommandState.Mode.COMMAND, CommandState.SubMode.NONE);
  }

  // |R| |i_<Insert>|
  public void testReplaceModeSwitchToInsertModeAndBack() {
    doTest(parseKeys("RXXX<Ins>YYY<Ins>ZZZ<Esc>"),
           "aaa<caret>bbbcccddd\n", "aaaXXXYYYZZ<caret>Zddd\n", CommandState.Mode.COMMAND, CommandState.SubMode.NONE);
  }

  // |i| |i_<Insert>|
  public void testInsertModeSwitchToReplaceModeAndBack() {
    doTest(parseKeys("iXXX<Ins>YYY<Ins>ZZZ<Esc>"),
           "aaa<caret>bbbcccddd\n", "aaaXXXYYYZZ<caret>Zcccddd\n", CommandState.Mode.COMMAND,
           CommandState.SubMode.NONE);
  }

  // VIM-511 |.|
  public void testRepeatWithBackspaces() {
    doTest(parseKeys("ce", "foo", "<BS><BS><BS>", "foo", "<Esc>", "j0", "."),
           "<caret>foo baz\n" +
           "baz quux\n",
           "foo baz\n" + "fo<caret>o quux\n", CommandState.Mode.COMMAND, CommandState.SubMode.NONE);
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

  // VIM-1067 |.|
  public void testRepeatWithInsertAfterLineEnd() {
    //Case 1
    configureByText("<caret>- 1\n" +
            "- 2\n" +
            "- 3\n");
    typeText(parseKeys("A", "<BS>", "<Esc>", "j", "."));
    myFixture.checkResult("- \n" +
            "- \n" +
            "- 3\n");

    //Case 2
    configureByText("<caret>- 1\n" +
            "- 2\n" +
            "- 3\n");
    typeText(parseKeys("A", "4", "<BS>", "<Esc>", "j", "."));
    myFixture.checkResult("- 1\n" +
            "- 2\n" +
            "- 3\n");

    //Case 3
    configureByText("<caret>- 1\n" +
            "- 2\n" +
            "- 3\n");
    typeText(parseKeys("A", "<BS>", "4",  "<Esc>", "j", "."));
    myFixture.checkResult("- 4\n" +
            "- 4\n" +
            "- 3\n");
  }

  // VIM-287 |zc| |O|
  public void testInsertAfterFold() {
    configureByJavaText("<caret>/**\n" +
                        " * I should be fold\n" +
                        " * a little more text\n" +
                        " * and final fold\n" +
                        " */\n" +
                        "and some text after\n");
    typeText(parseKeys("zc", "G", "O"));
    myFixture.checkResult("/**\n" +
                          " * I should be fold\n" +
                          " * a little more text\n" +
                          " * and final fold\n" +
                          " */\n" +
                          "<caret>\n" +
                          "and some text after\n");
  }

  // VIM-287 |zc| |o|
  public void testInsertBeforeFold() {
    configureByJavaText("<caret>/**\n" +
                        " * I should be fold\n" +
                        " * a little more text\n" +
                        " * and final fold\n" +
                        " */\n" +
                        "and some text after\n");
    typeText(parseKeys("zc", "o"));
    myFixture.checkResult("/**\n" +
                          " * I should be fold\n" +
                          " * a little more text\n" +
                          " * and final fold\n" +
                          " */\n" +
                          "<caret>\n" +
                          "and some text after\n");
  }

  public void testRepeatChangeWordDoesNotBreakNextRepeatFind() {
    doTest(parseKeys("fXcfYPATATA<Esc>fX.;."), "<caret>aaaaXBBBBYaaaaaaaXBBBBYaaaaaaXBBBBYaaaaaaaa\n",
           "aaaaPATATAaaaaaaaPATATAaaaaaaPATATAaaaaaaaa\n", CommandState.Mode.COMMAND, CommandState.SubMode.NONE);
  }

  public void testRepeatReplace() {
    configureByText("<caret>foobarbaz spam\n");
    typeText(parseKeys("R"));
    assertMode(CommandState.Mode.REPLACE);
    typeText(parseKeys("FOO", "<Esc>", "l", "2."));
    myFixture.checkResult("FOOFOOFO<caret>O spam\n");
    assertMode(CommandState.Mode.COMMAND);
  }

  public void testDownMovementAfterDeletionToStart() {
    doTest(parseKeys("ld^j"),
            "lorem <caret>ipsum dolor sit amet\n" +
                   "lorem ipsum dolor sit amet",
           "psum dolor sit amet\n" + "<caret>lorem ipsum dolor sit amet", CommandState.Mode.COMMAND,
           CommandState.SubMode.NONE);
  }

  public void testDownMovementAfterDeletionToPrevWord() {
    doTest(parseKeys("ldbj"),
            "lorem<caret> ipsum dolor sit amet\n" +
                    "lorem ipsum dolor sit amet",
           "ipsum dolor sit amet\n" + "<caret>lorem ipsum dolor sit amet", CommandState.Mode.COMMAND,
           CommandState.SubMode.NONE);
  }

  public void testDownMovementAfterChangeToPrevWord() {
    doTest(parseKeys("lcb<Esc>j"),
            "lorem<caret> ipsum dolor sit amet\n" +
                    "lorem ipsum dolor sit amet",
           "ipsum dolor sit amet\n" + "<caret>lorem ipsum dolor sit amet", CommandState.Mode.COMMAND,
           CommandState.SubMode.NONE);
  }

  public void testDownMovementAfterChangeToLineStart() {
    doTest(parseKeys("lc^<Esc>j"),
            "lorem<caret> ipsum dolor sit amet\n" +
                    "lorem ipsum dolor sit amet",
           "ipsum dolor sit amet\n" + "<caret>lorem ipsum dolor sit amet", CommandState.Mode.COMMAND,
           CommandState.SubMode.NONE);
  }

  public void testUpMovementAfterDeletionToStart() {
    doTest(parseKeys("ld^k"),
            "lorem ipsum dolor sit amet\n" +
                    "lorem <caret>ipsum dolor sit amet",
           "<caret>lorem ipsum dolor sit amet\n" + "psum dolor sit amet", CommandState.Mode.COMMAND,
           CommandState.SubMode.NONE);
  }

  public void testUpMovementAfterChangeToPrevWord() {
    doTest(parseKeys("lcb<Esc>k"),
            "lorem ipsum dolor sit amet\n" +
                    "lorem<caret> ipsum dolor sit amet",
           "<caret>lorem ipsum dolor sit amet\n" + "ipsum dolor sit amet", CommandState.Mode.COMMAND,
           CommandState.SubMode.NONE);
  }

  // VIM-714 |v|
  public void testDeleteVisualColumnPositionOneLine() {
    doTest(parseKeys("vwxj"),
           "<caret>lorem ipsum dolor sit amet\n" +
           "lorem ipsum dolor sit amet\n",
           "psum dolor sit amet\n" +
           "<caret>lorem ipsum dolor sit amet\n", CommandState.Mode.COMMAND, CommandState.SubMode.NONE);
  }

  // VIM-714 |v|
  public void testDeleteVisualColumnPositionMultiLine() {
    doTest(parseKeys("v3wfixj"),
           "gaganis <caret>gaganis gaganis\n" +
           "gaganis gaganis gaganis\n" +
           "gaganis gaganis gaganis\n",
           "gaganis s gaganis\n" +
           "gaganis <caret>gaganis gaganis\n", CommandState.Mode.COMMAND, CommandState.SubMode.NONE);
  }

  public void testChangeSameLine() {
    doTest(parseKeys("d_"),
           "line 1\n"+
           "line<caret> 2\n"+
           "line 3",
           "line 1\n"+
           "<caret>line 3",
           CommandState.Mode.COMMAND, CommandState.SubMode.NONE);
  }
}
