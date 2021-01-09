/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2020 The IdeaVim authors
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
package org.jetbrains.plugins.ideavim.action

import com.intellij.codeInsight.folding.CodeFoldingManager
import com.intellij.codeInsight.folding.impl.FoldingUtil
import com.maddyhome.idea.vim.command.CommandState
import com.maddyhome.idea.vim.helper.StringHelper
import com.maddyhome.idea.vim.helper.VimBehaviorDiffers
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase

/**
 * @author vlan
 */
class ChangeActionTest : VimTestCase() {
  // VIM-620 |i_CTRL-O|
  fun testInsertSingleCommandAndInserting() {
    doTest(listOf("i", "<C-O>", "a", "123", "<Esc>", "x"), "abc${c}d\n", "abcd12\n", CommandState.Mode.COMMAND,
      CommandState.SubMode.NONE)
  }

  // VIM-620 |i_CTRL-O|
  fun testInsertSingleCommandAndNewLineInserting() {
    doTest(listOf("i", "<C-O>", "o", "123", "<Esc>", "x"),
      "abc${c}d\n", "abcd\n12\n", CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  // VIM-311 |i_CTRL-O|
  fun testInsertSingleCommand() {
    doTest(listOf("i", "def", "<C-O>", "d2h", "x"),
      "abc${c}.\n", "abcdx.\n", CommandState.Mode.INSERT, CommandState.SubMode.NONE)
  }

  // VIM-321 |d| |count|
  fun testDeleteEmptyRange() {
    doTest("d0", "${c}hello\n", "hello\n", CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  // VIM-157 |~|
  fun testToggleCharCase() {
    doTest("~~", "${c}hello world\n", "HEllo world\n", CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  // VIM-157 |~|
  fun testToggleCharCaseLineEnd() {
    doTest("~~", "hello wor${c}ld\n", "hello worLD\n", CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  fun testToggleCaseMotion() {
    doTest("g~w", "${c}FooBar Baz\n", "fOObAR Baz\n", CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  fun testChangeUpperCase() {
    doTest("gUw", "${c}FooBar Baz\n", "FOOBAR Baz\n", CommandState.Mode.COMMAND,
      CommandState.SubMode.NONE)
  }

  fun testChangeLowerCase() {
    doTest("guw", "${c}FooBar Baz\n", "foobar Baz\n", CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  fun testToggleCaseVisual() {
    doTest("ve~", "${c}FooBar Baz\n", "fOObAR Baz\n", CommandState.Mode.COMMAND,
      CommandState.SubMode.NONE)
  }

  fun testChangeUpperCaseVisual() {
    doTest("veU", "${c}FooBar Baz\n", "FOOBAR Baz\n", CommandState.Mode.COMMAND,
      CommandState.SubMode.NONE)
  }

  fun testChangeLowerCaseVisual() {
    doTest("veu", "${c}FooBar Baz\n", "foobar Baz\n", CommandState.Mode.COMMAND,
      CommandState.SubMode.NONE)
  }

  // VIM-85 |i| |gi| |gg|
  fun testInsertAtPreviousAction() {
    doTest(listOf("i", "hello", "<Esc>", "gg", "gi", " world! "), """
   one
   two ${c}three
   four
   
   """.trimIndent(), """
   one
   two hello world! three
   four
   
   """.trimIndent(),
      CommandState.Mode.INSERT, CommandState.SubMode.NONE)
  }

  // VIM-312 |d| |w|
  fun testDeleteLastWordInFile() {
    doTest("dw",
      """
        one
        ${c}two
        
        """.trimIndent(),
      """
        one
        
        
        """.trimIndent(), CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
    assertOffset(4)
  }

  // |d| |w|
  fun testDeleteLastWordBeforeEOL() {
    doTest("dw", """
   one ${c}two
   three
   
   """.trimIndent(), """
   one 
   three
   
   """.trimIndent(), CommandState.Mode.COMMAND,
      CommandState.SubMode.NONE)
  }

  // VIM-105 |d| |w|
  fun testDeleteLastWordBeforeEOLs() {
    doTest("dw", """
   one ${c}two
   
   three
   
   """.trimIndent(), """
   one 
   
   three
   
   """.trimIndent(), CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  // VIM-105 |d| |w|
  fun testDeleteLastWordBeforeEOLAndWhitespace() {
    doTest("dw",
      """one ${c}two
 three
""",
      """one 
 three
""", CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
    assertOffset(3)
  }

  // VIM-105 |d| |w| |count|
  fun testDeleteTwoWordsOnTwoLines() {
    doTest("d2w", """
   one ${c}two
   three four
   
   """.trimIndent(), "one four\n", CommandState.Mode.COMMAND,
      CommandState.SubMode.NONE)
  }

  // VIM-1380 |d| |w| |count|
  fun testDeleteTwoWordsAtLastChar() {
    doTest("d2w", "on${c}e two three\n", "on${c}three\n", CommandState.Mode.COMMAND,
      CommandState.SubMode.NONE)
  }

  // VIM-394 |d| |v_aw|
  fun testDeleteIndentedWordBeforePunctuation() {
    doTest("daw", """foo
  ${c}bar, baz
""", """foo
  , baz
""", CommandState.Mode.COMMAND,
      CommandState.SubMode.NONE)
  }

  // |d| |v_aw|
  fun testDeleteLastWordAfterPunctuation() {
    doTest("daw", """
   foo(${c}bar
   baz
   
   """.trimIndent(), """
   foo(
   baz
   
   """.trimIndent(), CommandState.Mode.COMMAND,
      CommandState.SubMode.NONE)
  }

  // VIM-244 |d| |l|
  fun testDeleteLastCharInLine() {
    doTest("dl",
      """
        fo${c}o
        bar
        
        """.trimIndent(),
      """
        fo
        bar
        
        """.trimIndent(), CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
    assertOffset(1)
  }

  // VIM-393 |d|
  fun testDeleteBadArgument() {
    doTest(listOf("dD", "dd"), """
   one
   two
   
   """.trimIndent(), "two\n", CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  // VIM-262 |i_CTRL-R|
  fun testInsertFromRegister() {
    setRegister('a', "World")
    doTest(listOf("A", ", ", "<C-R>", "a", "!"), "${c}Hello\n", "Hello, World!\n", CommandState.Mode.INSERT,
      CommandState.SubMode.NONE)
  }

  // VIM-404 |O|
  fun testInsertNewLineAboveFirstLine() {
    doTest(listOf("O", "bar"),
      "fo${c}o\n", "bar\nfoo\n", CommandState.Mode.INSERT, CommandState.SubMode.NONE)
  }

  // VIM-472 |v|
  fun testVisualSelectionRightMargin() {
    doTest(listOf("v", "k\$d"),
      "foo\n${c}bar\n", "fooar\n", CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  // VIM-632 |CTRL-V| |v_d|
  fun testDeleteVisualBlock() {
    doTest(listOf("<C-V>", "jjl", "d"),
      """
        ${c}foo
        bar
        baz
        quux
        
        """.trimIndent(),
      """
        ${c}o
        r
        z
        quux
        
        """.trimIndent(), CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  fun testDeleteCharVisualBlock() {
    doTest(listOf("<C-V>", "jjl", "x"),
      """
        ${c}foo
        bar
        baz
        quux
        
        """.trimIndent(),
      """
        ${c}o
        r
        z
        quux
        
        """.trimIndent(), CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  fun testDeleteJoinLinesSpaces() {
    doTest("3J",
      """    a${c} 1
    b 2
    c 3
quux
""",
      """    a 1 b 2 c 3
quux
""", CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  fun testDeleteJoinLines() {
    doTest("3gJ",
      """    a${c} 1
    b 2
    c 3
quux
""",
      """    a 1    b 2    c 3
quux
""", CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  fun testDeleteJoinLinesWithTrailingSpaceThenEmptyLine() {
    doTest("3J",
      """
        foo 
        
        bar
        """.trimIndent(), "foo bar", CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  fun testDeleteJoinLinesWithTwoTrailingSpaces() {
    doTest("J",
      """
        foo  
        bar
        """.trimIndent(), "foo  bar", CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  fun testDeleteJoinVisualLinesSpaces() {
    doTest("v2jJ",
      """    a${c} 1
    b 2
    c 3
quux
""",
      """    a 1 b 2 c 3
quux
""", CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  fun testDeleteJoinVisualLines() {
    doTest("v2jgJ",
      """    a${c} 1
    b 2
    c 3
quux
""",
      """    a 1    b 2    c 3
quux
""", CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  fun testDeleteCharVisualBlockOnLastCharOfLine() {
    doTest(listOf("<C-V>", "x"),
      "fo${c}o\n", "fo\n", CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  fun testDeleteCharVisualBlockOnEmptyLinesDoesntDeleteAnything() {
    setupChecks {
      this.neoVim.ignoredRegisters = setOf('1', '"')
    }
    doTest(listOf("<C-V>", "j", "x"),
      "\n\n", "\n\n", CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  // VIM-781 |CTRL-V| |j|
  fun testDeleteCharVisualBlockWithEmptyLineInTheMiddle() {
    doTest(listOf("l", "<C-V>", "jj", "x"),
      """
        foo
        
        bar
        
        """.trimIndent(),
      """
        fo
        
        br
        
        """.trimIndent(), CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  // VIM-781 |CTRL-V| |j|
  @VimBehaviorDiffers(description = "Different registers content")
  fun testDeleteCharVisualBlockWithShorterLineInTheMiddle() {
    doTest(listOf("l", "<C-V>", "jj", "x"),
      """
        foo
        x
        bar
        
        """.trimIndent(),
      """
        fo
        x
        br
        
        """.trimIndent(), CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  // VIM-845 |CTRL-V| |x|
  fun testDeleteVisualBlockOneCharWide() {
    configureByText("""
  foo
  bar
  
  """.trimIndent())
    typeText(StringHelper.parseKeys("<C-V>", "j", "x"))
    myFixture.checkResult("""
  oo
  ar
  
  """.trimIndent())
  }

  // |r|
  fun testReplaceOneChar() {
    doTest("rx", "b${c}ar\n", "b${c}xr\n", CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  // |r|
  @VimBehaviorDiffers(originalVimAfter = "foXX${c}Xr\n")
  fun testReplaceMultipleCharsWithCount() {
    doTest("3rX", "fo${c}obar\n", "fo${c}XXXr\n", CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  // |r|
  fun testReplaceMultipleCharsWithCountPastEndOfLine() {
    doTest("6rX", "fo${c}obar\n", "fo${c}obar\n", CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  // |r|
  @VimBehaviorDiffers(description = "Different caret position")
  fun testReplaceMultipleCharsWithVisual() {
    doTest(listOf("v", "ll", "j", "rZ"),
      """
        fo${c}obar
        foobaz
        
        """.trimIndent(),
      """
        foZZZZ
        ZZZZZz
        
        """.trimIndent(), CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  // |r|
  fun testReplaceOneCharWithNewline() {
    doTest("r<Enter>",
      """    fo${c}obar
foobaz
""",
      """    fo
    bar
foobaz
""", CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  // |r|
  @VimBehaviorDiffers(description = "Different caret position")
  fun testReplaceCharWithNewlineAndCountAddsOnlySingleNewline() {
    doTest("3r<Enter>",
      """    fo${c}obar
foobaz
""",
      """    fo
    r
foobaz
""", CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  // |s|
  fun testReplaceOneCharWithText() {
    doTest("sxy<Esc>", "b${c}ar\n", "bx${c}yr\n", CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  // |s|
  fun testReplaceMultipleCharsWithTextWithCount() {
    doTest("3sxy<Esc>",
      "fo${c}obar\n", "fox${c}yr\n", CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  // |s|
  fun testReplaceMultipleCharsWithTextWithCountPastEndOfLine() {
    doTest("99sxyz<Esc>",
      """
        foo${c}bar
        biff
        
        """.trimIndent(),
      """
        fooxy${c}z
        biff
        
        """.trimIndent(), CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  // |R|
  fun testReplaceMode() {
    doTest("Rbaz<Esc>", "foo${c}bar\n", "fooba${c}z\n", CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  // |R| |i_<Insert>|
  @VimBehaviorDiffers(description = "Different caret position")
  fun testReplaceModeSwitchToInsertModeAndBack() {
    doTest("RXXX<Ins>YYY<Ins>ZZZ<Esc>",
      "aaa${c}bbbcccddd\n", "aaaXXXYYYZZ${c}Zddd\n", CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  // |i| |i_<Insert>|
  @TestWithoutNeovim(SkipNeovimReason.UNCLEAR, "<INS> works strange")
  fun testInsertModeSwitchToReplaceModeAndBack() {
    doTest("iXXX<Ins>YYY<Ins>ZZZ<Esc>",
      "aaa${c}bbbcccddd\n", "aaaXXXYYYZZ${c}Zcccddd\n", CommandState.Mode.COMMAND,
      CommandState.SubMode.NONE)
  }

  // VIM-511 |.|
  @TestWithoutNeovim(SkipNeovimReason.UNCLEAR, "Backspace workspace strange")
  fun testRepeatWithBackspaces() {
    doTest(listOf("ce", "foo", "<BS><BS><BS>", "foo", "<Esc>", "j0", "."),
      """
        ${c}foo baz
        baz quux
        
        """.trimIndent(),
      """
        foo baz
        fo${c}o quux
        
        """.trimIndent(), CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  // VIM-511 |.|
  fun testRepeatWithParensAndQuotesAutoInsertion() {
    configureByJavaText("""
  class C ${c}{
  }
  
  """.trimIndent())
    typeText(StringHelper.parseKeys("o", "foo(\"<Right>, \"<Right><Right>;", "<Esc>", "."))
    myFixture.checkResult("""class C {
    foo("", "");
    foo("", "");
}
""")
  }

  // VIM-511 |.|
  fun testDeleteBothParensAndStartAgain() {
    configureByJavaText("""
  class C ${c}{
  }
  
  """.trimIndent())
    typeText(StringHelper.parseKeys("o", "C(", "<BS>", "(int i) {}", "<Esc>", "."))
    myFixture.checkResult("""class C {
    C(int i) {}
    C(int i) {}
}
""")
  }

  // VIM-613 |.|
  fun testDeleteEndOfLineAndAgain() {
    configureByText("""
  ${c}- 1
  - 2
  - 3
  
  """.trimIndent())
    typeText(StringHelper.parseKeys("d$", "j", "."))
    myFixture.checkResult("""
  
  
  - 3
  
  """.trimIndent())
  }

  // VIM-511 |.|
  fun testAutoCompleteCurlyBraceWithEnterWithinFunctionBody() {
    configureByJavaText("""
  class C ${c}{
  }
  
  """.trimIndent())
    typeText(StringHelper.parseKeys("o", "C(", "<BS>", "(int i) {", "<Enter>", "i = 3;", "<Esc>", "<Down>", "."))
    myFixture.checkResult("""class C {
    C(int i) {
        i = 3;
    }
    C(int i) {
        i = 3;
    }
}
""")
  }

  // VIM-1067 |.|
  fun testRepeatWithInsertAfterLineEnd() {
    //Case 1
    configureByText("""
  ${c}- 1
  - 2
  - 3
  
  """.trimIndent())
    typeText(StringHelper.parseKeys("A", "<BS>", "<Esc>", "j", "."))
    myFixture.checkResult("""
  - 
  - 
  - 3
  
  """.trimIndent())

    //Case 2
    configureByText("""
  ${c}- 1
  - 2
  - 3
  
  """.trimIndent())
    typeText(StringHelper.parseKeys("A", "4", "<BS>", "<Esc>", "j", "."))
    myFixture.checkResult("""
  - 1
  - 2
  - 3
  
  """.trimIndent())

    //Case 3
    configureByText("""
  ${c}- 1
  - 2
  - 3
  
  """.trimIndent())
    typeText(StringHelper.parseKeys("A", "<BS>", "4", "<Esc>", "j", "."))
    myFixture.checkResult("""
  - 4
  - 4
  - 3
  
  """.trimIndent())
  }

  // VIM-287 |zc| |O|
  fun testInsertAfterFold() {
    configureByJavaText("""${c}/**
 * I should be fold
 * a little more text
 * and final fold
 */
and some text after""")
    typeText(StringHelper.parseKeys("zc", "G", "O"))
    myFixture.checkResult("""/**
 * I should be fold
 * a little more text
 * and final fold
 */
${c}
and some text after""")
  }

  // VIM-287 |zc| |o|
  fun testInsertBeforeFold() {
    configureByJavaText("""
          ${c}/**
           * I should be fold
           * a little more text
           * and final fold
           */
          and some text after
        """.trimIndent())

    myFixture.editor.foldingModel.runBatchFoldingOperation {
      CodeFoldingManager.getInstance(myFixture.project).updateFoldRegions(myFixture.editor)
      FoldingUtil.findFoldRegionStartingAtLine(myFixture.editor, 0)!!.isExpanded = false
    }

    typeText(StringHelper.parseKeys("o"))
    myFixture.checkResult("""
            /**
             * I should be fold
             * a little more text
             * and final fold
             */
            ${c}
            and some text after
            """.trimIndent())
  }

  fun testRepeatChangeWordDoesNotBreakNextRepeatFind() {
    doTest("fXcfYPATATA<Esc>fX.;.", "${c}aaaaXBBBBYaaaaaaaXBBBBYaaaaaaXBBBBYaaaaaaaa\n",
      "aaaaPATATAaaaaaaaPATATAaaaaaaPATATAaaaaaaaa\n", CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  fun testRepeatReplace() {
    configureByText("${c}foobarbaz spam\n")
    typeText(StringHelper.parseKeys("R"))
    assertMode(CommandState.Mode.REPLACE)
    typeText(StringHelper.parseKeys("FOO", "<Esc>", "l", "2."))
    myFixture.checkResult("FOOFOOFO${c}O spam\n")
    assertMode(CommandState.Mode.COMMAND)
  }

  fun testDownMovementAfterDeletionToStart() {
    doTest("ld^j",
      """
        lorem ${c}ipsum dolor sit amet
        lorem ipsum dolor sit amet
        """.trimIndent(),
      """
        psum dolor sit amet
        ${c}lorem ipsum dolor sit amet
        """.trimIndent(), CommandState.Mode.COMMAND,
      CommandState.SubMode.NONE)
  }

  fun testDownMovementAfterDeletionToPrevWord() {
    doTest("ldbj",
      """
        lorem${c} ipsum dolor sit amet
        lorem ipsum dolor sit amet
        """.trimIndent(),
      """
        ipsum dolor sit amet
        ${c}lorem ipsum dolor sit amet
        """.trimIndent(), CommandState.Mode.COMMAND,
      CommandState.SubMode.NONE)
  }

  fun testDownMovementAfterChangeToPrevWord() {
    doTest("lcb<Esc>j",
      """
        lorem${c} ipsum dolor sit amet
        lorem ipsum dolor sit amet
        """.trimIndent(),
      """
        ipsum dolor sit amet
        ${c}lorem ipsum dolor sit amet
        """.trimIndent(), CommandState.Mode.COMMAND,
      CommandState.SubMode.NONE)
  }

  fun testDownMovementAfterChangeToLineStart() {
    doTest("lc^<Esc>j",
      """
        lorem${c} ipsum dolor sit amet
        lorem ipsum dolor sit amet
        """.trimIndent(),
      """
        ipsum dolor sit amet
        ${c}lorem ipsum dolor sit amet
        """.trimIndent(), CommandState.Mode.COMMAND,
      CommandState.SubMode.NONE)
  }

  fun testUpMovementAfterDeletionToStart() {
    doTest("ld^k",
      """
        lorem ipsum dolor sit amet
        lorem ${c}ipsum dolor sit amet
        """.trimIndent(),
      """
        ${c}lorem ipsum dolor sit amet
        psum dolor sit amet
        """.trimIndent(), CommandState.Mode.COMMAND,
      CommandState.SubMode.NONE)
  }

  fun testUpMovementAfterChangeToPrevWord() {
    doTest("lcb<Esc>k",
      """
        lorem ipsum dolor sit amet
        lorem${c} ipsum dolor sit amet
        """.trimIndent(),
      """
        ${c}lorem ipsum dolor sit amet
        ipsum dolor sit amet
        """.trimIndent(), CommandState.Mode.COMMAND,
      CommandState.SubMode.NONE)
  }

  // VIM-714 |v|
  fun testDeleteVisualColumnPositionOneLine() {
    doTest("vwxj",
      """
        ${c}lorem ipsum dolor sit amet
        lorem ipsum dolor sit amet
        
        """.trimIndent(),
      """
        psum dolor sit amet
        ${c}lorem ipsum dolor sit amet
        
        """.trimIndent(), CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  // VIM-714 |v|
  fun testDeleteVisualColumnPositionMultiLine() {
    doTest("v3wfixj",
      """
        gaganis ${c}gaganis gaganis
        gaganis gaganis gaganis
        gaganis gaganis gaganis
        
        """.trimIndent(),
      """
        gaganis s gaganis
        gaganis ${c}gaganis gaganis
        
        """.trimIndent(), CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  fun testChangeSameLine() {
    doTest("d_",
      """
        line 1
        line${c} 2
        line 3
        """.trimIndent(),
      """
        line 1
        ${c}line 3
        """.trimIndent(),
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }
}
