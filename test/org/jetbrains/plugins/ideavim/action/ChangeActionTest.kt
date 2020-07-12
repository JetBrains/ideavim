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
    doTestWithNeovim(listOf("i", "<C-O>", "a", "123", "<Esc>", "x"), "abc${c}d\n", "abcd12\n", CommandState.Mode.COMMAND,
      CommandState.SubMode.NONE)
  }

  // VIM-620 |i_CTRL-O|
  fun testInsertSingleCommandAndNewLineInserting() {
    doTestWithNeovim(listOf("i", "<C-O>", "o", "123", "<Esc>", "x"),
      "abc${c}d\n", "abcd\n12\n", CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  // VIM-311 |i_CTRL-O|
  fun testInsertSingleCommand() {
    doTestWithNeovim(listOf("i", "def", "<C-O>", "d2h", "x"),
      "abc${c}.\n", "abcdx.\n", CommandState.Mode.INSERT, CommandState.SubMode.NONE)
  }

  // VIM-321 |d| |count|
  fun testDeleteEmptyRange() {
    doTestWithNeovim("d0", "${c}hello\n", "hello\n", CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  // VIM-157 |~|
  fun testToggleCharCase() {
    doTestWithNeovim("~~", "${c}hello world\n", "HEllo world\n", CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  // VIM-157 |~|
  fun testToggleCharCaseLineEnd() {
    doTestWithNeovim("~~", "hello wor${c}ld\n", "hello worLD\n", CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  fun testToggleCaseMotion() {
    doTestWithNeovim("g~w", "${c}FooBar Baz\n", "fOObAR Baz\n", CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  fun testChangeUpperCase() {
    doTestWithNeovim("gUw", "${c}FooBar Baz\n", "FOOBAR Baz\n", CommandState.Mode.COMMAND,
      CommandState.SubMode.NONE)
  }

  fun testChangeLowerCase() {
    doTestWithNeovim("guw", "${c}FooBar Baz\n", "foobar Baz\n", CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  fun testToggleCaseVisual() {
    doTestWithNeovim("ve~", "${c}FooBar Baz\n", "fOObAR Baz\n", CommandState.Mode.COMMAND,
      CommandState.SubMode.NONE)
  }

  fun testChangeUpperCaseVisual() {
    doTestWithNeovim("veU", "${c}FooBar Baz\n", "FOOBAR Baz\n", CommandState.Mode.COMMAND,
      CommandState.SubMode.NONE)
  }

  fun testChangeLowerCaseVisual() {
    doTestWithNeovim("veu", "${c}FooBar Baz\n", "foobar Baz\n", CommandState.Mode.COMMAND,
      CommandState.SubMode.NONE)
  }

  // VIM-85 |i| |gi| |gg|
  fun testInsertAtPreviousAction() {
    doTestWithNeovim(listOf("i", "hello", "<Esc>", "gg", "gi", " world! "), """
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
    doTestWithNeovim("dw",
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
    doTestWithNeovim("dw", """
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
    doTestWithNeovim("dw", """
   one ${c}two
   
   three
   
   """.trimIndent(), """
   one 
   
   three
   
   """.trimIndent(), CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  // VIM-105 |d| |w|
  fun testDeleteLastWordBeforeEOLAndWhitespace() {
    doTestWithNeovim("dw",
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
    doTestWithNeovim("d2w", """
   one ${c}two
   three four
   
   """.trimIndent(), "one four\n", CommandState.Mode.COMMAND,
      CommandState.SubMode.NONE)
  }

  // VIM-1380 |d| |w| |count|
  fun testDeleteTwoWordsAtLastChar() {
    doTestWithNeovim("d2w", "on${c}e two three\n", "on${c}three\n", CommandState.Mode.COMMAND,
      CommandState.SubMode.NONE)
  }

  // VIM-394 |d| |v_aw|
  fun testDeleteIndentedWordBeforePunctuation() {
    doTestWithNeovim("daw", """foo
  ${c}bar, baz
""", """foo
  , baz
""", CommandState.Mode.COMMAND,
      CommandState.SubMode.NONE)
  }

  // |d| |v_aw|
  fun testDeleteLastWordAfterPunctuation() {
    doTestWithNeovim("daw", """
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
    doTestWithNeovim("dl",
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
    doTestWithNeovim(listOf("dD", "dd"), """
   one
   two
   
   """.trimIndent(), "two\n", CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  // VIM-262 |i_CTRL-R|
  fun testInsertFromRegister() {
    setRegister('a', "World")
    doTestWithNeovim(listOf("A", ", ", "<C-R>", "a", "!"), "${c}Hello\n", "Hello, World!\n", CommandState.Mode.INSERT,
      CommandState.SubMode.NONE)
  }

  // VIM-404 |O|
  fun testInsertNewLineAboveFirstLine() {
    doTestWithNeovim(listOf("O", "bar"),
      "fo${c}o\n", "bar\nfoo\n", CommandState.Mode.INSERT, CommandState.SubMode.NONE)
  }

  // VIM-472 |v|
  fun testVisualSelectionRightMargin() {
    doTestWithNeovim(listOf("v", "k\$d"),
      "foo\n${c}bar\n", "fooar\n", CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  // VIM-632 |CTRL-V| |v_d|
  fun testDeleteVisualBlock() {
    doTestWithNeovim(listOf("<C-V>", "jjl", "d"),
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
    doTestWithNeovim(listOf("<C-V>", "jjl", "x"),
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
    doTestWithNeovim("3J",
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
    doTestWithNeovim("3gJ",
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
    doTestWithNeovim("3J",
      """
        foo 
        
        bar
        """.trimIndent(), "foo bar", CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  fun testDeleteJoinLinesWithTwoTrailingSpaces() {
    doTestWithNeovim("J",
      """
        foo  
        bar
        """.trimIndent(), "foo  bar", CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  fun testDeleteJoinVisualLinesSpaces() {
    doTestWithNeovim("v2jJ",
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
    doTestWithNeovim("v2jgJ",
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
    doTestWithNeovim(listOf("<C-V>", "x"),
      "fo${c}o\n", "fo\n", CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  fun testDeleteCharVisualBlockOnEmptyLinesDoesntDeleteAnything() {
    doTestWithNeovim(listOf("<C-V>", "j", "x"),
      "\n\n", "\n\n", CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  // VIM-781 |CTRL-V| |j|
  fun testDeleteCharVisualBlockWithEmptyLineInTheMiddle() {
    doTestWithNeovim(listOf("l", "<C-V>", "jj", "x"),
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
  fun testDeleteCharVisualBlockWithShorterLineInTheMiddle() {
    doTestWithNeovim(listOf("l", "<C-V>", "jj", "x"),
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
    doTestWithNeovim("rx", "b${c}ar\n", "b${c}xr\n", CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  // |r|
  @VimBehaviorDiffers(originalVimAfter = "foXX${c}Xr\n")
  fun testReplaceMultipleCharsWithCount() {
    doTestWithNeovim("3rX", "fo${c}obar\n", "fo${c}XXXr\n", CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  // |r|
  fun testReplaceMultipleCharsWithCountPastEndOfLine() {
    doTestWithNeovim("6rX", "fo${c}obar\n", "fo${c}obar\n", CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  // |r|
  @VimBehaviorDiffers(description = "Different caret position")
  fun testReplaceMultipleCharsWithVisual() {
    doTestWithNeovim(listOf("v", "ll", "j", "rZ"),
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
    doTestWithNeovim("r<Enter>",
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
    doTestWithNeovim("3r<Enter>",
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
    doTestWithNeovim("sxy<Esc>", "b${c}ar\n", "bx${c}yr\n", CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  // |s|
  fun testReplaceMultipleCharsWithTextWithCount() {
    doTestWithNeovim("3sxy<Esc>",
      "fo${c}obar\n", "fox${c}yr\n", CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  // |s|
  fun testReplaceMultipleCharsWithTextWithCountPastEndOfLine() {
    doTestWithNeovim("99sxyz<Esc>",
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
    doTestWithNeovim("Rbaz<Esc>", "foo${c}bar\n", "fooba${c}z\n", CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  // |R| |i_<Insert>|
  @VimBehaviorDiffers(description = "Different caret position")
  fun testReplaceModeSwitchToInsertModeAndBack() {
    doTestWithNeovim("RXXX<Ins>YYY<Ins>ZZZ<Esc>",
      "aaa${c}bbbcccddd\n", "aaaXXXYYYZZ${c}Zddd\n", CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  // |i| |i_<Insert>|
  @TestWithoutNeovim(SkipNeovimReason.UNCLEAR, "<INS> works strange")
  fun testInsertModeSwitchToReplaceModeAndBack() {
    doTestWithNeovim("iXXX<Ins>YYY<Ins>ZZZ<Esc>",
      "aaa${c}bbbcccddd\n", "aaaXXXYYYZZ${c}Zcccddd\n", CommandState.Mode.COMMAND,
      CommandState.SubMode.NONE)
  }

  // VIM-511 |.|
  @TestWithoutNeovim(SkipNeovimReason.UNCLEAR, "Backspace workspace strange")
  fun testRepeatWithBackspaces() {
    doTestWithNeovim(listOf("ce", "foo", "<BS><BS><BS>", "foo", "<Esc>", "j0", "."),
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
    doTestWithNeovim("fXcfYPATATA<Esc>fX.;.", "${c}aaaaXBBBBYaaaaaaaXBBBBYaaaaaaXBBBBYaaaaaaaa\n",
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
    doTestWithNeovim("ld^j",
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
    doTestWithNeovim("ldbj",
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
    doTestWithNeovim("lcb<Esc>j",
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
    doTestWithNeovim("lc^<Esc>j",
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
    doTestWithNeovim("ld^k",
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
    doTestWithNeovim("lcb<Esc>k",
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
    doTestWithNeovim("vwxj",
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
    doTestWithNeovim("v3wfixj",
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
    doTestWithNeovim("d_",
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
